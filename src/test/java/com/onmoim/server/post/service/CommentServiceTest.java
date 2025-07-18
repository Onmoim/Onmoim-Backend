package com.onmoim.server.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.repository.CommentRepository;
import com.onmoim.server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private Group testGroup;
    private GroupPost testPost;
    private Comment parentComment;
    private Comment childComment;

    @BeforeEach
    void setUp() {
        // User 객체 생성
        testUser = User.builder()
                .name("testUser")
                .build();
        setId(testUser, 1L);

        // Group 객체 생성
        testGroup = Group.builder()
                .name("testGroup")
                .capacity(10)
                .build();
        setId(testGroup, 1L);

        // GroupPost 객체 생성
        testPost = GroupPost.builder()
                .title("Test Title")
                .content("Test Content")
                .type(GroupPostType.FREE)
                .group(testGroup)
                .author(testUser)
                .build();
        setId(testPost, 1L);

        // 부모 댓글 생성
        parentComment = Comment.builder()
                .post(testPost)
                .author(testUser)
                .parent(null)
                .content("부모 댓글입니다")
                .build();
        setId(parentComment, 1L);

        // 자식 댓글 생성
        childComment = Comment.builder()
                .post(testPost)
                .author(testUser)
                .parent(parentComment)
                .content("답글입니다")
                .build();
        setId(childComment, 2L);
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }

    @Test
    @DisplayName("부모 댓글 작성 성공")
    void createParentCommentSuccess() {
        // given
        String content = "새로운 댓글입니다";
        Comment savedComment = Comment.builder()
                .post(testPost)
                .author(testUser)
                .parent(null)
                .content(content)
                .build();
        setId(savedComment, 3L);

        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // when
        Long commentId = commentService.createParentComment(testPost, testUser, content);

        // then
        assertThat(commentId).isEqualTo(3L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("답글 작성 성공")
    void createReplySuccess() {
        // given
        String content = "답글 내용입니다";
        Long parentCommentId = 1L;
        
        when(commentRepository.findByIdWithAuthor(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(childComment);

        // when
        Long replyId = commentService.createReply(testPost, testUser, parentCommentId, content);

        // then
        assertThat(replyId).isEqualTo(2L);
        verify(commentRepository).findByIdWithAuthor(parentCommentId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("답글 작성 실패 - 부모 댓글이 존재하지 않음")
    void createReplyFailNotFoundParent() {
        // given
        String content = "답글 내용입니다";
        Long parentCommentId = 999L;
        
        when(commentRepository.findByIdWithAuthor(parentCommentId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> commentService.createReply(testPost, testUser, parentCommentId, content));
        
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentRepository).findByIdWithAuthor(parentCommentId);
    }

    @Test
    @DisplayName("답글 작성 실패 - 부모가 답글인 경우 (3단계 깊이 방지)")
    void createReplyFailInvalidDepth() {
        // given
        String content = "답글의 답글";
        Long childCommentId = 2L;
        
        when(commentRepository.findByIdWithAuthor(childCommentId)).thenReturn(Optional.of(childComment));

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> commentService.createReply(testPost, testUser, childCommentId, content));
        
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_COMMENT_THREAD);
        verify(commentRepository).findByIdWithAuthor(childCommentId);
    }

    @Test
    @DisplayName("답글 작성 실패 - 다른 게시글의 댓글에 답글 시도")
    void createReplyFailDifferentPost() {
        // given
        GroupPost otherPost = GroupPost.builder()
                .title("Other Post")
                .content("Other Content")
                .type(GroupPostType.FREE)
                .group(testGroup)
                .author(testUser)
                .build();
        setId(otherPost, 2L);

        String content = "답글 내용입니다";
        Long parentCommentId = 1L;
        
        when(commentRepository.findByIdWithAuthor(parentCommentId)).thenReturn(Optional.of(parentComment));

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> commentService.createReply(otherPost, testUser, parentCommentId, content));
        
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_COMMENT_THREAD);
        verify(commentRepository).findByIdWithAuthor(parentCommentId);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateCommentSuccess() {
        // given
        Long commentId = 1L;
        String newContent = "수정된 댓글 내용";
        
        when(commentRepository.findByIdAndAuthor(commentId, testUser)).thenReturn(Optional.of(parentComment));

        // when
        Long updatedCommentId = commentService.updateComment(commentId, testUser, newContent);

        // then
        assertThat(updatedCommentId).isEqualTo(1L);
        assertThat(parentComment.getContent()).isEqualTo(newContent);
        verify(commentRepository).findByIdAndAuthor(commentId, testUser);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글을 찾을 수 없음 또는 작성자가 아님")
    void updateCommentFailNotFound() {
        // given
        Long commentId = 999L;
        String newContent = "수정된 댓글 내용";
        
        when(commentRepository.findByIdAndAuthor(commentId, testUser)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> commentService.updateComment(commentId, testUser, newContent));
        
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentRepository).findByIdAndAuthor(commentId, testUser);
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteCommentSuccess() {
        // given
        Long commentId = 1L;
        
        when(commentRepository.findByIdAndAuthor(commentId, testUser)).thenReturn(Optional.of(parentComment));

        // when
        Long deletedCommentId = commentService.deleteComment(commentId, testUser);

        // then
        assertThat(deletedCommentId).isEqualTo(1L);
        assertThat(parentComment.isDeleted()).isTrue();
        verify(commentRepository).findByIdAndAuthor(commentId, testUser);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글을 찾을 수 없음 또는 작성자가 아님")
    void deleteCommentFailNotFound() {
        // given
        Long commentId = 999L;
        
        when(commentRepository.findByIdAndAuthor(commentId, testUser)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> commentService.deleteComment(commentId, testUser));
        
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentRepository).findByIdAndAuthor(commentId, testUser);
    }
} 