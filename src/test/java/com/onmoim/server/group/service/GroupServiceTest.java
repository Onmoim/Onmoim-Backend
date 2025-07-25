package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static com.onmoim.server.group.entity.GroupLikeStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.onmoim.server.group.entity.*;
import com.onmoim.server.group.repository.GroupViewLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.chat.dto.ChatRoomResponse;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.dto.GroupMember;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.group.implement.GroupUserQueryService;
import com.onmoim.server.group.repository.GroupLikeRepository;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

@Transactional
@SpringBootTest
class GroupServiceTest {
	@Autowired
	private GroupService groupService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GroupRepository groupRepository;
	@Autowired
	private GroupUserRepository groupUserRepository;
	@Autowired
	private LocationRepository locationRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private GroupUserQueryService groupUserQueryService;
	@Autowired
	private GroupQueryService groupQueryService;
	@Autowired
	private GroupLikeRepository groupLikeRepository;
	@Autowired
	private GroupViewLogRepository groupViewLogRepository;

	@AfterEach
	void tearDown() {
		groupUserRepository.deleteAll();
		groupRepository.deleteAll();
		userRepository.deleteAll();
		locationRepository.deleteAll();
		categoryRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	private void setSecurityContext(Long userId) {
		var detail = new CustomUserDetails(
			userId,
			null,
			null
		);
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
				detail,
				null,
				null
		);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}

	@Test
	@DisplayName("모임 좋아요: 신규 상태 -> 좋아요")
	void likeGroupTest1() {
		// given
		User user = User.builder().name("mock user").build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("mock group")
			.description("mock description")
			.capacity(10)
			.build();
		groupRepository.save(group);

		setSecurityContext(user.getId());

		// when
		GroupLikeStatus status = groupService.likeGroup(group.getId());

		// then
		assertThat(status).isEqualTo(LIKE);
		Optional<GroupLike> result = groupLikeRepository.findById(new GroupUserId(group.getId(), user.getId()));
		assertThat(result.isPresent()).isTrue();
		assertThat(result.get().getStatus()).isEqualTo(LIKE);
	}

	@Test
	@DisplayName("모임 좋아요: 좋아요 상태 -> 취소(PENDING) 상태")
	void likeGroupTest2() {
		// given
		User user = User.builder().name("mock user").build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("mock group")
			.description("mock description")
			.capacity(10)
			.build();
		groupRepository.save(group);

		GroupLike groupLike = GroupLike.create(group, user, LIKE);
		groupLikeRepository.save(groupLike);

		setSecurityContext(user.getId());

		// when
		GroupLikeStatus status = groupService.likeGroup(group.getId());

		// then
		assertThat(status).isEqualTo(PENDING);
		Optional<GroupLike> result = groupLikeRepository.findById(new GroupUserId(group.getId(), user.getId()));
		assertThat(result.isPresent()).isTrue();
		assertThat(result.get().getStatus()).isEqualTo(PENDING);
	}

	@Test
	@DisplayName("모임 좋아요: 취소(PENDING) 상태 -> 좋아요(LIKE) 상태")
	void likeGroupTest3() {
		// given
		User user = User.builder().name("mock user").build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("mock group")
			.description("mock description")
			.capacity(10)
			.build();
		groupRepository.save(group);

		GroupLike groupLike = GroupLike.create(group, user, PENDING);
		groupLikeRepository.save(groupLike);

		setSecurityContext(user.getId());

		// when
		GroupLikeStatus status = groupService.likeGroup(group.getId());

		// then
		assertThat(status).isEqualTo(LIKE);
		Optional<GroupLike> result = groupLikeRepository.findById(new GroupUserId(group.getId(), user.getId()));
		assertThat(result.isPresent()).isTrue();
		assertThat(result.get().getStatus()).isEqualTo(LIKE);
	}

	@Test
	@DisplayName("모임 회원 조회")
	void selectGroupMembers() {
		// given
		User owner = User.builder()
			.name("owner")
			.build();
		userRepository.save(owner);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		for(int i = 0 ; i < 50; i++) { // owner + member = 26
			User member = User.builder()
				.name("member " + i)
				.build();
			userRepository.save(member);
			if (i % 2 == 0) {
				groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));
			}
		}

		setSecurityContext(owner.getId());

		// expected
		List<GroupMember> groupMembers1 = groupService.readGroupMembers(group.getId(), null, 10);
		assertThat(groupMembers1.size()).isEqualTo(11);
		// owner 0 2 4 6 8 10 12 14 16 18
		System.out.println(groupMembers1);
		GroupMember last1 = groupMembers1.get(9);
		assertThat(groupMembers1.getFirst().username()).isEqualTo("owner");
		assertThat(groupMembers1.getLast().username()).isEqualTo("member 18");

		// 18 20 22 24 26 28 30 32 34 36 38
		List<GroupMember> groupMembers2 = groupService.readGroupMembers(group.getId(), last1.memberId(), 10);
		assertThat(groupMembers2.size()).isEqualTo(11);
		System.out.println(groupMembers2);
		GroupMember last2 = groupMembers2.get(9);
		assertThat(groupMembers2.getFirst().username()).isEqualTo("member 18");
		assertThat(groupMembers2.getLast().username()).isEqualTo("member 38");

		List<GroupMember> groupMembers3 = groupService.readGroupMembers(group.getId(), last2.memberId(), 10);
		assertThat(groupMembers3.size()).isEqualTo(6);
		// 38 40 42 44 46 48
		System.out.println(groupMembers3);
		assertThat(groupMembers3.getFirst().username()).isEqualTo("member 38");
		assertThat(groupMembers3.getLast().username()).isEqualTo("member 48");
	}

	@Test
	@DisplayName("모임 생성 성공")
	void createGroupSuccess() {
		// given
		Location location = Location.create("1234", "서울특별시", "종로구", "청운동", null);
		locationRepository.save(location);

		Category category = Category.create("음악", null);
		categoryRepository.save(category);

		User user = User.builder().name("test").build();
		userRepository.save(user);

		String name = "테스트 모임";
		String description = "모임 설명";
		int capacity = 100;

		setSecurityContext(user.getId());

		// when
		ChatRoomResponse response = groupService.createGroup(
			category.getId(),
			location.getId(),
			name,
			description,
			capacity
		);
		Long groupId = response.getGroupId();

		// then
		Group group = groupRepository.getById(groupId);
		assertThat(group.getName()).isEqualTo(name);
		assertThat(group.getDescription()).isEqualTo(description);
		assertThat(group).isNotNull();
		GroupUser groupUser = groupUserRepository.findGroupUser(groupId, user.getId()).get();
		assertThat(groupUser).isNotNull();
		assertThat(groupUser.getStatus()).isEqualTo(Status.OWNER);
	}

	@Test
	@DisplayName("모임 수정 성공")
	void updateGroupSuccess() {
		// given
		User user = User.builder().name("owner").build();
		userRepository.save(user);

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(100)
			.build());

		groupUserRepository.save(GroupUser.create(group, user, Status.OWNER));

		TestTransaction.flagForCommit();
		TestTransaction.end();

		final Long groupId = group.getId();
		setSecurityContext(user.getId());

		// when
		groupService.updateGroup(groupId, "after", 20, null);

		// then
		Group updated = groupQueryService.getById(groupId);
		assertThat(updated.getDescription()).isEqualTo("after");
		assertThat(updated.getCapacity()).isEqualTo(20);
	}

	@Test
	@DisplayName("모임 수정 실패: 모임장 X")
	void updateGroupFailure1() {
		// given
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(100)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		TestTransaction.flagForCommit();
		TestTransaction.end();

		final Long groupId = group.getId();
		setSecurityContext(member.getId());

		// expected
		assertThatThrownBy(() -> groupService.updateGroup(groupId, "after", 20, null))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("모임 수정 실패: 정원 설정 문제")
	void updateGroupFailure2() {
		// given
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		TestTransaction.flagForCommit();
		TestTransaction.end();

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// expected
		assertThatThrownBy(() -> groupService.updateGroup(groupId, "after", 1, null))
			.isInstanceOf(CustomException.class)
			.hasMessage(CAPACITY_MUST_BE_GREATER_THAN_CURRENT.getDetail());
	}

	@Test
	@DisplayName("모임원 강퇴 성공")
	void banSuccess() {
		// given
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		final Long groupId = group.getId();
		final Long memberId = member.getId();
		setSecurityContext(owner.getId());

		// when
		groupService.banMember(groupId, memberId);

		// then
		Optional<GroupUser> banMember = groupUserRepository.findGroupUser(groupId, memberId);
		assertThat(banMember.isPresent()).isTrue();
		assertThat(banMember.get().getStatus()).isEqualTo(Status.BAN);
	}

	@Test
	@DisplayName("모임원 강퇴 실패: 모임장 X")
	void banFailure1() {
		// given
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		final Long groupId = group.getId();
		setSecurityContext(member.getId());

		// expected
		assertThatThrownBy(() -> groupService.banMember(groupId, owner.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("모임원 강퇴 실패: 모임장 자신 강퇴 시도")
	void banFailure2() {
		// given
		User owner = User.builder().name("owner").build();
		userRepository.save(owner);

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// expected
		assertThatThrownBy(() -> groupService.banMember(groupId, owner.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("모임 삭제 성공")
	void deleteGroupSuccess() {
		// given
		User owner = User.builder().name("owner").build();
		userRepository.save(owner);

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// when
		groupService.deleteGroup(groupId);

		// then
		Optional<Group> deletedGroup = groupRepository.findById(groupId);
		assertThat(deletedGroup.isPresent()).isTrue();
		assertThat(deletedGroup.get().isDeleted()).isTrue();
	}

	@Test
	@DisplayName("모임 삭제 실패: 모임장 X")
	void deleteGroupFailure1() {
		// given
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		final Long groupId = group.getId();
		setSecurityContext(member.getId());

		// expected
		assertThatThrownBy(() -> groupService.deleteGroup(groupId))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("모임 삭제 실패: 모임 멤버 X")
	void deleteGroupFailure2() {
		// given
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		final Long groupId = group.getId();
		setSecurityContext(member.getId());

		// expected
		assertThatThrownBy(() -> groupService.deleteGroup(groupId))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("모임 탈퇴 성공: 모임원 (MEMBER -> PENDING)")
	void leaveGroupSuccess1() {
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		TestTransaction.flagForCommit();
		TestTransaction.end();

		final Long groupId = group.getId();
		setSecurityContext(member.getId());

		// when
		groupService.leaveGroup(groupId);

		// then
		assertThat(groupUserQueryService.countMembers(groupId)).isEqualTo(1L);
		Optional<GroupUser> optionalGroupUser = groupUserRepository.findById(new GroupUserId(groupId, member.getId()));
		assertThat(optionalGroupUser.isPresent()).isTrue();
		assertThat(optionalGroupUser.get().getStatus()).isEqualTo(Status.PENDING);
	}

	@Test
	@DisplayName("모임 탈퇴 성공 + 모임 삭제: 모임장 + 모임원 = 1명")
	void leaveGroupSuccess2() {
		User owner = User.builder().name("owner").build();
		userRepository.save(owner);

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		TestTransaction.flagForCommit();
		TestTransaction.end();

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// when
		groupService.leaveGroup(groupId);

		// then
		Optional<Group> optionalGroup = groupRepository.findById(groupId);
		assertThat(optionalGroup.isPresent()).isTrue();
		assertThat(optionalGroup.get().isDeleted()).isTrue();
	}

	@Test
	@DisplayName("모임 탈퇴 실패: 모임장 + 모임원 = 2명")
	void leaveGroupFailure1() {
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		TestTransaction.flagForCommit();
		TestTransaction.end();

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// expected
		assertThatThrownBy(() -> groupService.leaveGroup(groupId))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_OWNER_TRANSFER_REQUIRED.getDetail());
	}

	@Test
	@DisplayName("모임 탈퇴 실패: 모임원 아닌 사용자 탈퇴 시도")
	void leaveGroupFailure2() {
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		TestTransaction.flagForCommit();
		TestTransaction.end();

		final Long groupId = group.getId();
		setSecurityContext(member.getId());

		// expected
		assertThatThrownBy(() -> groupService.leaveGroup(groupId))
			.isInstanceOf(CustomException.class)
			.hasMessage(NOT_GROUP_MEMBER.getDetail());
	}

	@Test
	@DisplayName("모임장 권한 위임 성공")
	void transferOwnerSuccess() {
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// when
		groupService.transferOwnership(groupId, member.getId());

		// then
		Optional<GroupUser> member_to_owner =
			groupUserRepository.findById(new GroupUserId(groupId, member.getId()));

		assertThat(member_to_owner.isPresent()).isTrue();
		assertThat(member_to_owner.get().getStatus()).isEqualTo(Status.OWNER);

		Optional<GroupUser> owner_to_member =
			groupUserRepository.findById(new GroupUserId(groupId, owner.getId()));

		assertThat(owner_to_member.isPresent()).isTrue();
		assertThat(owner_to_member.get().getStatus()).isEqualTo(Status.MEMBER);
	}

	@Test
	@DisplayName("모임장 권한 위임 실패: 모임장 X")
	void transferOwnerFailure1() {
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));

		final Long groupId = group.getId();
		setSecurityContext(member.getId());

		// expected
		assertThatThrownBy(() -> groupService.transferOwnership(groupId, member.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("모임장 권한 위임 실패: 모임에 없는 사용자")
	void transferOwnerFailure2() {
		User owner = User.builder().name("owner").build();
		User member = User.builder().name("member").build();
		userRepository.saveAll(List.of(owner, member));

		Group group = groupRepository.save(Group.builder()
			.name("테스트 모임")
			.description("before")
			.capacity(20)
			.build());

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// expected
		assertThatThrownBy(() -> groupService.transferOwnership(groupId, member.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("모임 조회 로그 생성(처음)")
	void createNewGroupViewLog() {
		// given
		// 1. 유저 생성
		Location location = locationRepository.save(Location.create("100000", "서울특별시", "강남구", "역삼동", null));
		Category category = categoryRepository.save(Category.create("운동/스포츠", null));

		User user = userRepository.save(User.builder()
			.oauthId("1234567890")
			.provider("google")
			.email("test@test.com")
			.name("홍길동")
			.gender("F")
			.birth(LocalDateTime.now())
			.location(location)
			.profileImgUrl("https://cdn.example.com/profile/test.jpg")
			.build()
		);

		// 인증 정보 설정
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		CustomUserDetails userDetails = new CustomUserDetails(user.getId());
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		Group group = groupRepository.save(Group.builder()
			.name("테스트")
			.location(location)
			.category(category)
			.build());

		// when
		groupService.createGroupViewLog(group.getId());

		// then
		List<GroupViewLog> groupViewLogList = groupViewLogRepository.findAll();
		assertEquals(1, groupViewLogList.size());
		assertEquals(user.getId(), groupViewLogList.get(0).getUser().getId());
		assertEquals(group.getId(), groupViewLogList.get(0).getGroup().getId());
	}

	@Test
	@DisplayName("모임 조회 로그 생성(기존 로그 있는 경우)")
	void createExistingGroupViewLog() {
		// given
		// 1. 유저 생성
		Location location = locationRepository.save(Location.create("100000", "서울특별시", "강남구", "역삼동", null));
		Category category = categoryRepository.save(Category.create("운동/스포츠", null));

		User user = userRepository.save(User.builder()
			.oauthId("1234567890")
			.provider("google")
			.email("test@test.com")
			.name("홍길동")
			.gender("F")
			.birth(LocalDateTime.now())
			.location(location)
			.profileImgUrl("https://cdn.example.com/profile/test.jpg")
			.build()
		);

		// 인증 정보 설정
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		CustomUserDetails userDetails = new CustomUserDetails(user.getId());
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		Group group = groupRepository.save(Group.builder()
			.name("테스트")
			.location(location)
			.category(category)
			.build());

		GroupViewLog log = groupViewLogRepository.save(GroupViewLog.create(user, group));

		Long originalViewCount = log.getViewCount();
		LocalDateTime originalViewedAt = log.getModifiedDate();

		// when
		groupService.createGroupViewLog(group.getId());

		// then
		GroupViewLog updatedLog = groupViewLogRepository.findByUserAndGroup(user, group).get();
		assertTrue(updatedLog.getModifiedDate().isAfter(originalViewedAt));
		assertEquals(originalViewCount + 1, updatedLog.getViewCount());
	}

}
