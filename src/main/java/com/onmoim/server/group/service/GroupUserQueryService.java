package com.onmoim.server.group.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	// 회원가입 변경감지(BOOKMARK -> MEMBER), 신규 저장(PENDING -> MEMBER)
	public void joinGroup(GroupUser groupUser) {
		Status prevStatus = groupUser.getStatus();
		groupUser.updateStatus(Status.MEMBER);
		if (prevStatus == Status.PENDING) {
			save(groupUser);
		}
	}
}
