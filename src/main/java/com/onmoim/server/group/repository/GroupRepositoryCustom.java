package com.onmoim.server.group.repository;

import java.util.List;

import com.onmoim.server.group.dto.GroupCommonInfo;
import com.onmoim.server.group.dto.GroupCommonSummary;
import com.onmoim.server.group.entity.GroupUser;

public interface GroupRepositoryCustom {
	Long countGroupMembers(Long groupId);
	List<GroupUser> findGroupUsers(Long groupId, Long cursorId, int size);
	List<GroupCommonSummary> readPopularGroupsNearMe(Long locationId, Long cursorId, Long memberCount, int size);
	List<GroupCommonInfo> readGroupsCommonInfo(List<Long> groupIds, Long userId);
}
