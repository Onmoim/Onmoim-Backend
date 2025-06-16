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
		assertThat(meeting.getCreatorId()).isEqualTo(owner.getId());

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
		assertThat(meeting.getCreatorId()).isEqualTo(member.getId());

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
	@DisplayName("06. 일정 참석 취소 시 자동 삭제")
	@Transactional
	@Rollback
	void test06_leaveMeeting_AutoDelete() {
		// given
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		User owner = createUser("모임장_" + uniqueId);
		Group group = createGroup("테스트모임_" + uniqueId, owner);
		Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 5, uniqueId);

		setAuthContext(owner.getId());

		// when
		meetingService.leaveMeeting(meeting.getId());

		// then
		Meeting deletedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
		assertThat(deletedMeeting.getDeletedDate()).isNotNull();
		assertThat(deletedMeeting.getJoinCount()).isEqualTo(0);

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
		meetingService.updateMeeting(meeting.getId(), request, null);

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
	@DisplayName("09.동시성 테스트 - 20명이 3명 정원에 동시 참석 신청")
	void test09_concurrentJoinMeeting_WithFacade() throws InterruptedException {
		// given
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		List<User> userList = new ArrayList<>();
		Long meetingId = null;

		try {
			tx.begin();

			User owner = User.builder().name("모임장").build();
			em.persist(owner);

			Group group = Group.groupCreateBuilder()
				.name("테스트모임")
				.capacity(100)
				.build();
			em.persist(group);
			em.persist(GroupUser.create(group, owner, Status.OWNER));

			Meeting meeting = Meeting.meetingCreateBuilder()
				.groupId(group.getId())
				.type(MeetingType.FLASH)
				.title("테스트일정")
				.startAt(LocalDateTime.now().plusDays(30))
				.placeName("테스트장소")
				.capacity(3)
				.cost(0)
				.creatorId(owner.getId())
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

					// 🎭 파사드 패턴 사용 (완벽한 Named Lock 보장)
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

		System.out.println("🔥 MeetingService 극한 동시성 테스트 성공!");
		System.out.println("   - 경쟁률: 10:1 (20명 중 2명만 성공!)");

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
		Group group = Group.groupCreateBuilder()
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
			.groupId(group.getId())
			.type(type)
			.title("테스트일정_" + uniqueId)
			.startAt(startTime)
			.placeName("테스트장소")
			.capacity(capacity)
			.cost(0)
			.creatorId(creator.getId())
			.build();
		meeting = meetingRepository.save(meeting);

		// 생성자 자동 참석 (시간 제약 없음)
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
}
