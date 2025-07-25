package com.onmoim.server.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

@SpringBootTest
@Transactional
class MeetingQueryServiceTest {

	@Autowired private MeetingQueryService meetingQueryService;
	@Autowired private UserRepository userRepository;
	@Autowired private GroupRepository groupRepository;
	@Autowired private GroupUserRepository groupUserRepository;
	@Autowired private MeetingRepository meetingRepository;
	@Autowired private UserMeetingRepository userMeetingRepository;

	private User leader;
	private User member;
	private Group group;

	@BeforeEach
	void setUp() {
		leader = userRepository.save(User.builder().name("모임장").build());
		member = userRepository.save(User.builder().name("멤버").build());
		group = groupRepository.save(Group.builder().name("테스트 그룹").capacity(100).build());
		groupUserRepository.save(GroupUser.create(group, leader, Status.OWNER));
		groupUserRepository.save(GroupUser.create(group, member, Status.MEMBER));
	}

	@Test
	@DisplayName("그룹 모임 전체 조회 (타입 필터 X)")
	void getUpcomingMeetingsInGroup_All() {
		// Given: 20개의 모임 생성
		createTestMeetings(20);

		// When: 첫 페이지 조회 (충분히 큰 사이즈로 전체 조회)
		CursorPageResponseDto<MeetingResponseDto> result = meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), null, null, 20);

		// Then: 20개 모두 조회됨
		assertThat(result.getContent()).hasSize(20);
	}

	@Test
	@DisplayName("그룹 모임 타입별 조회 (정기모임)")
	void getUpcomingMeetingsInGroup_ByType() {
		// Given: 정기모임 10개, 번개모임 10개 생성
		createTestMeetingsByType(MeetingType.REGULAR, 10);
		createTestMeetingsByType(MeetingType.FLASH, 10);

		// When: 정기모임만 조회
		CursorPageResponseDto<MeetingResponseDto> result = meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), MeetingType.REGULAR, null, 10);

		// Then: 정기모임 10개만 조회됨
		assertThat(result.getContent()).hasSize(10);
		assertThat(result.getContent()).allMatch(meeting -> meeting.getType() == MeetingType.REGULAR);
	}

	@Test
	@DisplayName("사용자 참여 모임 전체 조회")
	void getMyUpcomingMeetings_All() {
		// Given: 그룹에 15개 모임 생성하고 멤버가 일부 참석
		List<Meeting> meetings = createTestMeetings(15);
		for (int i = 0; i < 10; i++) {
			userMeetingRepository.save(UserMeeting.create(meetings.get(i), member));
		}

		// When: 멤버 참여 모임 조회
		CursorPageResponseDto<MeetingResponseDto> result = meetingQueryService.getMyUpcomingMeetings(member.getId(), null, 10);

		// Then: 10개 모임만 조회됨
		assertThat(result.getContent()).hasSize(10);
	}

	@Test
	@DisplayName("그룹 모임 커서 페이징 조회")
	void getUpcomingMeetingsInGroup_Paging() {
		// Given: 25개의 모임 생성
		createTestMeetings(25);
		int pageSize = 10;

		// When: 첫 번째 페이지 조회
		CursorPageResponseDto<MeetingResponseDto> firstPage = meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), null, null, pageSize);

		// Then: 첫 번째 페이지 검증
		assertThat(firstPage.getContent()).hasSize(pageSize);
		assertThat(firstPage.isHasNext()).isTrue();
		assertThat(firstPage.getNextCursorId()).isNotNull();

		// When: 두 번째 페이지 조회
		CursorPageResponseDto<MeetingResponseDto> secondPage = meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), null, firstPage.getNextCursorId(), pageSize);

		// Then: 두 번째 페이지 검증
		assertThat(secondPage.getContent()).hasSize(pageSize);
		assertThat(secondPage.isHasNext()).isTrue();

		// When: 세 번째 페이지 조회
		CursorPageResponseDto<MeetingResponseDto> thirdPage = meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), null, secondPage.getNextCursorId(), pageSize);

		// Then: 세 번째 페이지 검증
		assertThat(thirdPage.getContent()).hasSize(5);
		assertThat(thirdPage.isHasNext()).isFalse();
		assertThat(thirdPage.getNextCursorId()).isNull();
	}

	@Test
	@DisplayName("사용자 참여 모임 커서 페이징 조회")
	void getMyUpcomingMeetings_Paging() {
		// Given: 25개 모임 중 15개만 참여
		List<Meeting> meetings = createTestMeetings(25);
		for (int i = 0; i < 15; i++) {
			userMeetingRepository.save(UserMeeting.create(meetings.get(i), member));
		}
		int pageSize = 10;

		// When: 첫 번째 페이지 조회
		CursorPageResponseDto<MeetingResponseDto> firstPage = meetingQueryService.getMyUpcomingMeetings(member.getId(), null, pageSize);

		// Then: 첫 번째 페이지 검증
		assertThat(firstPage.getContent()).hasSize(pageSize);
		assertThat(firstPage.isHasNext()).isTrue();

		// When: 두 번째 페이지 조회
		CursorPageResponseDto<MeetingResponseDto> secondPage = meetingQueryService.getMyUpcomingMeetings(member.getId(), firstPage.getNextCursorId(), pageSize);

		// Then: 두 번째 페이지 검증
		assertThat(secondPage.getContent()).hasSize(5);
		assertThat(secondPage.isHasNext()).isFalse();
	}

	@Test
	@DisplayName("D-day가 가까운 일정 조회")
	void getUpcomingMeetingsByDday() {
		// Given: 다양한 시작 시간을 가진 10개의 모임 생성
		LocalDateTime now = LocalDateTime.now();
		List<Meeting> meetings = new ArrayList<>();

		// 과거 모임 1개 (조회되지 않아야 함)
		meetings.add(meetingRepository.save(Meeting.meetingCreateBuilder()
			.title("과거 모임")
			.group(group)
			.creator(leader)
			.startAt(now.minusDays(1))
			.placeName("테스트 장소")
			.capacity(10)
			.type(MeetingType.REGULAR)
			.build()));

		// 미래 모임 10개 (다양한 시작 시간)
		for (int i = 0; i < 10; i++) {
			meetings.add(meetingRepository.save(Meeting.meetingCreateBuilder()
				.title("테스트 모임 " + (i + 1))
				.group(group)
				.creator(leader)
				.startAt(now.plusDays(i + 1)) // 1일 후부터 10일 후까지
				.placeName("테스트 장소")
				.capacity(10)
				.type(MeetingType.REGULAR)
				.build()));
		}

		// When: D-day가 가까운 일정 2개 조회
		List<Meeting> result = meetingQueryService.getUpcomingMeetingsByDday(group.getId(), 2);

		// Then: 가장 가까운 미래 일정 2개만 조회되어야 함
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getStartAt()).isEqualTo(now.plusDays(1));
		assertThat(result.get(1).getStartAt()).isEqualTo(now.plusDays(2));
	}

	//테스트 헬퍼 메서드

	private List<Meeting> createTestMeetings(int count) {
		return createTestMeetingsByType(null, count);
	}

	private List<Meeting> createTestMeetingsByType(MeetingType type, int count) {
		List<Meeting> meetings = new ArrayList<>();
		LocalDateTime baseTime = LocalDateTime.now().plusDays(1);

		for (int i = 0; i < count; i++) {
			MeetingType currentType = (type == null) ? (i % 2 == 0 ? MeetingType.REGULAR : MeetingType.FLASH) : type;
			meetings.add(meetingRepository.save(Meeting.meetingCreateBuilder()
				.title("테스트 모임 " + (i + 1))
				.group(group)
				.creator(leader)
				.startAt(baseTime.plusHours(i))
				.placeName("테스트 장소")
				.capacity(10)
				.type(currentType)
				.build()));
		}
		return meetings;
	}
}
