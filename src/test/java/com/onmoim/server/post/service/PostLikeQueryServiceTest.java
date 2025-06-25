package com.onmoim.server.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onmoim.server.post.repository.PostLikeRepository;
import com.onmoim.server.post.vo.PostLikeInfo;
import com.onmoim.server.post.vo.PostLikeBatchResult;

@ExtendWith(MockitoExtension.class)
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
        List<Long> likedPostIds = Arrays.asList(1L);

        when(postLikeRepository.findLikedPostIdsByUserAndPostIds(List.of(postId), userId))
                .thenReturn(likedPostIds);

        // when
        boolean result = postLikeQueryService.isLikedByUser(postId, userId);

        // then
        assertThat(result).isTrue();
        verify(postLikeRepository).findLikedPostIdsByUserAndPostIds(List.of(postId), userId);
    }

    @Test
    @DisplayName("사용자가 특정 게시글에 좋아요했는지 확인 - 좋아요하지 않은 경우")
    void isLikedByUserFalse() {
        // given
        Long postId = 1L;
        Long userId = 1L;
        List<Long> emptyList = Arrays.asList();

        when(postLikeRepository.findLikedPostIdsByUserAndPostIds(List.of(postId), userId))
                .thenReturn(emptyList);

        // when
        boolean result = postLikeQueryService.isLikedByUser(postId, userId);

        // then
        assertThat(result).isFalse();
        verify(postLikeRepository).findLikedPostIdsByUserAndPostIds(List.of(postId), userId);
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
        Long expectedCount = 10L;

        when(postLikeRepository.countActiveLikesByPostId(postId))
                .thenReturn(expectedCount);

        // when
        Long result = postLikeQueryService.getLikeCount(postId);

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(postLikeRepository).countActiveLikesByPostId(postId);
    }

    @Test
    @DisplayName("게시글 좋아요 배치 조회 테스트 - 타입 안전한 VO 사용")
    void getPostLikeBatchResult() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L);
        Long userId = 1L;
        List<PostLikeRepository.PostLikeCountProjection> mockResults = Arrays.asList(
                new PostLikeRepository.PostLikeCountProjection() {
                    @Override public Long getPostId() { return 1L; }
                    @Override public Long getLikeCount() { return 5L; }
                },
                new PostLikeRepository.PostLikeCountProjection() {
                    @Override public Long getPostId() { return 2L; }
                    @Override public Long getLikeCount() { return 3L; }
                }
        );
        List<Long> mockLikedPostIds = Arrays.asList(1L); // 1번 게시글만 좋아요

        when(postLikeRepository.countActiveLikesByPostIds(postIds))
                .thenReturn(mockResults);
        when(postLikeRepository.findLikedPostIdsByUserAndPostIds(postIds, userId))
                .thenReturn(mockLikedPostIds);

        // when
        PostLikeBatchResult result = postLikeQueryService.getPostLikeBatchResult(postIds, userId);

        // then
        assertThat(result.getLikeCount(1L)).isEqualTo(5L);
        assertThat(result.getLikeCount(2L)).isEqualTo(3L);
        assertThat(result.isLikedByUser(1L)).isTrue();   // 1번 게시글은 좋아요
        assertThat(result.isLikedByUser(2L)).isFalse();  // 2번 게시글은 좋아요 안함
        assertThat(result.getLikedPostCount()).isEqualTo(1L);
        
        verify(postLikeRepository).countActiveLikesByPostIds(postIds);
        verify(postLikeRepository).findLikedPostIdsByUserAndPostIds(postIds, userId);
    }

    @Test
    @DisplayName("비로그인 사용자의 좋아요 배치 조회 - 모두 false 반환")
    void getPostLikeBatchResultWithNullUser() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L);
        Long nullUserId = null;
        List<PostLikeRepository.PostLikeCountProjection> mockResults = Arrays.asList(
                new PostLikeRepository.PostLikeCountProjection() {
                    @Override public Long getPostId() { return 1L; }
                    @Override public Long getLikeCount() { return 5L; }
                },
                new PostLikeRepository.PostLikeCountProjection() {
                    @Override public Long getPostId() { return 2L; }
                    @Override public Long getLikeCount() { return 3L; }
                }
        );

        when(postLikeRepository.countActiveLikesByPostIds(postIds))
                .thenReturn(mockResults);

        // when
        PostLikeBatchResult result = postLikeQueryService.getPostLikeBatchResult(postIds, nullUserId);

        // then
        assertThat(result.getLikeCount(1L)).isEqualTo(5L);
        assertThat(result.getLikeCount(2L)).isEqualTo(3L);
        assertThat(result.isLikedByUser(1L)).isFalse(); // 비로그인 사용자는 모두 false
        assertThat(result.isLikedByUser(2L)).isFalse();
        assertThat(result.getLikedPostCount()).isEqualTo(0L);
        
        verify(postLikeRepository).countActiveLikesByPostIds(postIds);
    }

    @Test
    @DisplayName("게시글 좋아요 정보 조회 - 단일 게시글")
    void getPostLikeInfo() {
        // given
        Long postId = 1L;
        Long userId = 1L;
        Long expectedCount = 5L;
        List<Long> likedPostIds = Arrays.asList(1L);

        when(postLikeRepository.countActiveLikesByPostId(postId))
                .thenReturn(expectedCount);
        when(postLikeRepository.findLikedPostIdsByUserAndPostIds(List.of(postId), userId))
                .thenReturn(likedPostIds);

        // when
        PostLikeInfo result = postLikeQueryService.getPostLikeInfo(postId, userId);

        // then
        assertThat(result.likeCount()).isEqualTo(5L);
        assertThat(result.isLiked()).isTrue();
        verify(postLikeRepository).countActiveLikesByPostId(postId);
        verify(postLikeRepository).findLikedPostIdsByUserAndPostIds(List.of(postId), userId);
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
}
