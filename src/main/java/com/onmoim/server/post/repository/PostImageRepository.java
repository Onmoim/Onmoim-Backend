package com.onmoim.server.post.repository;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.entity.PostImageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, PostImageId> {
    List<PostImage> findAllByPost(GroupPost post);
    void deleteAllByPost(GroupPost post);
}
