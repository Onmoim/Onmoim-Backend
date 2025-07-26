package com.onmoim.server.group.repository;

import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;

import java.util.List;

public interface GroupLikeRepositoryCustom {

	List<GroupSummaryResponseDto> findLikedGroupList(Long userId, Long cursorId, int size);

}
