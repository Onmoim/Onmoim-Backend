package com.onmoim.server.group.service;

import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.common.kakaomap.GeoPointUpdateEvent;
import com.onmoim.server.group.aop.NamedLock;
import com.onmoim.server.group.aop.Retry;
import com.onmoim.server.group.dto.request.GroupCreateRequestDto;
import com.onmoim.server.group.dto.response.CursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupMembersResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.service.LocationQueryService;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {
	private final GroupQueryService groupQueryService;
	private final GroupUserQueryService groupUserQueryService;
	private final UserQueryService userQueryService;
	private final LocationQueryService locationQueryService;
	private final CategoryQueryService categoryQueryService;
	private final GroupUserRepository groupUserRepository;
	private final ApplicationEventPublisher eventPublisher;

	// 모임 생성
	@Transactional
	public Long createGroup(
			Long categoryId,
			Long locationId,
			String name,
			String description,
			int capacity
	) {
		User user = userQueryService.findById(getCurrentUserId());
		Category category = categoryQueryService.getById(categoryId);
		Location location = locationQueryService.getById(locationId);

		Group group = groupQueryService.saveGroup(category, location, name, description, capacity);

		GroupUser groupUser = GroupUser.create(group, user, Status.OWNER);
		groupUserQueryService.save(groupUser);

		String address = location.getFullAddress();

		eventPublisher.publishEvent(new GeoPointUpdateEvent(group.getId(), address));
		return group.getId();
	}

	/**
	 * 모임 가입
	 * 이미 가입된 상태 (MEMBER, OWNER) -> 에러 처리
	 * 벤 상태 (BAN) -> 에러 처리
	 * BOOKMARK or 바로 가입 -> 정원 파악 이후 가입 처리
	 * NamedLock -> 네임드 락 획득 이후 시도
	 */
	@Retry
	@NamedLock
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void joinGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 관계가 없었던 경우 PENDING 상태
		GroupUser groupUser = groupUserQueryService.findOrCreate(group, user, Status.PENDING);
		// OWNER, MEMBER, BAN 검사
		groupUser.joinValidate();
		// 현재 모임원 숫자 조회
		Long current = groupUserRepository.countByGroupAndStatuses(groupId, List.of(Status.MEMBER, Status.OWNER));
		// 정원 초과 검사
		group.join(current);
		// 모임 검사
		groupUserQueryService.joinGroup(groupUser);
	}

	// 모임 찜 또는 찜 취소
	@Retry
	@Transactional
	public void likeGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 관계가 없었던 경우 PENDING 상태
		GroupUser groupUser = groupUserQueryService.findOrCreate(group, user, Status.PENDING);
		// 찜하기 또는 취소
		groupUserQueryService.likeGroup(groupUser);
	}

	// 모임 회원 조회
	@Transactional(readOnly = true)
	public CursorPageResponseDto<GroupMembersResponseDto> getGroupMembers(
		Long groupId,
		Long cursorId,
		int size
	) {
		groupQueryService.existsById(groupId);
		return groupUserQueryService.findGroupUserAndMembers(groupId, cursorId, size);
	}

	// 모임 삭제
	@Transactional
	public void deleteGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 모임장 권한 확인
		groupUserQueryService.checkOwner(groupId, user.getId());
		// 모임 삭제
		groupQueryService.deleteGroup(group);
	}

	// 모임 탈퇴
	@Retry
	@NamedLock
	@Transactional
	public void leaveGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		groupQueryService.existsById(groupId);
		// 탈퇴 가능 여부 확인
		GroupUser groupUser = groupUserQueryService.checkCanLeave(groupId, user.getId());
		// 모임 탈퇴
		groupUserQueryService.leave(groupUser);
	}

	// 모임장 위임
	@Retry
	@Transactional
	public void transferOwnership(
		Long groupId,
		Long userId
	) {
		// 현재 모임장 조회
		User from = userQueryService.findById(getCurrentUserId());
		// 권한 위임 대상 회원 조회
		User to = userQueryService.findById(userId);
		// 모임 조회
		groupQueryService.existsById(groupId);
		// 현재 모임장 확인
		GroupUser owner = groupUserQueryService.checkAndGetOwner(groupId, from.getId());
		// 권한 위임 대상 확인
		GroupUser user = groupUserQueryService.checkAndGetMember(groupId, to.getId());
		// 권한 위임
		groupUserQueryService.transferOwnership(owner, user);
	}

	// 모임원 강퇴
	@Retry
	@Transactional
	public void banMember(
		Long groupId,
		Long userId
	) {
		// 현재 모임장 조회
		User from = userQueryService.findById(getCurrentUserId());
		// 강퇴 대상 조회
		userQueryService.findById(userId);
		// 모임 조회
		groupQueryService.existsById(groupId);
		// 현재 모임장 확인
		groupUserQueryService.checkOwner(groupId, from.getId());
		// 강태 대상 확인
		GroupUser user = groupUserQueryService.checkAndGetMember(groupId, userId);
		// 강퇴
		groupQueryService.banMember(user);
	}

	// 모임 수정
	@NamedLock
	@Transactional
	public void updateGroup(
		Long groupId,
		String description,
		int capacity,
		MultipartFile image
	) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 모임장 권한 확인
		groupUserQueryService.checkOwner(groupId, user.getId());
		// 업데이트
		groupQueryService.updateGroup(group, description, capacity, image);
	}

	// 현재 사용자
	private Long getCurrentUserId() {
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}
