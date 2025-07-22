package com.onmoim.server.group.repository;

import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;

public interface GroupLikeRepositoryCustom {

	CommonCursorPageResponseDto<GroupSummaryResponseDto> findLikedGroupListByUserId(Long userId, Long cursorId, int size);

}
