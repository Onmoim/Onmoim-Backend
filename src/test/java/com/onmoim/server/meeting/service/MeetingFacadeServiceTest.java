package com.onmoim.server.meeting.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

/**
 * Meeting Facade Service 테스트
 *
 * 테스트 목표:
 * 1. 파사드 패턴이 제대로 작동하는지
 * 2. 완벽한 Named Lock이 동시성을 제어하는지
 * 3. 락 관리 복잡성이 클라이언트에게 숨겨지는지
 */
@SpringBootTest
class MeetingFacadeServiceTest {

    @Autowired
    private MeetingFacadeService meetingFacadeService; // 파사드 서비스 테스트

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

    @Test
    @DisplayName("파사드 패턴 - 일정 참석 신청 성공")
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

        // when - 파사드 패턴 사용 (락 복잡성 완전 숨김)
        meetingFacadeService.joinMeeting(meeting.getId());

        // then
        Meeting updatedMeeting = meetingQueryService.getById(meeting.getId());
        assertThat(updatedMeeting.getJoinCount()).isEqualTo(2); // 생성자 + 신청자

        boolean isJoined = userMeetingRepository.existsByMeetingIdAndUserId(meeting.getId(), member.getId());
        assertThat(isJoined).isTrue();

        clearAuthContext();
    }

    @Test
    @DisplayName("파사드 패턴 - 일정 참석 취소 성공")
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

        // 2명이 참석 신청 (생성자 포함 총 3명)
        setAuthContext(member1.getId());
        meetingFacadeService.joinMeeting(meeting.getId());
        clearAuthContext();

        setAuthContext(member2.getId());
        meetingFacadeService.joinMeeting(meeting.getId());
        clearAuthContext();

        // when - member1이 참석 취소 (여전히 2명 남아서 자동 삭제 안됨)
        setAuthContext(member1.getId());
        meetingFacadeService.leaveMeeting(meeting.getId());

        // then
        Meeting updatedMeeting = meetingQueryService.getById(meeting.getId());
        assertThat(updatedMeeting.getJoinCount()).isEqualTo(2); // owner + member2 남음

        boolean isJoined = userMeetingRepository.existsByMeetingIdAndUserId(meeting.getId(), member1.getId());
        assertThat(isJoined).isFalse();

        clearAuthContext();
    }

    @Test
    @DisplayName("파사드 패턴 동시성 테스트")
    void testConcurrentJoinMeeting_WithFacadePerfectNamedLock() throws InterruptedException {
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
                .title("파사드동시성테스트일정")
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

            // 20명의 모임원 생성
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

        // 20명이 동시에 3명 정원에 참석 신청
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

                    // 파사드 패턴 사용
                    meetingFacadeService.joinMeeting(finalMeetingId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("참석 신청 실패 - 사용자: " + idx + ", 오류: " + e.getMessage());
                } finally {
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

        // DB 검증
        long actualParticipants = userMeetingRepository.countByMeetingId(finalMeetingId);
        assertThat(actualParticipants).isEqualTo(3);

        System.out.println("🔥 극한 동시성 테스트 성공!");
        System.out.println("   - 총 신청자: 20명");
        System.out.println("   - 정원: 3명 (극한 경쟁!)");
        System.out.println("   - 성공: " + successCount.get() + "명");
        System.out.println("   - 실패: " + failCount.get() + "명");
        System.out.println("   - 최종 참석자: " + finalMeeting.getJoinCount() + "명");
        System.out.println("   - 경쟁률: " + String.format("%.1f", 20.0/2) + ":1 (20명 중 2명만 성공!)");
        System.out.println("   - 락 복잡성: 완전히 숨겨짐 (클라이언트는 단순한 joinMeeting() 호출만)");

        cleanupTestData(finalMeetingId);
    }

    @Test
    @DisplayName("파사드 패턴 - 중복 참석 신청 방지")
    @Transactional
    @Rollback
    void testJoinMeeting_PreventDuplicate_WithFacade() {
        // given
        User owner = createUser("모임장");
        User member = createUser("모임원");
        Group group = createGroup("테스트모임", owner);
        addMemberToGroup(group, member, Status.MEMBER);

        Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 5);

        setAuthContext(member.getId());

        // 첫 번째 참석 신청
        meetingFacadeService.joinMeeting(meeting.getId());

        // when & then - 두 번째 참석 신청 시 예외 발생
        assertThatThrownBy(() -> meetingFacadeService.joinMeeting(meeting.getId()))
            .isInstanceOf(Exception.class);

        clearAuthContext();
    }

    @Test
    @DisplayName("파사드 패턴 - 정원 초과 시 실패")
    @Transactional
    @Rollback
    void testJoinMeeting_CapacityExceeded_WithFacade() {
        // given
        User owner = createUser("모임장");
        User member1 = createUser("모임원1");
        User member2 = createUser("모임원2");
        Group group = createGroup("테스트모임", owner);
        addMemberToGroup(group, member1, Status.MEMBER);
        addMemberToGroup(group, member2, Status.MEMBER);

        Meeting meeting = createMeeting(group, owner, MeetingType.FLASH, 2); // 정원 2명

        // 첫 번째 참석 신청 (생성자 포함 정원 가득)
        setAuthContext(member1.getId());
        meetingFacadeService.joinMeeting(meeting.getId());
        clearAuthContext();

        // when & then - 두 번째 참석 신청 시 정원 초과로 실패
        setAuthContext(member2.getId());
        assertThatThrownBy(() -> meetingFacadeService.joinMeeting(meeting.getId()))
            .isInstanceOf(Exception.class);
        clearAuthContext();
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

    private Meeting createMeeting(Group group, User creator, MeetingType type, int capacity) {
        LocalDateTime startTime = LocalDateTime.now().plusDays(30);

        Meeting meeting = Meeting.meetingCreateBuilder()
            .groupId(group.getId())
            .type(type)
            .title("테스트일정")
            .startAt(startTime)
            .placeName("테스트장소")
            .capacity(capacity)
            .cost(0)
            .creatorId(creator.getId())
            .build();
        meeting = meetingRepository.save(meeting);

        // 생성자 자동 참석
        UserMeeting userMeeting = UserMeeting.create(meeting, creator);
        userMeetingRepository.save(userMeeting);
        meeting.creatorJoin();

        return meeting;
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

    private void cleanupTestData(Long meetingId) {
        transactionTemplate.execute(status -> {
            userMeetingRepository.deleteByMeetingId(meetingId);
            groupUserRepository.deleteAll();
            meetingRepository.deleteAll();
            userRepository.deleteAll();
            groupRepository.deleteAll();
            return null;
        });
    }
}
