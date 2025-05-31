package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.S3FileStorageService;
import com.onmoim.server.group.dto.request.GroupRequestDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.service.LocationQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupQueryService {
	private final GroupRepository groupRepository;
	private final GroupUserQueryService groupUserQueryService;
	private final S3FileStorageService s3FileStorageService;
	private final CategoryQueryService categoryQueryService;
	private final LocationQueryService locationQueryService;

	public void saveGroup(Group group) {
		try {
			groupRepository.save(group);
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
		return groupRepository.findById(groupId)
			.filter(group -> !group.isDeleted())
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));
	}

	public Group getGroupWithRelations(Long groupId) {
		return groupRepository.findGroupWithRelations(groupId)
			.filter(group -> !group.isDeleted())
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));
	}

	// 모임을 삭제합니다.
	public void deleteGroup(Group group) {
		group.softDelete();
	}

	// 모임 업데이트
	public void updateGroup(Group group, GroupRequestDto request, MultipartFile image) {
		// 현재 모임원 숫자
		Long currentMember = groupUserQueryService.countMembers(group.getId());

		// 요청 카테고리
		Category category = categoryQueryService.getById(request.getCategoryId());
		// 모임 이름, 모임 설명, 모임 정원, 카테고리 변경
		group.update(currentMember, request.getName(),
			request.getDescription(), request.getCapacity(), category);

		// 모임 이미지 변경
		if(!image.isEmpty()) {
			FileUploadResponseDto uploadResponse = s3FileStorageService.uploadFile(image, "group");
			group.updateImage(uploadResponse.getFileUrl());
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateGeoPoint(Long groupId, Long locationId, GeoPoint geoPoint) {
		Group group = getById(groupId);
		Location location = locationQueryService.getById(locationId);
		group.updateLocation(location, geoPoint);
	}
}
