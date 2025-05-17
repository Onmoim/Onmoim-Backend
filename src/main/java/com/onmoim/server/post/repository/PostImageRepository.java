package com.onmoim.server.post.repository;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.entity.PostImageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, PostImageId> {
    List<PostImage> findAllByPost(GroupPost post);

    @Modifying
    @Query("UPDATE PostImage pi SET pi.isDeleted = true WHERE pi.post.id = :postId")
    void softDeleteAllByPostId(@Param("postId") Long postId);
}
