package com.onmoim.server.group.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.entity.GroupUser;

public interface GroupRepositoryCustom {
	Optional<GroupDetail> readGroupDetail(Long groupId, Long userId);
	Long countGroupMembers(Long groupId);
	List<GroupUser> findGroupUsers(Long groupId, Long lastMemberId, int size);
	List<PopularGroupSummary> readPopularGroupsNearMe(Long locationId, Long lastGroupId, Long memberCount, int size);
	List<PopularGroupRelation> readPopularGroupRelation(List<Long> groupIds, Long userId);
	List<ActiveGroup> readMostActiveGroups(Long lastGroupId, Long meetingCount, int size);
	List<ActiveGroupDetail> readGroupDetails(List<Long> groupIds);
	List<ActiveGroupRelation> readGroupsRelation(List<Long> groupIds, Long userId);
	Long readAnnualScheduleCount (Long groupId, LocalDateTime now);
	Long readMonthlyScheduleCount (Long groupId, LocalDateTime now);
}
