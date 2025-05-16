package com.onmoim.server.group.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.group.aop.NamedLock;
import com.onmoim.server.group.dto.request.CreateGroupRequestDto;
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

	@Transactional
	public Long createGroup(CreateGroupRequestDto request) {
		User user = userQueryService.findById(getCurrentUserId());
		Location location = locationQueryService.getById(request.getLocationId());
		Category category = categoryQueryService.getById(request.getCategoryId());

		Group group = Group.groupCreateBuilder()
			.name(request.getName())
			.description(request.getDescription())
			.capacity(request.getCapacity())
			.location(location)
			.category(category)
			.build();
		groupQueryService.saveGroup(group);

		GroupUser groupUser = GroupUser.create(group, user, Status.OWNER);
		groupUserQueryService.save(groupUser);
		return group.getId();
	}

	/**
	 * 모임 가입
	 * 이미 가입된 상태 (MEMBER, OWNER) -> 에러 처리
	 * 벤 상태 (BAN) -> 에러 처리
	 * BOOKMARK or 바로 가입 -> 정원 파악 이후 가입 처리
	 * @NaemdLock -> 네임드 락 획득 이후 시도
	 */
	@NamedLock
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void joinGroup(Long groupId) {
		User user = userQueryService.findById(getCurrentUserId());
		Group group = groupQueryService.getById(groupId);
		// 관계가 없었던 경우 PENDING 상태
		GroupUser groupUser = groupUserQueryService.findById(groupId, user.getId())
			.orElseGet(() -> GroupUser.create(group, user, Status.PENDING));
		// OWNER, MEMBER, BAN 검사
		groupUser.joinValidate();
		// 현재 모임원 숫자 조회
		Long current = groupUserRepository.countGroupMember(groupId, List.of(Status.MEMBER, Status.OWNER));
		// 정원 초과 검사
		group.join(current);
		groupUserQueryService.joinGroup(groupUser);
	}

	private Long getCurrentUserId()	{
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}
