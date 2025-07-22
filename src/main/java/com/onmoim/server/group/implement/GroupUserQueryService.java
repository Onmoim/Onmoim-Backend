package com.onmoim.server.group.implement;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static com.onmoim.server.group.entity.GroupLikeStatus.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.security.CustomUserDetails;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.dto.GroupMember;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupLike;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.GroupUserId;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupLikeRepository;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.user.entity.User;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupUserQueryService {
	private final GroupUserRepository groupUserRepository;
	private final GroupRepository groupRepository;
	private final GroupLikeRepository groupLikeRepository;

	public void groupUserSave(GroupUser groupUser) {
		try {
			groupUserRepository.save(groupUser);
		} catch (DataIntegrityViolationException e) {
			throw new CustomException(TOO_MANY_REQUEST);
		}
	}

	public GroupUser findOrCreateForUpdate(
		Group group,
		User user,
		Status status
	)
	{
		return findGroupUserForUpdate(group.getId(), user.getId()).
			orElseGet(() -> GroupUser.create(group, user, status));
	}

	/**
	 * select for update .. 비관적 락(쓰기 락)
	 * mysql 락 타임아웃 설정은 쿼리 문장으로 제어 불가능
	 */
	public Optional<GroupUser> findGroupUserForUpdate(
		Long groupId,
		Long userId
	)
	{
		try {
			return groupUserRepository.findGroupUserForUpdate(groupId, userId);
		}
		catch (CannotAcquireLockException e){
			throw new CustomException(TOO_MANY_REQUEST);
		}
	}

	public Optional<GroupUser> findById(
		Long groupId,
		Long userId
	)
	{
		return groupUserRepository.findGroupUser(groupId, userId);
	}

	public GroupUser getById(
		Long groupId,
		Long userId
	)
	{
		return findById(groupId, userId)
			.orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND_IN_GROUP));
	}

	public void checkJoined(Long groupId, Long userId){
		 findAndValidate(
			 groupId,
			 userId,
			 GroupUser::isJoined,
			 () -> new CustomException(GROUP_FORBIDDEN)
		 );
	}

	private GroupUser findAndValidate(
		Long groupId,
		Long userId,
		Predicate<GroupUser> predicate,
		Supplier<CustomException> exceptionSupplier
	)
	{
		return findById(groupId, userId)
			.filter(predicate)
			.orElseThrow(exceptionSupplier);
	}

	public void checkCanLeave(GroupUser groupUser) {
		groupUser.checkGroupMember();
		// 현재 사용자 모임장 + 모임 회원 2명 이상
		if (groupUser.isOwner() && countMembers(groupUser.getGroup().getId()) > 1) {
			throw new CustomException(GROUP_OWNER_TRANSFER_REQUIRED);
		}
	}

	public void leave(GroupUser groupUser) {
		// 현재 사용자 모임장 바로 모임 삭제
		if (groupUser.isOwner()) {
			groupUser.getGroup().softDelete();
			return;
		}
		groupUser.updateStatus(Status.PENDING);
	}

	// 모임장 양도
	public void transferOwnership(
		GroupUser owner,
		GroupUser member
	)
	{
		owner.updateStatus(Status.MEMBER);
		member.updateStatus(Status.OWNER);
	}

	// 회원가입 변경감지(BOOKMARK -> MEMBER), 신규 저장(PENDING -> MEMBER)
	public void joinGroup(GroupUser groupUser) {
		Status prevStatus = groupUser.getStatus();
		groupUser.updateStatus(Status.MEMBER);
		if (prevStatus == Status.PENDING) {
			groupUserSave(groupUser);
		}
	}

	private void groupLikeSave(GroupLike groupLike) {
		try {
			groupLikeRepository.save(groupLike);
		}
		catch (DataIntegrityViolationException e) {
			throw new CustomException(TOO_MANY_REQUEST);
		}
	}

	// 모임-좋아요 조회
	public GroupLike findOrCreateLike(
		Group group,
		User user
	)
	{
		return groupLikeRepository.findById(new GroupUserId(group.getId(), user.getId()))
			.orElseGet(() -> GroupLike.create(group, user, NEW));
	}

	// 좋아요 취소(LIKE-> PENDING) / 신규 저장(NEW -> LIKE) / 좋아요 등록(PENDING -> LIKE)
	public GroupLikeStatus likeGroup(GroupLike groupLike) {
		GroupLikeStatus prevStatus = groupLike.getStatus();
		GroupLikeStatus status = groupLike.updateStatus();
		if(prevStatus == NEW) {
			groupLikeSave(groupLike);
		}
		return status;
	}

	// 현재 모임 회원 수
	public Long countMembers(Long groupId) {
		return groupRepository.countGroupMembers(groupId);
	}

	// fetch join 사용해서 모임 멤버 조회
	public List<GroupMember> findGroupUserAndMembers(
		Long groupId,
		@Nullable Long lastGroupId,
		int size
	)
	{
		List<GroupUser> groupUsers = groupRepository.findGroupUsers(groupId, lastGroupId, size);
		return groupUsers.stream().map(GroupMember::of).toList();
	}

	/**
	 * 가입한 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getJoinedGroups(Long cursorId, int size) {
		Long userId = getCurrentUserId();

		return groupUserRepository.findJoinedGroupListByUserId(userId, cursorId, size);
	}

	public Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}

		Object principal = auth.getPrincipal();

		if (principal instanceof CustomUserDetails userDetails) {
			return userDetails.getUserId();
		} else {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}
	}
}
