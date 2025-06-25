package com.onmoim.server.post.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostLike;
import com.onmoim.server.user.entity.User;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 사용자의 특정 게시글에 대한 좋아요 조회
     */
    Optional<PostLike> findByPostAndUser(GroupPost post, User user);

    /**
     * 특정 사용자의 특정 게시글에 대한 활성 좋아요 조회
     */
    @Query("SELECT pl FROM PostLike pl WHERE pl.post = :post AND pl.user = :user AND pl.deletedDate IS NULL")
    Optional<PostLike> findByPostAndUserAndActive(@Param("post") GroupPost post, @Param("user") User user);

    /**
     * 특정 게시글의 활성 좋아요 수 조회
     */
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId AND pl.deletedDate IS NULL")
    Long countActiveLikesByPostId(@Param("postId") Long postId);

    /**
     * 여러 게시글의 활성 좋아요 수를 한 번에 조회
     */
    @Query("SELECT pl.post.id as postId, COUNT(pl) as likeCount " +
           "FROM PostLike pl " +
           "WHERE pl.post.id IN :postIds AND pl.deletedDate IS NULL " +
           "GROUP BY pl.post.id")
    List<PostLikeCountProjection> countActiveLikesByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * 특정 사용자가 여러 게시글에 좋아요를 했는지 확인
     */
    @Query("SELECT pl.post.id FROM PostLike pl " +
           "WHERE pl.post.id IN :postIds AND pl.user.id = :userId AND pl.deletedDate IS NULL")
    List<Long> findLikedPostIdsByUserAndPostIds(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);

    /**
     * 좋아요 수 집계를 위한 인터페이스
     */
    interface PostLikeCountProjection {
        Long getPostId();
        Long getLikeCount();
    }
}
