package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupQueryService {
	private final GroupRepository groupRepository;

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
			.filter(Group::isActive)
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));
	}

	public void deleteGroup(Group group) {
		group.softDelete();
	}
}
