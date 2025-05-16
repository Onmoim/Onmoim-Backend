package com.onmoim.server.post.repository;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.entity.PostImageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, PostImageId> {
    List<PostImage> findAllByPost(GroupPost post);
    void deleteAllByPost(GroupPost post);
} 