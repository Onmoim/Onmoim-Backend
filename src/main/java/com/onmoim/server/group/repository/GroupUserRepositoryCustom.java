package com.onmoim.server.group.repository;

import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;

public interface GroupUserRepositoryCustom {

	CommonCursorPageResponseDto<GroupSummaryResponseDto> findJoinedGroupListByUserId(Long userId, Long cursorId, int size);

}
