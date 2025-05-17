package com.onmoim.server.post.repository;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;

/**
 * 모임 게시글을 위한 커스텀 레포지토리 인터페이스 (Querydsl 기능 확장용)
 */
public interface GroupPostRepositoryCustom {
    CursorPageResponseDto<GroupPost> findPosts(
            Group group,
            GroupPostType type,
            Long cursorId,
            int size
    );
}