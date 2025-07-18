package com.onmoim.server.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.onmoim.server.post.repository.PostLikeRepository;
import com.onmoim.server.post.entity.vo.PostLikeInfo;
import com.onmoim.server.post.dto.internal.PostLikeBatchResult;
import com.onmoim.server.post.entity.PostLike;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.user.entity.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostLikeQueryServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostLikeQueryService postLikeQueryService;

    @Test
    @DisplayName("사용자가 특정 게시글에 좋아요했는지 확인 - 좋아요한 경우")
    void isLikedByUserTrue() {
        // given
        Long postId = 1L;
        Long userId = 1L;

        PostLike activeLike = createMockPostLike(postId, userId, true);
        when(postLikeRepository.findByPostIdIn(List.of(postId)))
                .thenReturn(List.of(activeLike));

        // when
        boolean result = postLikeQueryService.isLikedByUser(postId, userId);

        // then
        assertThat(result).isTrue();
        verify(postLikeRepository).findByPostIdIn(List.of(postId));
    }

    @Test
    @DisplayName("사용자가 특정 게시글에 좋아요했는지 확인 - 좋아요하지 않은 경우")
    void isLikedByUserFalse() {
        // given
        Long postId = 1L;
        Long userId = 1L;

        // 다른 사용자나 비활성 좋아요만 있는 경우
        PostLike otherUserLike = createMockPostLike(postId, 2L, true);
        when(postLikeRepository.findByPostIdIn(List.of(postId)))
                .thenReturn(List.of(otherUserLike));

        // when
        boolean result = postLikeQueryService.isLikedByUser(postId, userId);

        // then
        assertThat(result).isFalse();
        verify(postLikeRepository).findByPostIdIn(List.of(postId));
    }

    @Test
    @DisplayName("비로그인 사용자(userId null)는 항상 좋아요하지 않은 상태")
    void isLikedByUserNull() {
        // given
        Long postId = 1L;
        Long userId = null;

        // when
        boolean result = postLikeQueryService.isLikedByUser(postId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("특정 게시글의 좋아요 수 조회")
    void getLikeCount() {
        // given
        Long postId = 1L;

        PostLike activeLike1 = createMockPostLike(postId, 1L, true);
        PostLike activeLike2 = createMockPostLike(postId, 2L, true);
        PostLike inactiveLike = createMockPostLike(postId, 3L, false);

        when(postLikeRepository.findByPostIdIn(List.of(postId)))
                .thenReturn(List.of(activeLike1, activeLike2, inactiveLike));

        // when
        Long result = postLikeQueryService.getLikeCount(postId);

        // then
        assertThat(result).isEqualTo(2L); // 활성 좋아요 2개
        verify(postLikeRepository).findByPostIdIn(List.of(postId));
    }

    @Test
    @DisplayName("게시글 좋아요 배치 조회 테스트 - 타입 안전한 VO 사용")
    void getPostLikeBatchResult() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L);
        Long userId = 1L;

        List<PostLike> mockLikes = Arrays.asList(
            createMockPostLike(1L, 1L, true),  // 1번 게시글에 1번 사용자 좋아요
            createMockPostLike(1L, 2L, true),  // 1번 게시글에 2번 사용자 좋아요
            createMockPostLike(1L, 3L, false), // 1번 게시글에 3번 사용자 비활성 좋아요
            createMockPostLike(2L, 2L, true),  // 2번 게시글에 2번 사용자 좋아요
            createMockPostLike(2L, 3L, true)   // 2번 게시글에 3번 사용자 좋아요
        );

        when(postLikeRepository.findByPostIdIn(postIds))
                .thenReturn(mockLikes);

        // when
        PostLikeBatchResult result = postLikeQueryService.getPostLikeBatchResult(postIds, userId);

        // then
        assertThat(result.getLikeCount(1L)).isEqualTo(2L); // 1번 게시글 활성 좋아요 2개
        assertThat(result.getLikeCount(2L)).isEqualTo(2L); // 2번 게시글 활성 좋아요 2개
        assertThat(result.isLikedByUser(1L)).isTrue();     // 1번 게시글은 1번 사용자가 좋아요
        assertThat(result.isLikedByUser(2L)).isFalse();    // 2번 게시글은 1번 사용자가 좋아요 안함
        assertThat(result.getLikedPostCount()).isEqualTo(1L);

        verify(postLikeRepository, times(2)).findByPostIdIn(postIds);
    }

    @Test
    @DisplayName("비로그인 사용자의 좋아요 배치 조회 - 모두 false 반환")
    void getPostLikeBatchResultWithNullUser() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L);
        Long nullUserId = null;

        List<PostLike> mockLikes = Arrays.asList(
            createMockPostLike(1L, 1L, true),
            createMockPostLike(2L, 2L, true)
        );

        when(postLikeRepository.findByPostIdIn(postIds))
                .thenReturn(mockLikes);

        // when
        PostLikeBatchResult result = postLikeQueryService.getPostLikeBatchResult(postIds, nullUserId);

        // then
        assertThat(result.getLikeCount(1L)).isEqualTo(1L);
        assertThat(result.getLikeCount(2L)).isEqualTo(1L);
        assertThat(result.isLikedByUser(1L)).isFalse(); // 비로그인 사용자는 모두 false
        assertThat(result.isLikedByUser(2L)).isFalse();
        assertThat(result.getLikedPostCount()).isEqualTo(0L);

        // fetchLikeCountMap에서만 호출됨 (getLikedPostIds는 nullUser에서 early return)
        verify(postLikeRepository, times(1)).findByPostIdIn(postIds);
    }

    @Test
    @DisplayName("게시글 좋아요 정보 조회 - 단일 게시글")
    void getPostLikeInfo() {
        // given
        Long postId = 1L;
        Long userId = 1L;

        List<PostLike> mockLikes = Arrays.asList(
            createMockPostLike(postId, userId, true),
            createMockPostLike(postId, 2L, true),
            createMockPostLike(postId, 3L, false) // 비활성
        );

        when(postLikeRepository.findByPostIdIn(List.of(postId)))
                .thenReturn(mockLikes);

        // when
        PostLikeInfo result = postLikeQueryService.getPostLikeInfo(postId, userId);

        // then
        assertThat(result.likeCount()).isEqualTo(2L); // 활성 좋아요 2개
        assertThat(result.isLiked()).isTrue(); // 해당 사용자 좋아요함

        // getPostLikeInfo는 내부적으로 getLikeCount와 isLikedByUser를 호출하므로 총 2번 호출
        verify(postLikeRepository, times(2)).findByPostIdIn(List.of(postId));
    }

    @Test
    @DisplayName("빈 게시글 ID 리스트로 배치 조회 시 빈 결과 반환")
    void getPostLikeBatchResultEmpty() {
        // given
        List<Long> emptyPostIds = Arrays.asList();
        Long userId = 1L;

        // when
        PostLikeBatchResult result = postLikeQueryService.getPostLikeBatchResult(emptyPostIds, userId);

        // then
        assertThat(result.likeInfoByPostId()).isEmpty();
        assertThat(result.likedPostIds()).isEmpty();
        assertThat(result.getLikedPostCount()).isEqualTo(0L);

    }

    /**
     * 테스트용 Mock PostLike 생성 헬퍼 메서드
     */
    private PostLike createMockPostLike(Long postId, Long userId, boolean isActive) {
        GroupPost mockPost = mock(GroupPost.class);
        User mockUser = mock(User.class);
        PostLike mockPostLike = mock(PostLike.class);

        when(mockPost.getId()).thenReturn(postId);
        when(mockUser.getId()).thenReturn(userId);
        when(mockPostLike.getPost()).thenReturn(mockPost);
        when(mockPostLike.getUser()).thenReturn(mockUser);
        when(mockPostLike.isActive()).thenReturn(isActive);
        when(mockPostLike.getDeletedDate()).thenReturn(isActive ? null : java.time.LocalDateTime.now());

        return mockPostLike;
    }
}
