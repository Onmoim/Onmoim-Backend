package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.dto.response.CursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupMembersResponseDto;
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
			throw new CustomException(TOO_MANY_REQUEST);
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
			.orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND_IN_GROUP));
	}

	public GroupUser checkCanLeave(Long groupId, Long userId) {
		GroupUser groupUser = findById(groupId, userId)
			.filter(GroupUser::isJoined)
			.orElseThrow(() -> new CustomException(NOT_GROUP_MEMBER));

		if (groupUser.isOwner()) {
			throw new CustomException(GROUP_OWNER_TRANSFER_REQUIRED);
		}
		return groupUser;
	}

	public void leave(GroupUser groupUser) {
		groupUser.updateStatus(Status.PENDING);
	}

	public void transferOwnership(GroupUser owner, GroupUser member) {
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

	// fetch join 사용해서 모임 멤버 조회
	public CursorPageResponseDto<GroupMembersResponseDto> findGroupUserAndMembers(Long groupId, Long cursorId, int size) {
		List<GroupUser> result = groupUserRepository.findGroupUsers(groupId, cursorId, size);
		boolean hasNext = result.size() > size;
		extractPageContent(result, hasNext);
		List<GroupMembersResponseDto> list = result.stream()
			.map(GroupMembersResponseDto::new)
			.toList();
		Long totalCount = cursorId == null ? groupUserRepository.countGroupMembers(groupId) : null ;
		return CursorPageResponseDto.<GroupMembersResponseDto>builder()
			.content(list)
			.totalCount(totalCount)
			.hasNext(hasNext)
			.cursorId(list.isEmpty() ? null : list.getLast().getUserId())
			.build();
	}
	private void extractPageContent(List<GroupUser> result, boolean hasNext) {
		if (hasNext){
			result.removeLast();
		}
	}
}
