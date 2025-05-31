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
import com.onmoim.server.group.dto.request.GroupRequestDto;
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
	public Long createGroup(GroupRequestDto request) {
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

		String address = location.getFullAddress();

		eventPublisher.publishEvent(
			new GeoPointUpdateEvent(group.getId(), location.getId(), address));
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
	public CursorPageResponseDto<GroupMembersResponseDto> getGroupMembers(Long groupId, Long cursorId, int size) {
		groupQueryService.getById(groupId);
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
		groupUserQueryService.checkAndGetOwner(groupId, user.getId());
		// 모임 삭제
		groupQueryService.deleteGroup(group);
	}

	// 모임 탈퇴
	@Retry
	@Transactional
	public void leaveGroup(Long groupId) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		groupQueryService.getById(groupId);
		// 탈퇴 가능 여부 확인
		GroupUser groupUser = groupUserQueryService.checkCanLeave(groupId, user.getId());
		// 모임 탈퇴
		groupUserQueryService.leave(groupUser);
	}

	// 모임장 위임
	@Retry
	@Transactional
	public void transferOwnership(Long groupId, Long userId) {
		// 현재 모임장 조회
		User from = userQueryService.findById(getCurrentUserId());
		// 권한 위임 대상 회원 조회
		User to = userQueryService.findById(userId);
		// 모임 조회
		groupQueryService.getById(groupId);
		// 현재 모임장 확인
		GroupUser owner = groupUserQueryService.checkAndGetOwner(groupId, from.getId());
		// 권한 위임 대상 확인
		GroupUser user = groupUserQueryService.checkAndGetMember(groupId, to.getId());
		// 권한 위임
		groupUserQueryService.transferOwnership(owner, user);
	}

	/**
	 * 모임 수정
	 * 모임 정원 변경의 경우 현재 모임원 수 관련된 동시성 이슈가 발생 예상
	 * -> 네임드 락을 획득하고 시도하도록 하였습니다.
	 * 정원 감소 시: 현재 모임원 수보다 작은 값으로 정원을 설정하려 할 때
	 * 예외 발생(설정 불가)로 처리하여 데이터 정합성을 보장합니다.
	 *
	 * 발생 쿼리
	 * - 유저 조회, 모임 조회(카테고리, 지역), 현재 모임장 조회, 현재 모임원 수 조회
	 * - 업데이트 쿼리, x,y 업데이트 쿼리
	 *
	 */
	@NamedLock
	@Transactional
	public void updateGroup(Long groupId, GroupRequestDto request, MultipartFile image) {
		// 유저 조회
		User user = userQueryService.findById(getCurrentUserId());
		// 모임 조회
		Group group = groupQueryService.getGroupWithRelations(groupId);
		// 현재 모임장
		groupUserQueryService.checkAndGetMember(groupId, user.getId());
		// 업데이트(카테고리, 제목, 설명, 정원)
		groupQueryService.updateGroup(group, request, image);
		// 현재 모임 Location
		Location location = group.getLocation();
		// 현재 모임 Location, 요청 Location 다르면 업데이트 시도
		if(!Objects.equals(location.getId(), request.getLocationId())){
			Location requestLocation = locationQueryService.getById(request.getLocationId());
			String fullAddress = location.getFullAddress();
			eventPublisher.publishEvent(new GeoPointUpdateEvent(groupId, requestLocation.getId(), fullAddress));
		}
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
