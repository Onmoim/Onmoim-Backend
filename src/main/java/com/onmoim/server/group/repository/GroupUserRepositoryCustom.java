package com.onmoim.server.group.repository;

import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.JoinedGroupResponseDto;

public interface GroupUserRepositoryCustom {

	CommonCursorPageResponseDto<JoinedGroupResponseDto> findJoinedGroupListByUserId(Long userId, Long cursorId, int size);

}
