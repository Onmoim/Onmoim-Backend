package com.onmoim.server.group.repository;

import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;

import java.util.List;

public interface GroupUserRepositoryCustom {

	List<GroupSummaryResponseDto> findJoinedGroupList(Long userId, Long cursorId, int size);

	Long countJoinedGroups(Long userId);

}
