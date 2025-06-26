package com.onmoim.server.post.repository;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;

public interface GroupPostRepositoryCustom {

    /**
     * 게시글 목록과 이미지, 좋아요 정보를 함께 조회
     */
    CursorPageResponseDto<GroupPostResponseDto> findPostsWithImagesAndLikes(Group group, GroupPostType type, Long cursorId, int size, Long userId);
}
