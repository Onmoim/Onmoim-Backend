package com.onmoim.server.group.implement;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.group.dto.response.GroupSummaryByCategoryResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.dto.response.RecentViewedGroupSummaryResponseDto;
import com.onmoim.server.group.dto.response.cursor.RecentViewCursorPageResponseDto;
import com.onmoim.server.group.repository.GroupViewLogRepository;
import com.onmoim.server.security.CustomUserDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.location.entity.Location;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupQueryService {
	private final GroupRepository groupRepository;
	private final GroupViewLogRepository groupViewLogRepository;
	private final FileStorageService fileStorageService;

	public Group saveGroup(
		Category category,
		Location location,
		String name,
		String description,
		int capacity
	) {
		Group group = Group.builder()
			.category(category)
			.location(location)
			.name(name)
			.description(description)
			.capacity(capacity)
			.build();

		try {
			return groupRepository.save(group);
		}
		catch (DataIntegrityViolationException e) {
			throw new CustomException(ALREADY_EXISTS_GROUP);
		}
	}

	public Group getById(Long groupId) {
		return findActiveGroup(groupId);
	}

	public void existsById(Long groupId) {
		findActiveGroup(groupId);
	}

	private Group findActiveGroup(Long groupId) {
		return groupRepository.findById(groupId)
			.filter(group -> !group.isDeleted())
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));
	}

	// 모임 상세 조회
	public GroupDetail readGroupDetail(Long groupId, Long userId) {
		return groupRepository.readGroupDetail(groupId, userId)
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));
	}

	public Group getGroupWithDetails(Long groupId) {
		return groupRepository.findGroupWithDetails(groupId)
			.filter(group -> !group.isDeleted())
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));
	}

	// 모임을 삭제합니다.
	public void deleteGroup(Group group) {
		group.softDelete();
	}

	// 모임 수정
	public void updateGroup(
		Group group,
		String description,
		int capacity,
		MultipartFile image
	) {
		// 현재 모임원 숫자
		Long currentMember = groupRepository.countGroupMembers(group.getId());

		// 모임 설명, 모임 정원 변경
		group.update(description, capacity, currentMember);

		// 모임 이미지 변경
		if(image != null && !image.isEmpty()) {
			FileUploadResponseDto uploadResponse = fileStorageService.uploadFile(image, "images/group");
			group.updateImage(uploadResponse.getFileUrl());
		}
	}

	// 내 주변 인기 모임 조회
	public List<PopularGroupSummary> readPopularGroupsNearMe(
		Long locationId,
		@Nullable Long lastGroupId,
		@Nullable Long memberCount,
		int size
	)
	{
		return groupRepository.readPopularGroupsNearMe(
			locationId,
			lastGroupId,
			memberCount,
			size);
	}

	public List<PopularGroupRelation> readPopularGroupRelation(
		List<Long> groupIds,
		Long userId
	)
	{
		return groupRepository.readPopularGroupRelation(
			groupIds,
			userId);
	}

	// 활동이 활발한 모임 조회
	public List<ActiveGroup> readMostActiveGroups(
		@Nullable Long lastGroupId,
		@Nullable Long meetingCount,
		int size
	)
	{
		return groupRepository.readMostActiveGroups(
			lastGroupId,
			meetingCount,
			size
		);
	}

	public List<ActiveGroupDetail> readGroupsDetail(List<Long> groupIds) {
		return groupRepository.readGroupDetails(groupIds);
	}

	public List<ActiveGroupRelation> readGroupsRelation(
		List<Long> groupIds,
		Long userId
	)
	{
		return groupRepository.readGroupsRelation(groupIds, userId);
	}

	// 모임 연간 일정 개수
	public Long readAnnualScheduleCount(Long groupId, LocalDateTime now) {
		return groupRepository.readAnnualScheduleCount(groupId, now);
	}

	// 모임 월간 일전 개수
	public Long readMonthlyScheduleCount(Long groupId, LocalDateTime now) {
		return groupRepository.readMonthlyScheduleCount(groupId, now);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateGeoPoint(
		Long groupId,
		GeoPoint geoPoint
	) {
		Group group = getById(groupId);
		group.updateLocation(geoPoint);
	}

	/**
	 * 나와 비슷한 관심사 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getRecommendedGroupsByCategory(Long cursorId, int size) {
		Long userId = getCurrentUserId();

		List<GroupSummaryResponseDto> result = groupRepository.findRecommendedGroupListByCategory(userId, cursorId, size);

		if (result.isEmpty()) {
			return CommonCursorPageResponseDto.empty();
		}

		boolean hasNext = result.size() > size;
		List<GroupSummaryResponseDto> content = hasNext ? result.subList(0, size) : result;
		Long nextCursorId = hasNext ? content.get(content.size() - 1).getGroupId() : null;

		return CommonCursorPageResponseDto.of(content, hasNext, nextCursorId);
	}

	/**
	 * 나와 가까운 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getRecommendedGroupsByLocation(Long cursorId, int size) {
		Long userId = getCurrentUserId();

		List<GroupSummaryResponseDto> result = groupRepository.findRecommendedGroupListByLocation(userId, cursorId, size);

		if (result.isEmpty()) {
			return CommonCursorPageResponseDto.empty();
		}

		boolean hasNext = result.size() > size;
		List<GroupSummaryResponseDto> content = hasNext ? result.subList(0, size) : result;
		Long nextCursorId = hasNext ? content.get(content.size() - 1).getGroupId() : null;

		return CommonCursorPageResponseDto.of(content, hasNext, nextCursorId);
	}

	/**
	 * 최근 본 모임 조회
	 */
	public RecentViewCursorPageResponseDto<RecentViewedGroupSummaryResponseDto> getRecentViewedGroups(LocalDateTime cursorViewedAt, Long cursorLogId, int size) {
		Long userId = getCurrentUserId();

		List<RecentViewedGroupSummaryResponseDto> result = groupViewLogRepository.findRecentViewedGroupList(userId, cursorViewedAt, cursorLogId, size + 1);

		if (result.isEmpty()) {
			return RecentViewCursorPageResponseDto.empty();
		}

		boolean hasNext = result.size() > size;
		List<RecentViewedGroupSummaryResponseDto> content = hasNext ? result.subList(0, size) : result;

		// 추천 여부 삽입
		Set<Long> recommendedGroupIds = groupRepository.findRecommendedGroupIds(userId);

		for (RecentViewedGroupSummaryResponseDto dto : content) {
			if (recommendedGroupIds.contains(dto.getGroupId())) {
				dto.setRecommendStatus("RECOMMEND");
			} else {
				dto.setRecommendStatus("NONE");
			}
		}

		// 커서 추출
		LocalDateTime nextCursorViewedAt = hasNext ? content.get(content.size() - 1).getViewedAt() : null;
		Long nextCursorLogId = hasNext ? content.get(content.size() - 1).getGroupId() : null;

		return RecentViewCursorPageResponseDto.of(content, hasNext, nextCursorViewedAt, nextCursorLogId);
	}

	/**
	 * 카테고리별 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryByCategoryResponseDto> getGroupsByCategory(Long categoryId, Long cursorId, int size) {
		Long userId = getCurrentUserId();

		List<GroupSummaryByCategoryResponseDto> result = groupRepository.findGroupListByCategory(categoryId, userId, cursorId, size);

		if (result.isEmpty()) {
			return CommonCursorPageResponseDto.empty();
		}

		boolean hasNext = result.size() > size;
		List<GroupSummaryByCategoryResponseDto> content = hasNext ? result.subList(0, size) : result;

		// 추천 여부 삽입
		Set<Long> recommendedGroupIds = groupRepository.findRecommendedGroupIds(userId);

		for (GroupSummaryByCategoryResponseDto dto : content) {
			if (recommendedGroupIds.contains(dto.getGroupId())) {
				dto.setRecommendStatus("RECOMMEND");
			} else {
				dto.setRecommendStatus("NONE");
			}
		}

		Long nextCursorId = hasNext ? content.get(content.size() - 1).getGroupId() : null;

		return CommonCursorPageResponseDto.of(content, hasNext, nextCursorId);
	}


	public Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}

		Object principal = auth.getPrincipal();

		if (principal instanceof CustomUserDetails userDetails) {
			return userDetails.getUserId();
		} else {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}
	}
}
