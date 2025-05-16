package com.onmoim.server.post.repository;

import com.onmoim.server.post.entity.GroupPost;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * 모임 게시글 레포지토리
 */
public interface GroupPostRepository extends JpaRepository<GroupPost, Long>, GroupPostRepositoryCustom {

}
