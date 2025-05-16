package com.onmoim.server.post.repository;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;

/**
 * 모임 게시글을 위한 커스텀 레포지토리 인터페이스 (Querydsl 기능 확장용)
 */
public interface GroupPostRepositoryCustom {
    
    /**
     * 커서 기반 페이징으로 게시글 목록 조회
     */
    CursorPageResponseDto<GroupPost> findPostsWithCursor(Group group, GroupPostType type, Long cursorId, int size);
} 