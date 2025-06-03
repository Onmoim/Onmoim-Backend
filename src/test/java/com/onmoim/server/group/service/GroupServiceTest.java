package com.onmoim.server.group.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.dto.request.GroupRequestDto;
import com.onmoim.server.group.dto.response.CursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupMembersResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

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

	@Test
	@DisplayName("동시에 가입 요청 테스트")
	@Transactional
	void joinGroupConcurrencyTest() throws InterruptedException {
		// given
		assertThat(TestTransaction.isActive()).isTrue();

		List<User> userList = new ArrayList<>();
		Group group = Group.groupCreateBuilder()
			.name("group")
			.capacity(10)
			.build();

		groupRepository.save(group);
		final Long groupId = group.getId();
		User owner = User.builder().name("모임장").build();
		userList.add(owner);
		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		userRepository.save(owner);
		IntStream.range(0, 20).forEach(i -> {
			User user = User.builder()
				.name("test" + i)
				.build();
			userList.add(user);
			userRepository.save(user);
		});
		TestTransaction.flagForCommit(); // 트랜잭션 커밋
		TestTransaction.end();           // 트랜잭션 종료

		// when
		int taskCount = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(taskCount);
		for (int i = 0; i < taskCount; i++) {
			final int idx = i;
			executorService.submit(() -> {
				try {
					// 각 쓰레드마다 인증 정보 세팅
					Long userId = userList.get(idx).getId();
					var detail = new CustomUserDetails(userId, "test", "test");
					var authenticated = UsernamePasswordAuthenticationToken.authenticated(
						detail, null, null);
					SecurityContextHolder.getContext().setAuthentication(authenticated);

					// 동시성 테스트
					groupService.joinGroup(groupId);
				} finally {
					// 쓰레드 인증 정보 삭제
					SecurityContextHolder.clearContext();
					latch.countDown();
				}
			});
		}
		latch.await();
		executorService.shutdown();

		// then
		Group updatedGroup = groupRepository.findById(groupId).orElseThrow();
		Long count = groupUserRepository.countByGroupAndStatuses(groupId, List.of(Status.MEMBER, Status.OWNER));
		System.out.println("최종 모임 인원 by GroupUser = " + count);
		assertThat(count).isLessThanOrEqualTo(updatedGroup.getCapacity());
		assertThat(count).isEqualTo(updatedGroup.getCapacity());

		groupUserRepository.deleteAll();
		userRepository.deleteAll();
		groupRepository.deleteAll();
	}

	@Test
	@DisplayName("모임 좋아요: 성공")
	@Transactional
	void likeGroupSuccessTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.groupCreateBuilder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.BOOKMARK));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 성공: PENDING -> BOOKMARK ")
	@Transactional
	void likeGroupSuccessTest2() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.groupCreateBuilder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.PENDING));

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.BOOKMARK));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 실패: 이미 가입")
	@Transactional
	void likeGroupFailureTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.groupCreateBuilder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.MEMBER));

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		assertThatThrownBy(() -> groupService.likeGroup(group.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.GROUP_ALREADY_JOINED.getDetail());

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 취소 성공: BOOKMARK -> PENDING")
	@Transactional
	void likeCancelGroupSuccessTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.groupCreateBuilder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.BOOKMARK));

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.PENDING));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 회원 조회")
	@Transactional
	void selectGroupMembers() {
		// given
		Group group = Group.groupCreateBuilder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);

		// 유저: 40  모임장: 1 모임원: 19
		for (int i = 0; i < 40; i++) {
			User user = User.builder()
				.name("test" + i)
				.profileImgUrl("img_url" + i)
				.build();
			userRepository.save(user);
			if (i >= 20) continue;
			if (i == 0) {
				groupUserRepository.save(GroupUser.create(group, user, Status.OWNER));
				continue;
			}
			groupUserRepository.save(GroupUser.create(group, user, Status.MEMBER));
		}

		// expected

		// 1 ~ 10
		CursorPageResponseDto<GroupMembersResponseDto> result1 =
			groupService.getGroupMembers(group.getId(), null, 10);

		List<GroupMembersResponseDto> content1 = result1.getContent();
		assertThat(result1.getTotalCount()).isEqualTo(20);
		assertThat(result1.isHasNext()).isTrue();
		assertThat(content1.size()).isEqualTo(10);
		System.out.println(content1);
		Long cursorId1 = result1.getCursorId();

		// 11 ~ 20
		CursorPageResponseDto<GroupMembersResponseDto> result2 =
			groupService.getGroupMembers(group.getId(), cursorId1, 10);

		List<GroupMembersResponseDto> content2 = result2.getContent();
		assertThat(result2.isHasNext()).isFalse();
		assertThat(result2.getCursorId()).isNotNull();
		assertThat(content2.size()).isEqualTo(10);
		System.out.println(content2);

		CursorPageResponseDto<GroupMembersResponseDto> result3 =
			groupService.getGroupMembers(group.getId(), 50L, 10);

		List<GroupMembersResponseDto> content3 = result3.getContent();
		assertThat(result3.isHasNext()).isFalse();
		assertThat(result3.getCursorId()).isNull();
		assertThat(content3).isEmpty();
	}

	@Test
	@DisplayName("모임 생성 성공 테스트")
	void createGroupSuccess() throws Exception {
		// given
		Location location = Location.create("1234", "서울특별시", "종로구", "청운동", null);
		locationRepository.save(location);

		Category category = Category.create("음악", null);
		categoryRepository.save(category);

		User user = User.builder().name("test").build();
		userRepository.save(user);

		String name = "테스트 모임";
		String description = "모임 설명";
		GroupRequestDto request = GroupRequestDto.builder()
			.name(name)
			.description(description)
			.locationId(location.getId())
			.categoryId(category.getId())
			.capacity(100)
			.build();

		// when
		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
		Long groupId = groupService.createGroup(request);

		// then
		Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			Optional<Group> findGroup = groupRepository.findGroupWithDetails(groupId);
			assertThat(findGroup.isPresent()).isTrue();
			Group group = findGroup.get();
			assertThat(group.getName()).isEqualTo(name);
			assertThat(group.getDescription()).isEqualTo(description);
			assertThat(group.getGeoPoint()).isNotNull();
			assertThat(group.getGeoPoint().getX()).isGreaterThan(0.0);
			assertThat(group.getGeoPoint().getY()).isGreaterThan(0.0);
			System.out.println("geoPoint= " + group.getGeoPoint());
		});

		groupUserRepository.deleteAll(); // group, user 참조
		userRepository.deleteAll();
		groupRepository.deleteAll(); // location, category 참조
		locationRepository.deleteAll();
		categoryRepository.deleteAll();
	}

}
