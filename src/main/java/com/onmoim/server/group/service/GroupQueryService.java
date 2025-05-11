package com.onmoim.server.group.service;

import org.springframework.stereotype.Service;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupQueryService {
	private final GroupRepository groupRepository;

	public Long saveGroup(Group group) {
		try {
			groupRepository.save(group);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return group.getId();
	}
}
