package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryByCategoryResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.dto.response.RecentViewedGroupSummaryResponseDto;
import com.onmoim.server.group.dto.response.cursor.RecentViewCursorPageResponseDto;
import com.onmoim.server.group.entity.*;
import com.onmoim.server.group.implement.GroupLikeQueryService;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupViewLogRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.kakaomap.GeoPointUpdateEvent;
import com.onmoim.server.chat.dto.ChatRoomResponse;
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
	private final GroupRepository groupRepository;
	private final GroupViewLogRepository groupViewLogRepository;

	private final ApplicationEventPublisher eventPublisher;

	private final ChatRoomService chatRoomService;
	private final ChatMessageService chatMessageService;
	private final GroupLikeQueryService groupLikeQueryService;

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
		chatMessageService.sendSystemMessage(room.getGroupId(), "채팅방이 생성되었습니다.");

		GroupUser groupUser = GroupUser.create(group, user, Status.OWNER);
		groupUserQueryService.groupUserSave(groupUser);

		String address = location.getFullAddress();

		eventPublisher.publishEvent(new GeoPointUpdateEvent(group.getId(), address));
		return room;
	}

	// 모임 가입
	@NamedLock
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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

	// 모임 좋아요 또는 좋아요 취소
	@Transactional
	public GroupLikeStatus likeGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());

		// 모임 조회
		Group group = groupQueryService.getById(groupId);

		// 관계가 없었던 경우 NEW 상태
		GroupLike groupLike = groupUserQueryService.findOrCreateLike(group, user);

		// 찜하기 또는 취소
		return groupUserQueryService.likeGroup(groupLike);
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
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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

		// 모임 상세 조회
		return groupQueryService.readGroupDetail(groupId, user.getId());
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
		@Nullable Long meetingCount,
		int size
	)
	{
		return groupQueryService.readMostActiveGroups(lastGroupId, meetingCount, size);
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

	/**
	 * 가입한 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getJoinedGroups(Long cursorId, int size) {
		return groupUserQueryService.getJoinedGroups(cursorId, size);
	}

	/**
	 * 찜한 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getLikedGroups(Long cursorId, int size) {
		return groupLikeQueryService.getLikedGroups(cursorId, size);
	}

	/**
	 * 나와 비슷한 관심사 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getRecommendedGroupsByCategory(Long cursorId, int size) {
		return groupQueryService.getRecommendedGroupsByCategory(cursorId, size);
	}

	/**
	 * 나와 가까운 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getRecommendedGroupsByLocation(Long cursorId, int size) {
		return groupQueryService.getRecommendedGroupsByLocation(cursorId, size);
	}

	/**
	 * 최근 본 모임 로그 쌓기
	 */
	@Transactional
	public void createGroupViewLog(Long groupId) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);
		Group group = groupRepository.findById(groupId)
			.filter(g -> !g.isDeleted())
			.orElseThrow(() -> new CustomException(NOT_EXISTS_GROUP));

		Optional<GroupViewLog> existingLog = groupViewLogRepository.findByUserAndGroup(user, group);

		if (existingLog.isPresent()) {
			existingLog.get().markViewed();
		} else {
			groupViewLogRepository.save(GroupViewLog.create(user, group));
		}
	}

	/**
	 * 최근 본 모임 조회
	 */
	public RecentViewCursorPageResponseDto<RecentViewedGroupSummaryResponseDto> getRecentViewedGroups(LocalDateTime cursorViewedAt, Long cursorId, int size) {
		return groupQueryService.getRecentViewedGroups(cursorViewedAt, cursorId, size);
	}

	/**
	 * 카테고리별 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryByCategoryResponseDto> getGroupsByCategory(Long categoryId, Long cursorId, int size) {
		return groupQueryService.getGroupsByCategory(categoryId, cursorId, size);
	}

	private Long getCurrentUserId() {
		Object principal = SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();

		if ((principal instanceof CustomUserDetails customUserDetails)) {
			return  customUserDetails.getUserId();
		}

		throw new CustomException(UNAUTHORIZED_ACCESS);
	}
}
