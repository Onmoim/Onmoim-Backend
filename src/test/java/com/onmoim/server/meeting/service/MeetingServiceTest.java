package com.onmoim.server.meeting.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;


import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequestDto;
import com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
class MeetingServiceTest {

	@Autowired
	private MeetingService meetingService;
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
	private EntityManagerFactory emf;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private MeetingFacadeService meetingFacadeService;

	@Test
	@DisplayName("01. 정기모임 일정 생성 성공 - 모임장 권한")
	@Transactional
	@Rollback
	void test01_createRegularMeeting_Success_OwnerPermission() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);

		MeetingCreateRequestDto request = MeetingCreateRequestDto.builder()
			.type(MeetingType.REGULAR)
			.title("정기모임일정_" + uniqueId)
			.startAt(LocalDateTime.now().plusDays(30))
			.placeName("강남역")
			.geoPoint(new GeoPoint(37.498, 127.027))
			.capacity(10)
			.cost(5000)
			.build();

		setAuthContext(owner.getId());

		// when
		Long meetingId = meetingService.createMeeting(group.getId(), request, null);

		// then
		Meeting meeting = meetingQueryService.getById(meetingId);
		assertThat(meeting.getTitle()).isEqualTo("정기모임일정_" + uniqueId);
		assertThat(meeting.getType()).isEqualTo(MeetingType.REGULAR);
		assertThat(meeting.getJoinCount()).isEqualTo(1); // 생성자 자동 참석
		assertThat(meeting.getCreator().getId()).isEqualTo(owner.getId());

		// 생성자 자동 참석 확인
		boolean isJoined = userMeetingRepository.existsByMeetingIdAndUserId(meetingId, owner.getId());
		assertThat(isJoined).isTrue();

		clearAuthContext();
	}

	@Test
	@DisplayName("02. 번개모임 일정 생성 성공 - 모임원 권한")
	@Transactional
	@Rollback
	void test02_createFlashMeeting_Success_MemberPermission() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		User member = createUser("모임원_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);
		addMemberToGroup(group, member, Status.MEMBER);

		MeetingCreateRequestDto request = MeetingCreateRequestDto.builder()
			.type(MeetingType.FLASH)
			.title("번개모임일정_" + uniqueId)
			.startAt(LocalDateTime.now().plusDays(30))
			.placeName("홍대입구역")
			.geoPoint(new GeoPoint(37.557, 126.924))
			.capacity(5)
			.cost(10000)
			.build();

		setAuthContext(member.getId());

		// when
		Long meetingId = meetingService.createMeeting(group.getId(), request, null);

		// then
		Meeting meeting = meetingQueryService.getById(meetingId);
		assertThat(meeting.getTitle()).isEqualTo("번개모임일정_" + uniqueId);
		assertThat(meeting.getType()).isEqualTo(MeetingType.FLASH);
		assertThat(meeting.getCreator().getId()).isEqualTo(member.getId());

		clearAuthContext();
	}

	@Test
	@DisplayName("03. 일정 생성 실패 - 권한 없음")
	@Transactional
	@Rollback
	void test03_createMeeting_Fail_NoPermission() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		User outsider = createUser("외부인_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);

		MeetingCreateRequestDto request = MeetingCreateRequestDto.builder()
			.type(MeetingType.REGULAR)
			.title("권한없는일정_" + uniqueId)
			.startAt(LocalDateTime.now().plusDays(30)) // 30일 후로 통일
			.placeName("강남역")
			.capacity(10)
			.cost(0)
			.build();

		setAuthContext(outsider.getId());

		// when & then
		assertThatThrownBy(() -> meetingService.createMeeting(group.getId(), request, null))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.NOT_GROUP_MEMBER.getDetail());

		clearAuthContext();
	}

	@Test
	@DisplayName("04. 일정 참석 신청 성공")
	@Transactional
	@Rollback
	void test04_joinMeeting_Success() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		User member = createUser("모임원_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);
		addMemberToGroup(group, member, Status.MEMBER);

		Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 5, uniqueId);

		setAuthContext(member.getId());

		// when
		meetingService.joinMeeting(meeting.getId());

		// then
		Meeting updatedMeeting = meetingQueryService.getById(meeting.getId());
		assertThat(updatedMeeting.getJoinCount()).isEqualTo(2); // 생성자 + 신청자

		boolean isJoined = userMeetingRepository.existsByMeetingIdAndUserId(meeting.getId(), member.getId());
		assertThat(isJoined).isTrue();

		clearAuthContext();
	}

	@Test
	@DisplayName("05. 일정 참석 취소 성공")
	@Transactional
	@Rollback
	void test05_leaveMeeting_Success() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		User member1 = createUser("모임원1_" + uniqueId);
		User member2 = createUser("모임원2_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);
		addMemberToGroup(group, member1, Status.MEMBER);
		addMemberToGroup(group, member2, Status.MEMBER);

		Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 5, uniqueId);
		// 3명이 참석: owner + member1 + member2
		joinUserToMeeting(meeting, member1);
		joinUserToMeeting(meeting, member2);

		setAuthContext(member1.getId());

		// when - member1이 나가도 2명(owner + member2) 남음
		meetingService.leaveMeeting(meeting.getId());

		// then
		Meeting updatedMeeting = meetingQueryService.getById(meeting.getId());
		assertThat(updatedMeeting.getJoinCount()).isEqualTo(2); // owner + member2 남음

		boolean isJoined = userMeetingRepository.existsByMeetingIdAndUserId(meeting.getId(), member1.getId());
		assertThat(isJoined).isFalse();

		clearAuthContext();
	}

	@Test
	@DisplayName("06. 일정 참석 취소 - 마지막 참석자가 떠나고, 일정이 시작되어 자동 삭제되는 경우")
	@Transactional
	@Rollback
	void test06_leaveMeeting_AutoDelete() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User lastUser = createUser("마지막참석자_" + uniqueId);
		Group group = createGroup("자동삭제테스트모임_" + uniqueId, lastUser);

		// 1단계: 미래 시간으로 모임 생성 (정상적인 참여가 가능하도록)
		Meeting meetingToDelete = Meeting.meetingCreateBuilder()
			.group(group)
			.type(MeetingType.FLASH)
			.title("자동 삭제될 모임")
			.startAt(LocalDateTime.now().plusDays(1)) // 미래 시간으로 설정
			.placeName("사라질 장소")
			.capacity(5)
			.cost(0)
			.creator(lastUser)
			.build();
		meetingRepository.save(meetingToDelete);

		// 2단계: 사용자가 모임에 참여 (미래 모임이므로 정상적으로 참여 가능)
		joinUserToMeeting(meetingToDelete, lastUser);
		assertThat(meetingRepository.findById(meetingToDelete.getId()).get().getJoinCount()).isEqualTo(1);

		// 3단계: 모임 시작 시간을 과거로 변경 (이미 시작된 상태로 만들기)
		try {
			java.lang.reflect.Field startAtField = Meeting.class.getDeclaredField("startAt");
			startAtField.setAccessible(true);
			startAtField.set(meetingToDelete, LocalDateTime.now().minusHours(1));
			meetingRepository.save(meetingToDelete);
		} catch (Exception e) {
			throw new RuntimeException("테스트 설정 실패", e);
		}

		// when
		// 마지막 참석자가 모임을 떠남 (이제 isStarted() == true 이므로 자동 삭제 조건 충족)
		setAuthContext(lastUser.getId());
		meetingService.leaveMeeting(meetingToDelete.getId());

		// then
		Meeting foundMeeting = meetingRepository.findById(meetingToDelete.getId()).orElseThrow(
			() -> new AssertionError("삭제된 모임을 찾을 수 없습니다.")
		);
		assertThat(foundMeeting.getDeletedDate()).isNotNull();
		clearAuthContext();
	}

	@Test
	@DisplayName("07. 일정 수정 성공 - 모임장 권한")
	@Transactional
	@Rollback
	void test07_updateMeeting_Success() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);
		Meeting meeting = createMeeting(group, owner, MeetingType.REGULAR, 10, uniqueId);

		MeetingUpdateRequestDto request = MeetingUpdateRequestDto.builder()
			.title("수정된제목_" + uniqueId)
			.startAt(LocalDateTime.now().plusDays(30)) // 30일 후로 통일
			.placeName("신촌역")
			.geoPoint(new GeoPoint(37.555, 126.936))
			.capacity(15)
			.cost(3000)
			.build();

		setAuthContext(owner.getId());

		// when
		meetingService.updateMeeting(meeting.getId(), request);

		// then
		Meeting updatedMeeting = meetingQueryService.getById(meeting.getId());
		assertThat(updatedMeeting.getTitle()).isEqualTo("수정된제목_" + uniqueId);
		assertThat(updatedMeeting.getCapacity()).isEqualTo(15);
		assertThat(updatedMeeting.getCost()).isEqualTo(3000);

		clearAuthContext();
	}

	@Test
	@DisplayName("08. 일정 삭제 성공 - 모임장 권한")
	@Transactional
	@Rollback
	void test08_deleteMeeting_Success() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		User member = createUser("모임원_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);
		addMemberToGroup(group, member, Status.MEMBER);

		Meeting meeting = createMeeting(group, owner, MeetingType.REGULAR, 10, uniqueId);
		joinUserToMeeting(meeting, member);

		setAuthContext(owner.getId());

		// when
		meetingService.deleteMeeting(meeting.getId());

		// then
		Meeting deletedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
		assertThat(deletedMeeting.getDeletedDate()).isNotNull();

		// 관련 UserMeeting 데이터도 삭제되었는지 확인
		long userMeetingCount = userMeetingRepository.countByMeetingId(meeting.getId());
		assertThat(userMeetingCount).isEqualTo(0);

		clearAuthContext();
	}

	@Test
	@DisplayName("일정 수정 동시성 테스트 - 정원 축소와 참석 취소가 동시에 발생할 때")
	void testConcurrentUpdateAndLeaveMeeting() throws InterruptedException {
		// given
		final List<User> members = new ArrayList<>();
		final Holder<User> ownerHolder = new Holder<>();
		final Holder<Long> meetingIdHolder = new Holder<>();

		transactionTemplate.executeWithoutResult(status -> {
			User owner = createUser("모임장_" + UUID.randomUUID().toString().substring(0, 8));
			Group group = createGroup("테스트모임_" + UUID.randomUUID().toString().substring(0, 8), owner);
			Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 10, "update_concurrent");

			// 5명의 모임원 생성 및 미리 참석
			for (int i = 0; i < 5; i++) {
				User member = createUser("모임원" + i);
				addMemberToGroup(group, member, Status.MEMBER);
				joinUserToMeeting(meeting, member);
				members.add(member);
			}
			meetingRepository.save(meeting); // joinCount 변경사항 최종 저장

			ownerHolder.value = owner;
			meetingIdHolder.value = meeting.getId();
		});

		final User owner = ownerHolder.value;
		final Long meetingId = meetingIdHolder.value;

		CountDownLatch latch = new CountDownLatch(2);
		AtomicInteger exceptionCount = new AtomicInteger(0);

		// when
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		// 스레드 1: 관리자가 정원을 5명으로 줄이려고 시도 (현재 인원 6명보다 적으므로 실패해야 함)
		executorService.submit(() -> {
			try {
				setAuthContext(owner.getId());
				var request = new com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto(
						"수정된 제목", LocalDateTime.now().plusDays(1), "수정된 장소", null, 5, 0
				);
				meetingFacadeService.updateMeeting(meetingId, request);
			} catch (Exception e) {
				System.out.println("예상된 예외 발생(정원 축소 실패): " + e.getMessage());
				exceptionCount.incrementAndGet();
			} finally {
				clearAuthContext();
				latch.countDown();
			}
		});

		// 스레드 2: 모임원 중 한 명이 탈퇴 시도
		executorService.submit(() -> {
			try {
				User leavingMember = members.get(0);
				setAuthContext(leavingMember.getId());
				meetingFacadeService.leaveMeeting(meetingId);
			} catch (Exception e) {
				System.err.println("참석 취소 중 예상치 못한 오류 발생: " + e.getMessage());
			} finally {
				clearAuthContext();
				latch.countDown();
			}
		});

		latch.await();
		executorService.shutdown();

		// then
		Meeting finalMeeting = meetingQueryService.getById(meetingId);

		// 어떤 경우에도 최종 인원(5명)이 정원보다 많아지는 상황은 없어야 함
		assertThat(finalMeeting.getJoinCount()).isEqualTo(5);
		assertThat(finalMeeting.getCapacity()).isGreaterThanOrEqualTo(finalMeeting.getJoinCount());

		if (exceptionCount.get() == 1) { // 시나리오 1: update가 먼저 실패
			assertThat(finalMeeting.getCapacity()).isEqualTo(10);
			System.out.println("시나리오 1 통과: 정원 축소 실패 후 참석 취소 성공");
		} else { // 시나리오 2: leave가 먼저 성공
			assertThat(finalMeeting.getCapacity()).isEqualTo(5);
			System.out.println("시나리오 2 통과: 참석 취소 성공 후 정원 축소 성공");
		}

		cleanupTestData(meetingId);
	}

	@Test
	@DisplayName("09.동시성 테스트 - 20명이 3명 정원에 동시 참석 신청")
	void test09_concurrentJoinMeeting_WithFacade() throws InterruptedException {
		// given
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		List<User> userList = new ArrayList<>();
		Long meetingId = null;

		try {
			tx.begin();

			User owner = User.builder().name("모임장_" + UUID.randomUUID().toString().substring(0, 8)).build();
			em.persist(owner);

			Group group = Group.builder()
				.name("테스트모임_" + UUID.randomUUID().toString().substring(0, 8))
				.capacity(100)
				.build();
			em.persist(group);
			em.persist(GroupUser.create(group, owner, Status.OWNER));

			Meeting meeting = Meeting.meetingCreateBuilder()
				.group(group)
				.type(MeetingType.FLASH)
				.title("테스트일정")
				.startAt(LocalDateTime.now().plusDays(30))
				.placeName("테스트장소")
				.capacity(3)
				.cost(0)
				.creator(owner)
				.build();
			em.persist(meeting);

			UserMeeting ownerMeeting = UserMeeting.create(meeting, owner);
			em.persist(ownerMeeting);
			meeting.creatorJoin();
			meetingId = meeting.getId();

			for (int i = 0; i < 20; i++) {
				User member = User.builder().name("모임원" + i).build();
				em.persist(member);
				em.persist(GroupUser.create(group, member, Status.MEMBER));
				userList.add(member);
			}

			tx.commit();
		} finally {
			em.close();
		}

		final Long finalMeetingId = meetingId;

		// when - 20명이 동시에 3명 정원에 참석 신청
		int taskCount = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		CountDownLatch latch = new CountDownLatch(taskCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		for (int i = 0; i < taskCount; i++) {
			final int idx = i;
			executorService.submit(() -> {
				try {
					// 각 스레드마다 인증 정보 설정
					Long userId = userList.get(idx).getId();
					var detail = new CustomUserDetails(userId, "test", "test");
					var authenticated = UsernamePasswordAuthenticationToken.authenticated(
						detail, null, null);
					SecurityContextHolder.getContext().setAuthentication(authenticated);

					meetingFacadeService.joinMeeting(finalMeetingId);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					// 스레드 인증 정보 삭제
					SecurityContextHolder.clearContext();
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		// then - 정확히 3명만 참석해야 함 (생성자 1명 + 신청자 2명)
		Meeting finalMeeting = meetingQueryService.getById(finalMeetingId);
		assertThat(finalMeeting.getJoinCount()).isEqualTo(3);
		assertThat(successCount.get()).isEqualTo(2);  // 2명만 성공
		assertThat(failCount.get()).isEqualTo(18);    // 18명 실패

		long actualParticipants = userMeetingRepository.countByMeetingId(finalMeetingId);
		assertThat(actualParticipants).isEqualTo(3);

		cleanupTestData(finalMeetingId);
	}

	public void cleanupTestData(Long meetingId) {
		transactionTemplate.execute(status -> {
			userMeetingRepository.deleteByMeetingId(meetingId);
			groupUserRepository.deleteAll();
			meetingRepository.deleteAll();
			userRepository.deleteAll();
			groupRepository.deleteAll();
			return null;
		});
	}

	// Helper Methods

	private User createUser(String name) {
		User user = User.builder().name(name).build();
		return userRepository.save(user);
	}

	private Group createGroup(String name, User owner) {
		Group group = Group.builder()
			.name(name)
			.capacity(100)
			.build();
		group = groupRepository.save(group);

		GroupUser groupUser = GroupUser.create(group, owner, Status.OWNER);
		groupUserRepository.save(groupUser);

		return group;
	}

	private void addMemberToGroup(Group group, User user, Status status) {
		GroupUser groupUser = GroupUser.create(group, user, status);
		groupUserRepository.save(groupUser);
	}

	private Meeting createMeeting(Group group, User creator, MeetingType type, int capacity, String uniqueId) {
		LocalDateTime startTime = LocalDateTime.now().plusDays(30); // 30일 후로 설정

		Meeting meeting = Meeting.meetingCreateBuilder()
			.group(group)
			.type(type)
			.title("테스트일정_" + uniqueId)
			.startAt(startTime)
			.placeName("테스트장소")
			.capacity(capacity)
			.cost(0)
			.creator(creator)
			.build();
		meeting = meetingRepository.save(meeting);

		UserMeeting userMeeting = UserMeeting.create(meeting, creator);
		userMeetingRepository.save(userMeeting);
		meeting.creatorJoin();

		return meeting;
	}

	private void joinUserToMeeting(Meeting meeting, User user) {
		UserMeeting userMeeting = UserMeeting.create(meeting, user);
		userMeetingRepository.save(userMeeting);
		meeting.creatorJoin() ;
	}

	private void setAuthContext(Long userId) {
		var detail = new CustomUserDetails(userId, "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}

	private void clearAuthContext() {
		SecurityContextHolder.clearContext();
	}

	private static class Holder<T> {
		T value;
	}
}
