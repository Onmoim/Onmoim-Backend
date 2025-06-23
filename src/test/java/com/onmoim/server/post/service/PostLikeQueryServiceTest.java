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
import com.onmoim.server.post.service.PostLikeQueryService.PostLikeInfo;

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
    @DisplayName("게시글별 좋아요 수 배치 조회 테스트")
    void getLikeCountMap() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L);
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
        Map<Long, Long> result = postLikeQueryService.getLikeCountMap(postIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo(5L);
        assertThat(result.get(2L)).isEqualTo(3L);
        verify(postLikeRepository).countActiveLikesByPostIds(postIds);
    }

    @Test
    @DisplayName("사용자의 게시글별 좋아요 상태 배치 조회 테스트")
    void getLikedStatusMap() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L);
        Long userId = 1L;
        List<Long> mockLikedPostIds = Arrays.asList(1L); // 1번 게시글만 좋아요

        when(postLikeRepository.findLikedPostIdsByUserAndPostIds(postIds, userId))
                .thenReturn(mockLikedPostIds);

        // when
        Map<Long, Boolean> result = postLikeQueryService.getLikedStatusMap(postIds, userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isTrue();  // 1번 게시글은 좋아요
        assertThat(result.get(2L)).isFalse(); // 2번 게시글은 좋아요 안함
        verify(postLikeRepository).findLikedPostIdsByUserAndPostIds(postIds, userId);
    }

    @Test
    @DisplayName("비로그인 사용자의 좋아요 상태 조회 시 모두 false 반환")
    void getLikedStatusMapWithNullUser() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L);
        Long nullUserId = null;

        // when
        Map<Long, Boolean> result = postLikeQueryService.getLikedStatusMap(postIds, nullUserId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isFalse();
        assertThat(result.get(2L)).isFalse();
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
    void getLikeCountMapEmpty() {
        // given
        List<Long> emptyPostIds = Arrays.asList();

        // when
        Map<Long, Long> result = postLikeQueryService.getLikeCountMap(emptyPostIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 게시글 ID 리스트로 좋아요 상태 조회 시 빈 결과 반환")
    void getLikedStatusMapEmpty() {
        // given
        List<Long> emptyPostIds = Arrays.asList();
        Long userId = 1L;

        // when
        Map<Long, Boolean> result = postLikeQueryService.getLikedStatusMap(emptyPostIds, userId);

        // then
        assertThat(result).isEmpty();
    }
}
