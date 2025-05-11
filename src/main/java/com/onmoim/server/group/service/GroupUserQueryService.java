package com.onmoim.server.group.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.repository.GroupUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupUserQueryService {
	private final GroupUserRepository groupUserRepository;

	public void save(GroupUser groupUser) {
		groupUserRepository.save(groupUser);
	}

	public Optional<GroupUser> findById(Long groupId, Long userId) {
		return groupUserRepository.findGroupUser(groupId, userId);
	}
}
