package com.onmoim.server.group.service;

import java.time.LocalDateTime;
import java.util.List;

import com.onmoim.server.chat.domain.enums.ChatSystemMessageTemplate;
import com.onmoim.server.chat.messaging.ChatSystemMessageEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.common.kakaomap.GeoPointUpdateEvent;
import com.onmoim.server.chat.domain.dto.ChatRoomResponse;
import com.onmoim.server.chat.service.ChatMessageService;
import com.onmoim.server.chat.service.ChatRoomService;
import com.onmoim.server.group.aop.NamedLock;
import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.dto.GroupMember;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.group.implement.GroupUserQueryService;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.service.LocationQueryService;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

import jakarta.annotation.Nullable;
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

	private final ApplicationEventPublisher eventPublisher;

	private final ChatRoomService chatRoomService;
	private final ChatMessageService chatMessageService;

	// 모임 생성
	@Transactional
	public ChatRoomResponse createGroup(
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

		ChatRoomResponse room = chatRoomService.createRoom(group.getId(), name, description, user.getId());
		eventPublisher.publishEvent(new ChatSystemMessageEvent(group.getId(), ChatSystemMessageTemplate.CREATE_CHAT_ROOM));

		GroupUser groupUser = GroupUser.create(group, user, Status.OWNER);
		groupUserQueryService.save(groupUser);

		String address = location.getFullAddress();

		eventPublisher.publishEvent(new GeoPointUpdateEvent(group.getId(), address));
		return room;
	}

	// 모임 가입
	@NamedLock
	@Transactional
	public void joinGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 관계가 없었던 경우 PENDING 상태 + 비관적 락
		GroupUser groupUser = groupUserQueryService.findOrCreateForUpdate(group, user, Status.PENDING);
		// OWNER, MEMBER, BAN 검사
		groupUser.joinValidate();
		// 현재 모임원 숫자 조회
		Long current = groupUserQueryService.countMembers(groupId);
		// 정원 초과 검사
		group.join(current);
		// 모임 검사
		groupUserQueryService.joinGroup(groupUser);
	}

	// 모임 찜 또는 찜 취소
	@Transactional
	public void likeGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 관계가 없었던 경우 PENDING 상태 + 비관적 락
		GroupUser groupUser = groupUserQueryService.findOrCreateForUpdate(group, user, Status.PENDING);
		// 찜하기 또는 취소
		groupUserQueryService.likeGroup(groupUser);
	}

	// 모임원 조회
	@Transactional(readOnly = true)
	public List<GroupMember> readGroupMembers(
		Long groupId,
		@Nullable Long lastGroupId,
		int size
	) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 존재 확인
		groupQueryService.existsById(groupId);
		// 모인원 확인
		groupUserQueryService.checkJoined(groupId, user.getId());
		// 모임원 조회
		return groupUserQueryService.findGroupUserAndMembers(groupId, lastGroupId, size);
	}

	// 모임 전체 회원 수 조회
	@Transactional(readOnly = true)
	public Long groupMemberCount(Long groupId) {
		// 회원 수 조회
		return groupUserQueryService.countMembers(groupId);
	}

	// 모임 삭제
	@Transactional
	public void deleteGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 관계가 없었던 경우 PENDING 상태 + 비관적 락
		GroupUser groupUser = groupUserQueryService.findOrCreateForUpdate(group, user, Status.PENDING);
		groupUser.checkOwner();
		// 모임 삭제
		groupQueryService.deleteGroup(group);
	}

	// 모임 탈퇴
	@NamedLock
	@Transactional
	public void leaveGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 관계가 없었던 경우 PENDING 상태 + 비관적 락
		GroupUser groupUser = groupUserQueryService.findOrCreateForUpdate(group, user, Status.PENDING);
		// 모임 탈퇴 검증
		groupUserQueryService.checkCanLeave(groupUser);
		// 모임 탈퇴
		groupUserQueryService.leave(groupUser);
	}

	// 모임장 위임
	@Transactional
	public void transferOwnership(
		Long groupId,
		Long userId
	) {
		// 모임장 조회
		User from = userQueryService.findById(getCurrentUserId());
		// 모임원 조회
		User to = userQueryService.findById(userId);
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 모임장
		GroupUser owner = groupUserQueryService.findOrCreateForUpdate(group, from, Status.PENDING);
		owner.checkOwner();
		// 모임원
		GroupUser user = groupUserQueryService.findOrCreateForUpdate(group, to, Status.PENDING);
		user.checkMember();
		// 권한 위임
		groupUserQueryService.transferOwnership(owner, user);
	}

	// 모임원 강퇴
	@Transactional
	public void banMember(
		Long groupId,
		Long userId
	) {
		// 모임장 조회
		User from = userQueryService.findById(getCurrentUserId());
		// 모임원 조회 (강퇴 대상)
		User to = userQueryService.findById(userId);
		// 모임 조회
		Group group = groupQueryService.getById(groupId);
		// 모임장
		GroupUser owner = groupUserQueryService.findOrCreateForUpdate(group, from, Status.PENDING);
		owner.checkOwner();
		// 모임원
		GroupUser user = groupUserQueryService.findOrCreateForUpdate(group, to, Status.PENDING);
		user.checkMember();
		// 모임원 강퇴
		user.ban();
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
		// 모임장
		GroupUser owner = groupUserQueryService.findOrCreateForUpdate(group, user, Status.PENDING);
		owner.checkOwner();
		// 모임 업데이트
		groupQueryService.updateGroup(group, description, capacity, image);
	}

	// 모임 상세 조회
	@Transactional(readOnly = true)
	public GroupDetail readGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		groupQueryService.existsById(groupId);
		// 모인원 확인
		GroupUser member = groupUserQueryService.getById(groupId, user.getId());
		member.checkGroupMember();
		// (모임 + 카테고리 + 로케이션) 조회
		return groupQueryService.getGroupWithDetails(groupId);
	}

	// 내 주변 인기 모임 조회
	@Transactional(readOnly = true)
	public List<PopularGroupSummary> readPopularGroupsNearMe(
		@Nullable Long lastGroupId,
		int size,
		@Nullable Long memberCount
	)
	{
		// 유저 + 로케이션 조회
		User user = userQueryService.findUserWithLocationById(getCurrentUserId());
		Long locationId = user.getLocation().getId();

		return 	groupQueryService.readPopularGroupsNearMe(
			locationId,
			lastGroupId,
			memberCount,
			size
		);
	}

	// 모임 id, 현재 사용자와 모임 관계, 모임 다가오는 일정 개수 조회
	@Transactional(readOnly = true)
	public List<PopularGroupRelation> readGroupsCommonInfo(
		List<Long> groupIds
	)
	{
		return groupQueryService.readPopularGroupRelation(groupIds, getCurrentUserId());
	}

	// 활동이 활발한 모임
	@Transactional(readOnly = true)
	public List<ActiveGroup> readMostActiveGroups(
		@Nullable Long lastGroupId,
		@Nullable Long memberCount,
		int size
	)
	{
		return groupQueryService.readMostActiveGroups(lastGroupId, memberCount, size);
	}

	@Transactional(readOnly = true)
	public List<ActiveGroupDetail> readGroupsDetail(List<Long> groupIds) {
		return groupQueryService.readGroupsDetail(groupIds);
	}

	@Transactional(readOnly = true)
	public List<ActiveGroupRelation> readGroupsRelation(List<Long> groupIds) {
		return groupQueryService.readGroupsRelation(groupIds, getCurrentUserId());
	}

	// 모임장 확인
	@Transactional(readOnly = true)
	public void checkOwner(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		groupQueryService.existsById(groupId);
		// 모임장 확인
		GroupUser owner = groupUserQueryService.getById(groupId, user.getId());
		owner.checkOwner();
	}

	public Long readAnnualScheduleCount(Long groupId, LocalDateTime now) {
		return groupQueryService.readAnnualScheduleCount(groupId, now);
	}

	public Long readMonthlyScheduleCount(Long groupId, LocalDateTime now) {
		return groupQueryService.readMonthlyScheduleCount(groupId, now);
	}

	// 현재 사용자 getPrincipal -> NPE
	private Long getCurrentUserId() {
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}
