package com.onmoim.server.group.repository;

import com.onmoim.server.group.dto.response.RecentViewedGroupSummaryResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface GroupViewLogRepositoryCustom {

	List<RecentViewedGroupSummaryResponseDto> findRecentViewedGroupList(Long userId, LocalDateTime cursorViewedAt, Long cursorId, int size);

	Long countRecentViewedGroups(Long userId);

}
