package com.onmoim.server.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.onmoim.server.post.entity.GroupPost;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long>, GroupPostRepositoryCustom {
}