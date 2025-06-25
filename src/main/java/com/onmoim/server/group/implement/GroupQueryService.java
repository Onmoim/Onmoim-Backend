package com.onmoim.server.group.implement;

import static com.onmoim.server.common.exception.ErrorCode.*;

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
import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.location.entity.Location;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupQueryService {
	private final GroupRepository groupRepository;
	private final GroupUserQueryService groupUserQueryService;
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
		} catch (DataIntegrityViolationException e) {
			throw new CustomException(ALREADY_EXISTS_GROUP);
		}
	}
	/**
	 * group 존재 X -> CustomException
	 * group 존재 O, deletedDate 존재 O -> CustomException
	 * group 존재 O, deletedDate 존재 X -> group 반환
	 */
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

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateGeoPoint(
		Long groupId,
		GeoPoint geoPoint
	) {
		Group group = getById(groupId);
		group.updateLocation(geoPoint);
	}
}
