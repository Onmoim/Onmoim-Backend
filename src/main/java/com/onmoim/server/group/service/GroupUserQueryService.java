package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupUserQueryService {
	private final GroupUserRepository groupUserRepository;

	public void save(GroupUser groupUser) {
		try {
			groupUserRepository.save(groupUser);
		} catch (DataIntegrityViolationException e) {
			throw new CustomException(TOO_MANY_REQUESTS);
		}
	}

	public Optional<GroupUser> findById(Long groupId, Long userId) {
		return groupUserRepository.findGroupUser(groupId, userId);
	}

	public GroupUser findOrCreate(Group group, User user, Status status) {
		return findById(group.getId(), user.getId())
			.orElseGet(() -> GroupUser.create(group, user, status));
	}

	public GroupUser checkAndGetOwner(Long groupId, Long userId) {
		return findById(groupId, userId)
			.filter(GroupUser::isOwner)
			.orElseThrow(() -> new CustomException(GROUP_FORBIDDEN));
	}

	public GroupUser checkAndGetMember(Long groupId, Long userId) {
		return findById(groupId, userId)
			.filter(GroupUser::isMember)
			.orElseThrow(() -> new CustomException(INVALID_USER));
	}

	public GroupUser checkCanLeave(Long groupId, Long userId) {
		GroupUser groupUser = findById(groupId, userId)
			.filter(GroupUser::isJoined)
			.orElseThrow(() -> new CustomException(INVALID_USER));

		if (groupUser.isOwner()) {
			throw new CustomException(GROUP_OWNER_TRANSFER_REQUIRED);
		}
		return groupUser;
	}

	public void leave(GroupUser groupUser) {
		groupUser.updateStatus(Status.PENDING);
	}

	public void transferOwnerShip(GroupUser owner, GroupUser member) {
		owner.updateStatus(Status.MEMBER);
		member.updateStatus(Status.OWNER);
	}

	// 회원가입 변경감지(BOOKMARK -> MEMBER), 신규 저장(PENDING -> MEMBER)
	public void joinGroup(GroupUser groupUser) {
		Status prevStatus = groupUser.getStatus();
		groupUser.updateStatus(Status.MEMBER);
		if (prevStatus == Status.PENDING) {
			save(groupUser);
		}
	}

	// 북마크 취소(BOOKMARK -> PENDING) 신규 저장(PENDING -> BOOKMARK)
	public void likeGroup(GroupUser groupUser) {
		Status prevStatus = groupUser.getStatus();
		groupUser.like();
		if (prevStatus == Status.PENDING) {
			save(groupUser);
		}
	}

	// fetch join 사용해서 User 조회
	public List<GroupUser> findGroupUserAndMembers(Long groupId) {
		return groupUserRepository.findGroupUserAndMembers(groupId);
	}
}
