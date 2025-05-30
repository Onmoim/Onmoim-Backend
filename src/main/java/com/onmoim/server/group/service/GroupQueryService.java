package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.dto.request.GroupRequestDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.location.service.LocationQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupQueryService {
	private final GroupRepository groupRepository;
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
		System.out.println("getById");
		return groupRepository.findById(groupId)
			.filter(group -> !group.isDeleted())
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));
	}

	// 모임을 삭제합니다.
	public void deleteGroup(Group group) {
		group.softDelete();
	}

	// todo: 모임을 수정합니다.
	public void updateGroup(Group group, GroupRequestDto request, MultipartFile image) {

	}
}
