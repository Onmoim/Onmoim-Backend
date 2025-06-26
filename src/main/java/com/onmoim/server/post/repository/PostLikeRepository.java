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
    @Query("SELECT pl FROM PostLike pl WHERE pl.post = :post AND pl.user = :user")
    Optional<PostLike> findByPostAndUser(@Param("post") GroupPost post, @Param("user") User user);



    /**
     * 특정 게시글들에 대한 모든 좋아요 조회
     */
    @Query("SELECT pl FROM PostLike pl WHERE pl.post.id IN :postIds")
    List<PostLike> findByPostIdIn(@Param("postIds") List<Long> postIds);


}
