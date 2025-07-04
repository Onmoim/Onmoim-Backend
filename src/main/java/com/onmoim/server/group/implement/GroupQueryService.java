package com.onmoim.server.group.implement;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.S3FileStorageService;
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
	private final S3FileStorageService s3FileStorageService;

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
	public GroupDetail getGroupWithDetails(Long groupId) {
		return GroupDetail.of(groupRepository.findGroupWithDetails(groupId)
			.filter(group -> !group.isDeleted())
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP)));
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
			FileUploadResponseDto uploadResponse = s3FileStorageService.uploadFile(image, "group");
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
		@Nullable Long memberCount,
		int size
	)
	{
		return groupRepository.readMostActiveGroups(
			lastGroupId,
			memberCount,
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
}
