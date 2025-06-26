package com.onmoim.server.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.PostLike;
import com.onmoim.server.post.repository.PostLikeRepository;
import com.onmoim.server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostLikeService postLikeService;

    private User testUser;
    private Group testGroup;
    private GroupPost testPost;
    private PostLike testPostLike;

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

        testPostLike = PostLike.builder()
                .post(testPost)
                .user(testUser)
                .build();
        setId(testPostLike, 1L);
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
    @DisplayName("첫 좋아요 - 새 PostLike 엔티티 생성")
    void toggleLikeFirstTime() {
        // given
        when(postLikeRepository.findByPostAndUser(testPost, testUser))
                .thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testPostLike);

        // when
        boolean result = postLikeService.toggleLike(testPost, testUser);

        // then
        assertThat(result).isTrue(); // 좋아요 활성화
        verify(postLikeRepository).findByPostAndUser(testPost, testUser);
        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    @DisplayName("좋아요 취소 - 활성 상태에서 논리삭제")
    void toggleLikeCancelActive() {
        // given
        PostLike activeLike = PostLike.builder()
                .post(testPost)
                .user(testUser)
                .build();
        setId(activeLike, 1L);
        // activeLike는 deletedDate가 null인 활성 상태

        when(postLikeRepository.findByPostAndUser(testPost, testUser))
                .thenReturn(Optional.of(activeLike));

        // when
        boolean result = postLikeService.toggleLike(testPost, testUser);

        // then
        assertThat(result).isFalse(); // 좋아요 비활성화
        assertThat(activeLike.isActive()).isFalse(); // 논리삭제 확인
        verify(postLikeRepository).findByPostAndUser(testPost, testUser);
    }

    @Test
    @DisplayName("재좋아요 - 논리삭제된 상태에서 복구")
    void toggleLikeReactivate() {
        // given
        PostLike cancelledLike = PostLike.builder()
                .post(testPost)
                .user(testUser)
                .build();
        setId(cancelledLike, 1L);
        cancelledLike.cancel(); // 논리삭제 상태로 만듦

        when(postLikeRepository.findByPostAndUser(testPost, testUser))
                .thenReturn(Optional.of(cancelledLike));

        // when
        boolean result = postLikeService.toggleLike(testPost, testUser);

        // then
        assertThat(result).isTrue(); // 좋아요 활성화
        assertThat(cancelledLike.isActive()).isTrue(); // 복구 확인
        verify(postLikeRepository).findByPostAndUser(testPost, testUser);
    }

    @Test
    @DisplayName("좋아요 추가 - 새 레코드 생성")
    void likePostNewRecord() {
        // given
        when(postLikeRepository.findByPostAndUser(testPost, testUser))
                .thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testPostLike);

        // when
        Long result = postLikeService.likePost(testPost, testUser);

        // then
        assertThat(result).isEqualTo(1L);
        verify(postLikeRepository).findByPostAndUser(testPost, testUser);
        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    @DisplayName("좋아요 추가 - 기존 레코드 활성화")
    void likePostExistingRecord() {
        // given
        PostLike cancelledLike = PostLike.builder()
                .post(testPost)
                .user(testUser)
                .build();
        setId(cancelledLike, 1L);
        cancelledLike.cancel();

        when(postLikeRepository.findByPostAndUser(testPost, testUser))
                .thenReturn(Optional.of(cancelledLike));

        // when
        Long result = postLikeService.likePost(testPost, testUser);

        // then
        assertThat(result).isEqualTo(1L);
        assertThat(cancelledLike.isActive()).isTrue(); // 활성화 확인
        verify(postLikeRepository).findByPostAndUser(testPost, testUser);
        verify(postLikeRepository, times(0)).save(any(PostLike.class)); // 새로 저장하지 않음
    }

    @Test
    @DisplayName("좋아요 취소 - 성공")
    void unlikePostSuccess() {
        // given
        PostLike activeLike = PostLike.builder()
                .post(testPost)
                .user(testUser)
                .build();
        setId(activeLike, 1L);

        when(postLikeRepository.findByPostAndUser(testPost, testUser))
                .thenReturn(Optional.of(activeLike));

        // when
        Long result = postLikeService.unlikePost(testPost, testUser);

        // then
        assertThat(result).isEqualTo(1L);
        assertThat(activeLike.isActive()).isFalse();
        verify(postLikeRepository).findByPostAndUser(testPost, testUser);
    }

    @Test
    @DisplayName("좋아요 취소 실패 - 좋아요하지 않은 게시글")
    void unlikePostNotLiked() {
        // given
        when(postLikeRepository.findByPostAndUser(testPost, testUser))
                .thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> postLikeService.unlikePost(testPost, testUser));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_LIKED);
        verify(postLikeRepository).findByPostAndUser(testPost, testUser);
    }
}
