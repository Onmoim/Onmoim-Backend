package com.onmoim.server.meeting.service;

import static org.assertj.core.api.Assertions.*;

import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.entity.UserMeeting;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

@SpringBootTest
class MeetingFacadeServiceTest {

	@Autowired
	private MeetingFacadeService meetingFacadeService;

	@Autowired
	private MeetingQueryService meetingQueryService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupUserRepository groupUserRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserMeetingRepository userMeetingRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private final List<Long> createdMeetingIds = new ArrayList<>();

	@AfterEach
	void tearDown() {
		// 동시성 테스트에서 생성된 데이터 정리
		if (!createdMeetingIds.isEmpty()) {
			cleanupTestData(createdMeetingIds);
			createdMeetingIds.clear();
		}
	}

	@Test
	@DisplayName("일정 참석 신청 성공")
	@Transactional
	@Rollback
	void testJoinMeeting_Success_WithFacade() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);

		Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 5);

		setAuthContext(member.getId());

		// when
		meetingFacadeService.joinMeeting(meeting.getId());

		// then
		Meeting updatedMeeting = meetingQueryService.getById(meeting.getId());
		assertThat(updatedMeeting.getJoinCount()).isEqualTo(2); // 생성자 + 신청자

		boolean isJoined = userMeetingRepository.existsByMeetingIdAndUserId(meeting.getId(), member.getId());
		assertThat(isJoined).isTrue();

		clearAuthContext();
	}

	@Test
	@DisplayName("일정 참석 취소 성공")
	@Transactional
	@Rollback
	void testLeaveMeeting_Success_WithFacade() {
		// given
		User owner = createUser("모임장");
		User member1 = createUser("모임원1");
		User member2 = createUser("모임원2");
		Group group = createGroup("테스트모임", owner);
		addMemberToGroup(group, member1, Status.MEMBER);
		addMemberToGroup(group, member2, Status.MEMBER);

		Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 5);

		joinUserToMeeting(meeting, member1);
		joinUserToMeeting(meeting, member2);

		// when - member1이 참석 취소
		setAuthContext(member1.getId());
		meetingFacadeService.leaveMeeting(meeting.getId());

		// then
		Meeting updatedMeeting = meetingQueryService.getById(meeting.getId());
		assertThat(updatedMeeting.getJoinCount()).isEqualTo(2); // owner + member2

		boolean isJoined = userMeetingRepository.existsByMeetingIdAndUserId(meeting.getId(), member1.getId());
		assertThat(isJoined).isFalse();

		clearAuthContext();
	}

	@Test
	@DisplayName("일정 수정 동시성 테스트 - 정원 축소와 참석 취소가 동시에 발생할 때")
	void testConcurrentUpdateAndLeaveMeeting() throws InterruptedException {
		// given
		final Holder<Long> meetingIdHolder = new Holder<>();
		final Holder<User> ownerHolder = new Holder<>();
		final List<User> members = new ArrayList<>();

		transactionTemplate.executeWithoutResult(status -> {
			User owner = createUser("모임장");
			Group group = createGroup("테스트모임_" + UUID.randomUUID().toString().substring(0, 8), owner);
			Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 10);
			createdMeetingIds.add(meeting.getId());

			for (int i = 0; i < 5; i++) {
				User member = createUser("모임원" + i);
				addMemberToGroup(group, member, Status.MEMBER);
				joinUserToMeeting(meeting, member);
				members.add(member);
			}
			meetingRepository.saveAndFlush(meeting);

			ownerHolder.value = owner;
			meetingIdHolder.value = meeting.getId();
		});

		final Long meetingId = meetingIdHolder.value;
		final User owner = ownerHolder.value;

		CountDownLatch latch = new CountDownLatch(2);
		AtomicInteger exceptionCount = new AtomicInteger(0);

		// when
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		executorService.submit(() -> {
			try {
				setAuthContext(owner.getId());
				var request = new MeetingUpdateRequestDto("수정된 제목", LocalDateTime.now().plusDays(1), "수정된 장소", null, 5, 0);
				meetingFacadeService.updateMeeting(meetingId, request);
			} catch (CustomException e) {
				if (e.getErrorCode() == ErrorCode.MEETING_CAPACITY_CANNOT_REDUCE) {
					System.out.println("예상된 예외 발생(정원 축소 실패): " + e.getMessage());
					exceptionCount.incrementAndGet();
				}
			} finally {
				clearAuthContext();
				latch.countDown();
			}
		});

		executorService.submit(() -> {
			try {
				User leavingMember = members.get(0);
				setAuthContext(leavingMember.getId());
				meetingFacadeService.leaveMeeting(meetingId);
			} finally {
				clearAuthContext();
				latch.countDown();
			}
		});

		latch.await();
		executorService.shutdown();

		// then
		Meeting finalMeeting = meetingQueryService.getById(meetingId);

		assertThat(finalMeeting.getJoinCount()).isEqualTo(5);
		assertThat(finalMeeting.getCapacity()).isGreaterThanOrEqualTo(finalMeeting.getJoinCount());

		if (exceptionCount.get() == 1) {
			assertThat(finalMeeting.getCapacity()).isEqualTo(10);
			System.out.println("시나리오 1 통과: 정원 축소 실패 후 참석 취소 성공");
		} else {
			assertThat(finalMeeting.getCapacity()).isEqualTo(5);
			System.out.println("시나리오 2 통과: 참석 취소 성공 후 정원 축소 성공");
		}
	}

	@Test
	@DisplayName("동시성 테스트 - 20명이 3명 정원에 동시 참석 신청")
	void testConcurrentJoinMeeting() throws InterruptedException {
		// given
		final Holder<Long> meetingIdHolder = new Holder<>();
		final List<User> users = new ArrayList<>();

		transactionTemplate.executeWithoutResult(status -> {
			User owner = createUser("모임장");
			Group group = createGroup("테스트모임_" + UUID.randomUUID().toString().substring(0, 8), owner);
			Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 3);
			createdMeetingIds.add(meeting.getId());

			for (int i = 0; i < 20; i++) {
				User member = createUser("참여자" + i);
				addMemberToGroup(group, member, Status.MEMBER);
				users.add(member);
			}
			meetingIdHolder.value = meeting.getId();
		});

		final Long meetingId = meetingIdHolder.value;

		// when
		int taskCount = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		CountDownLatch latch = new CountDownLatch(taskCount);
		AtomicInteger successCount = new AtomicInteger(0);

		for (User user : users) {
			executorService.submit(() -> {
				try {
					setAuthContext(user.getId());
					meetingFacadeService.joinMeeting(meetingId);
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 실패는 예상된 결과
				} finally {
					clearAuthContext();
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		// then
		Meeting finalMeeting = meetingQueryService.getById(meetingId);
		assertThat(finalMeeting.getJoinCount()).isEqualTo(3);
		assertThat(successCount.get()).isEqualTo(2); // 생성자 1명 + 성공 2명
		assertThat(userMeetingRepository.countByMeetingId(meetingId)).isEqualTo(3);
	}

	@Test
	@DisplayName("중복 참석 신청 방지")
	@Transactional
	@Rollback
	void testJoinMeeting_PreventDuplicate_WithFacade() {
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 5);
		setAuthContext(member.getId());
		meetingFacadeService.joinMeeting(meeting.getId());

		assertThatThrownBy(() -> meetingFacadeService.joinMeeting(meeting.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.MEETING_ALREADY_JOINED.getDetail());

		clearAuthContext();
	}

	@Test
	@DisplayName("정원 초과 시 실패")
	@Transactional
	@Rollback
	void testJoinMeeting_CapacityExceeded_WithFacade() {
		User owner = createUser("모임장");
		User member1 = createUser("모임원1");
		User member2 = createUser("모임원2");
		Group group = createGroup("테스트모임", owner);
		addMemberToGroup(group, member1, Status.MEMBER);
		Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 2);
		setAuthContext(member1.getId());
		meetingFacadeService.joinMeeting(meeting.getId());
		clearAuthContext();

		setAuthContext(member2.getId());
		assertThatThrownBy(() -> meetingFacadeService.joinMeeting(meeting.getId()))
			.isInstanceOf(CustomException.class);
		clearAuthContext();
	}

	// Helper Methods

	private void cleanupTestData(List<Long> meetingIds) {
		transactionTemplate.executeWithoutResult(status -> {
			userMeetingRepository.deleteAllByMeetingIdIn(meetingIds);
			meetingRepository.deleteAllById(meetingIds);
		});
	}

	private User createUser(String name) {
		return userRepository.save(User.builder().name(name + "_" + UUID.randomUUID().toString().substring(0, 4)).build());
	}

	private Group createGroup(String name, User owner) {
		Group group = Group.builder()
			.name(name + "_" + UUID.randomUUID().toString().substring(0, 4))
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		return group;
	}

	private void addMemberToGroup(Group group, User user, Status status) {
		groupUserRepository.save(GroupUser.create(group, user, status));
	}

	private Meeting createMeeting(Group group, User creator, MeetingType type, int capacity) {
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(group.getId())
			.type(type)
			.title("테스트일정")
			.startAt(LocalDateTime.now().plusDays(30))
			.placeName("테스트장소")
			.capacity(capacity)
			.cost(0)
			.creatorId(creator.getId())
			.build();
		meetingRepository.save(meeting);
		joinUserToMeeting(meeting, creator);
		return meeting;
	}

	private void joinUserToMeeting(Meeting meeting, User user) {
		userMeetingRepository.save(UserMeeting.create(meeting, user));
		meeting.join();
	}

	private void setAuthContext(Long userId) {
		var detail = new CustomUserDetails(userId, "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}

	private void clearAuthContext() {
		SecurityContextHolder.clearContext();
	}

	private static class Holder<T> {
		T value;
	}
}
