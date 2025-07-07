package com.onmoim.server.group.service;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.chat.dto.ChatRoomResponse;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.dto.GroupMember;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.GroupUserId;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.group.implement.GroupUserQueryService;
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
	@DisplayName("모임 좋아요: 성공")
	void likeGroupSuccessTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);

		setSecurityContext(user.getId());

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.BOOKMARK));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 성공: PENDING -> BOOKMARK ")
	void likeGroupSuccessTest2() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.PENDING));

		setSecurityContext(user.getId());

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.BOOKMARK));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 실패: 이미 가입")
	void likeGroupFailureTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.MEMBER));

		setSecurityContext(user.getId());

		assertThatThrownBy(() -> groupService.likeGroup(group.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_ALREADY_JOINED.getDetail());

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 취소 성공: BOOKMARK -> PENDING")
	void likeCancelGroupSuccessTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.BOOKMARK));

		setSecurityContext(user.getId());

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.PENDING));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
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
	@Disabled
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
	@Disabled
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
	@Disabled
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

		final Long groupId = group.getId();
		setSecurityContext(owner.getId());

		// expected
		assertThatThrownBy(() -> groupService.leaveGroup(groupId))
			.isInstanceOf(CustomException.class)
			.hasMessage(GROUP_OWNER_TRANSFER_REQUIRED.getDetail());
	}

	@Test
	@Disabled
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
	@Disabled
	@DisplayName("내 주변 인기 모임 조회 - 회원 수 내림차순 정렬 및 다양한 location, group, groupUser 정보")
	void readPopularGroupsNearMe() {
		// given
		Location location1 = Location.create("1111", "서울특별시", "종로구", "청운동", null);
		Location location2 = Location.create("1112", "서울특별시", "강남구", "역삼동", null);
		Location location3 = Location.create("1113", "부산광역시", "해운대구", "우동", null);
		locationRepository.save(location1);
		locationRepository.save(location2);
		locationRepository.save(location3);

		User user = User.builder().name("test").location(location1).build();
		userRepository.save(user);

		setSecurityContext(user.getId());

		// location1에 6개, location2에 2개, location3에 1개 모임 생성
		List<Group> groups = new ArrayList<>();
		groups.add(Group.builder().name("종로 인기 모임 1").description("설명1").capacity(100).location(location1).build());
		groups.add(Group.builder().name("종로 인기 모임 2").description("설명2").capacity(100).location(location1).build());
		groups.add(Group.builder().name("종로 인기 모임 3").description("설명3").capacity(100).location(location1).build());
		groups.add(Group.builder().name("강남 인기 모임 1").description("설명4").capacity(100).location(location2).build());
		groups.add(Group.builder().name("강남 인기 모임 2").description("설명5").capacity(100).location(location2).build());
		groups.add(Group.builder().name("해운대 인기 모임 1").description("설명6").capacity(100).location(location3).build());
		groups.add(Group.builder().name("종로 인기 모임 4").description("설명7").capacity(100).location(location1).build());
		groups.add(Group.builder().name("종로 인기 모임 5").description("설명8").capacity(100).location(location1).build());
		groups.add(Group.builder().name("종로 인기 모임 6").description("설명9").capacity(100).location(location1).build());
		groupRepository.saveAll(groups);


		for (int i = 0; i < groups.size(); i++) {
			Group group = groups.get(i);
			// 각 모임 모임장 저장
			User owner = User.builder().name("owner" + i).location(location1).build();
			userRepository.save(owner);
			groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
			if(!group.getName().contains("종로")) continue;
			// 종로 모임에만 회원 추가
			int memberCount = 3 + i;
			for (int j = 0; j < memberCount; j++) {
				User member = User.builder().name("member" + i + j).location(location1).build();
				userRepository.save(member);
				groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));
			}
		}



	}
}
