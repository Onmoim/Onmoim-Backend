package com.onmoim.server.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
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
import com.onmoim.server.post.dto.response.CommentResponseDto;
import com.onmoim.server.post.dto.response.CommentThreadResponseDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.repository.CommentRepository;
import com.onmoim.server.post.repository.CommentRepository.CommentReplyCountProjection;
import com.onmoim.server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentQueryService commentQueryService;

    private User testUser;
    private Group testGroup;
    private GroupPost testPost;
    private Comment parentComment1;
    private Comment parentComment2;
    private Comment childComment1;
    private Comment childComment2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("testUser")
                .build();
        setId(testUser, 1L);

        testGroup = Group.builder()
                .name("testGroup")
                .capacity(10)
                .build();
        setId(testGroup, 1L);

        testPost = GroupPost.builder()
                .title("Test Title")
                .content("Test Content")
                .type(GroupPostType.FREE)
                .group(testGroup)
                .author(testUser)
                .build();
        setId(testPost, 1L);

        parentComment1 = Comment.builder()
                .post(testPost)
                .author(testUser)
                .parent(null)
                .content("첫 번째 부모 댓글")
                .build();
        setId(parentComment1, 1L);

        parentComment2 = Comment.builder()
                .post(testPost)
                .author(testUser)
                .parent(null)
                .content("두 번째 부모 댓글")
                .build();
        setId(parentComment2, 2L);

        childComment1 = Comment.builder()
                .post(testPost)
                .author(testUser)
                .parent(parentComment1)
                .content("첫 번째 답글")
                .build();
        setId(childComment1, 3L);

        childComment2 = Comment.builder()
                .post(testPost)
                .author(testUser)
                .parent(parentComment1)
                .content("두 번째 답글")
                .build();
        setId(childComment2, 4L);
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
    @DisplayName("부모댓글 목록 조회 성공 - 첫 페이지")
    void getParentCommentsFirstPage() {
        // given
        Long cursor = null;
        List<Comment> comments = Arrays.asList(parentComment2, parentComment1); // 최신순

        // CommentReplyCountProjection Mock 객체 생성
        CommentReplyCountProjection projection1 = mock(CommentReplyCountProjection.class);
        when(projection1.getParentId()).thenReturn(1L);
        when(projection1.getReplyCount()).thenReturn(2L);

        CommentReplyCountProjection projection2 = mock(CommentReplyCountProjection.class);
        when(projection2.getParentId()).thenReturn(2L);
        when(projection2.getReplyCount()).thenReturn(0L);

        List<CommentReplyCountProjection> replyCountData = Arrays.asList(projection1, projection2);

        when(commentRepository.findParentCommentsByPost(testPost, cursor)).thenReturn(comments);
        when(commentRepository.countRepliesByParentIds(Arrays.asList(2L, 1L))).thenReturn(replyCountData);

        // when
        CursorPageResponseDto<CommentResponseDto> result = commentQueryService.getParentComments(testPost, cursor);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(2L); // parentComment2
        assertThat(result.getContent().get(0).getReplyCount()).isEqualTo(0L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(1L); // parentComment1
        assertThat(result.getContent().get(1).getReplyCount()).isEqualTo(2L);

        verify(commentRepository).findParentCommentsByPost(testPost, cursor);
        verify(commentRepository).countRepliesByParentIds(Arrays.asList(2L, 1L));
    }

    @Test
    @DisplayName("부모댓글 목록 조회 성공 - 빈 결과")
    void getParentCommentsEmpty() {
        // given
        Long cursor = null;
        List<Comment> comments = Arrays.asList();

        when(commentRepository.findParentCommentsByPost(testPost, cursor)).thenReturn(comments);

        // when
        CursorPageResponseDto<CommentResponseDto> result = commentQueryService.getParentComments(testPost, cursor);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursorId()).isNull();

        verify(commentRepository).findParentCommentsByPost(testPost, cursor);
    }

    @Test
    @DisplayName("댓글 스레드 조회 성공")
    void getCommentThreadSuccess() {
        // given
        Long commentId = 1L;
        Long cursor = null;
        List<Comment> replies = Arrays.asList(childComment2, childComment1); // 최신순

        // CommentReplyCountProjection Mock 객체 생성 (배치 조회용)
        CommentReplyCountProjection projection = mock(CommentReplyCountProjection.class);
        when(projection.getParentId()).thenReturn(1L);
        when(projection.getReplyCount()).thenReturn(2L);

        when(commentRepository.findByIdWithAuthor(commentId)).thenReturn(Optional.of(parentComment1));
        when(commentRepository.findRepliesByParent(parentComment1, cursor)).thenReturn(replies);
        when(commentRepository.countRepliesByParentIds(Arrays.asList(1L))).thenReturn(Arrays.asList(projection));

        // when
        CommentThreadResponseDto result = commentQueryService.getCommentThread(commentId, cursor);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getParentComment()).isNotNull();
        assertThat(result.getParentComment().getId()).isEqualTo(1L);
        assertThat(result.getParentComment().getReplyCount()).isEqualTo(2L);

        assertThat(result.getReplies()).hasSize(2);
        assertThat(result.getReplies().get(0).getId()).isEqualTo(4L); // childComment2
        assertThat(result.getReplies().get(1).getId()).isEqualTo(3L); // childComment1

        verify(commentRepository).findByIdWithAuthor(commentId);
        verify(commentRepository).findRepliesByParent(parentComment1, cursor);
        verify(commentRepository).countRepliesByParentIds(Arrays.asList(1L));
    }

    @Test
    @DisplayName("댓글 스레드 조회 실패 - 댓글을 찾을 수 없음")
    void getCommentThreadFailNotFound() {
        // given
        Long commentId = 999L;
        Long cursor = null;

        when(commentRepository.findByIdWithAuthor(commentId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> commentQueryService.getCommentThread(commentId, cursor));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentRepository).findByIdWithAuthor(commentId);
    }

    @Test
    @DisplayName("댓글 스레드 조회 실패 - 답글에 대해 스레드 조회 시도")
    void getCommentThreadFailNotParentComment() {
        // given
        Long commentId = 3L; // childComment1의 ID (답글)
        Long cursor = null;

        when(commentRepository.findByIdWithAuthor(commentId)).thenReturn(Optional.of(childComment1));

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> commentQueryService.getCommentThread(commentId, cursor));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_COMMENT_THREAD);
        verify(commentRepository).findByIdWithAuthor(commentId);
    }

    @Test
    @DisplayName("댓글 스레드 조회 - 답글이 없는 경우")
    void getCommentThreadNoReplies() {
        // given
        Long commentId = 2L; // parentComment2 (답글 없음)
        Long cursor = null;
        List<Comment> replies = Arrays.asList();

        // CommentReplyCountProjection Mock 객체 생성 (답글 없음)
        CommentReplyCountProjection projection = mock(CommentReplyCountProjection.class);
        when(projection.getParentId()).thenReturn(2L);
        when(projection.getReplyCount()).thenReturn(0L);

        when(commentRepository.findByIdWithAuthor(commentId)).thenReturn(Optional.of(parentComment2));
        when(commentRepository.findRepliesByParent(parentComment2, cursor)).thenReturn(replies);
        when(commentRepository.countRepliesByParentIds(Arrays.asList(2L))).thenReturn(Arrays.asList(projection));

        // when
        CommentThreadResponseDto result = commentQueryService.getCommentThread(commentId, cursor);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getParentComment()).isNotNull();
        assertThat(result.getParentComment().getId()).isEqualTo(2L);
        assertThat(result.getParentComment().getReplyCount()).isEqualTo(0L);

        assertThat(result.getReplies()).isEmpty();
        assertThat(result.isHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();

        verify(commentRepository).findByIdWithAuthor(commentId);
        verify(commentRepository).findRepliesByParent(parentComment2, cursor);
        verify(commentRepository).countRepliesByParentIds(Arrays.asList(2L));
    }
}
