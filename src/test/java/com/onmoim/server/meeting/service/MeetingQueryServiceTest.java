package com.onmoim.server.meeting.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.dto.CursorPageResponse;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
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
class MeetingQueryServiceTest {

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

	@Test
	@DisplayName("일정 ID로 조회 성공")
	@Transactional
	void getById_Success() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		Meeting meeting = createAndSaveMeeting(group, owner, MeetingType.REGULAR, "정기모임 테스트");

		// when
		Meeting foundMeeting = meetingQueryService.getById(meeting.getId());

		// then
		assertThat(foundMeeting.getId()).isEqualTo(meeting.getId());
		assertThat(foundMeeting.getTitle()).isEqualTo("정기모임 테스트");
		assertThat(foundMeeting.getType()).isEqualTo(MeetingType.REGULAR);
	}

	@Test
	@DisplayName("일정 ID로 조회 실패 - 존재하지 않는 일정")
	@Transactional
	void getById_Fail_NotExists() {
		// when & then
		assertThatThrownBy(() -> meetingQueryService.getById(999L))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.NOT_EXISTS_GROUP.getDetail());
	}

	@Test
	@DisplayName("일정 ID로 조회 실패 - 삭제된 일정")
	@Transactional
	void getById_Fail_SoftDeleted() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		Meeting meeting = createAndSaveMeeting(group, owner, MeetingType.REGULAR, "삭제된 일정");
		meeting.softDelete();

		// when & then
		assertThatThrownBy(() -> meetingQueryService.getById(meeting.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.NOT_EXISTS_GROUP.getDetail());
	}

	@Test
	@DisplayName("그룹별 예정된 일정 조회 - 타입 필터링 없음")
	@Transactional
	void getUpcomingMeetingsInGroup_Success_NoTypeFilter() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		
		// 과거, 현재, 미래 일정 생성
		createAndSaveMeeting(group, owner, MeetingType.REGULAR, "과거 일정", 
			LocalDateTime.now().minusDays(1));
		createAndSaveMeeting(group, owner, MeetingType.FLASH, "미래 정기모임", 
			LocalDateTime.now().plusDays(1));
		createAndSaveMeeting(group, owner, MeetingType.FLASH, "미래 번개모임", 
			LocalDateTime.now().plusDays(2));

		// when
		CursorPageResponse<MeetingResponseDto> result = 
			meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), null, null, 10);

		// then
		assertThat(result.getContent()).hasSize(2); // 미래 일정만
		assertThat(result.getContent().get(0).getTitle()).isEqualTo("미래 정기모임");
		assertThat(result.getContent().get(1).getTitle()).isEqualTo("미래 번개모임");
	}

	@Test
	@DisplayName("그룹별 예정된 일정 조회 - 타입 필터링 적용")
	@Transactional
	void getUpcomingMeetingsInGroup_Success_WithTypeFilter() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		
		createAndSaveMeeting(group, owner, MeetingType.REGULAR, "정기모임 1", 
			LocalDateTime.now().plusDays(1));
		createAndSaveMeeting(group, owner, MeetingType.FLASH, "번개모임 1", 
			LocalDateTime.now().plusDays(2));
		createAndSaveMeeting(group, owner, MeetingType.REGULAR, "정기모임 2", 
			LocalDateTime.now().plusDays(3));

		// when - 정기모임만 조회
		CursorPageResponse<MeetingResponseDto> result = 
			meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), MeetingType.REGULAR, null, 10);

		// then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent()).allMatch(dto -> dto.getType() == MeetingType.REGULAR);
	}

	@Test
	@DisplayName("그룹별 예정된 일정 조회 - 페이지네이션")
	@Transactional
	void getUpcomingMeetingsInGroup_Success_Pagination() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		
		// 시간순으로 5개 일정 생성
		Meeting meeting1 = createAndSaveMeeting(group, owner, MeetingType.REGULAR, "일정 1", 
			LocalDateTime.now().plusDays(1));
		Meeting meeting2 = createAndSaveMeeting(group, owner, MeetingType.REGULAR, "일정 2", 
			LocalDateTime.now().plusDays(2));
		Meeting meeting3 = createAndSaveMeeting(group, owner, MeetingType.REGULAR, "일정 3", 
			LocalDateTime.now().plusDays(3));

		// when - 첫 번째 페이지 (크기: 2)
		CursorPageResponse<MeetingResponseDto> page1 = 
			meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), null, null, 2);

		// then - 첫 번째 페이지 검증
		assertThat(page1.getContent()).hasSize(2);
		assertThat(page1.isHasNext()).isTrue();
		assertThat(page1.getNextCursorId()).isNotNull();

		// when - 두 번째 페이지
		CursorPageResponse<MeetingResponseDto> page2 = 
			meetingQueryService.getUpcomingMeetingsInGroup(group.getId(), null, page1.getNextCursorId(), 2);

		// then - 두 번째 페이지 검증
		assertThat(page2.getContent()).hasSize(1);
		assertThat(page2.isHasNext()).isFalse();
	}

	@Test
	@DisplayName("내가 참여한 예정된 일정 조회")
	@Transactional
	void getMyUpcomingMeetings_Success() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		
		Group group1 = createGroup("테스트 모임 1", owner);
		Group group2 = createGroup("테스트 모임 2", owner);
		addMemberToGroup(group1, member, Status.MEMBER);
		addMemberToGroup(group2, member, Status.MEMBER);
		
		Meeting meeting1 = createAndSaveMeeting(group1, owner, MeetingType.REGULAR, "일정 1", 
			LocalDateTime.now().plusDays(1));
		Meeting meeting2 = createAndSaveMeeting(group2, owner, MeetingType.FLASH, "일정 2", 
			LocalDateTime.now().plusDays(2));
		Meeting meeting3 = createAndSaveMeeting(group1, owner, MeetingType.REGULAR, "일정 3", 
			LocalDateTime.now().plusDays(3));

		// member를 일정에 참석시킴
		joinUserToMeeting(meeting1, member);
		joinUserToMeeting(meeting2, member);
		// meeting3에는 참석하지 않음

		// when
		CursorPageResponse<MeetingResponseDto> result = 
			meetingQueryService.getMyUpcomingMeetings(member.getId(), null, 10);

		// then
		assertThat(result.getContent()).hasSize(2); // member가 참석한 일정만
		assertThat(result.getContent().get(0).getTitle()).isEqualTo("일정 1");
		assertThat(result.getContent().get(1).getTitle()).isEqualTo("일정 2");
	}

	@Test
	@DisplayName("내가 참여한 예정된 일정 조회 - 페이지네이션")
	@Transactional
	void getMyUpcomingMeetings_Success_Pagination() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		
		// 5개 일정 생성 및 참석
		for (int i = 1; i <= 5; i++) {
			Meeting meeting = createAndSaveMeeting(group, owner, MeetingType.FLASH, "일정 " + i, 
				LocalDateTime.now().plusDays(i));
			joinUserToMeeting(meeting, member);
		}

		// when - 첫 번째 페이지 (크기: 3)
		CursorPageResponse<MeetingResponseDto> page1 = 
			meetingQueryService.getMyUpcomingMeetings(member.getId(), null, 3);

		// then - 첫 번째 페이지 검증
		assertThat(page1.getContent()).hasSize(3);
		assertThat(page1.isHasNext()).isTrue();

		// when - 두 번째 페이지
		CursorPageResponse<MeetingResponseDto> page2 = 
			meetingQueryService.getMyUpcomingMeetings(member.getId(), page1.getNextCursorId(), 3);

		// then - 두 번째 페이지 검증
		assertThat(page2.getContent()).hasSize(2);
		assertThat(page2.isHasNext()).isFalse();
	}

	@Test
	@DisplayName("내가 참여한 예정된 일정 조회 - 참석하지 않은 일정 제외")
	@Transactional
	void getMyUpcomingMeetings_Success_OnlyJoinedMeetings() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		
		Meeting joinedMeeting = createAndSaveMeeting(group, owner, MeetingType.FLASH, "참석한 일정", 
			LocalDateTime.now().plusDays(1));
		Meeting notJoinedMeeting = createAndSaveMeeting(group, owner, MeetingType.FLASH, "참석하지 않은 일정", 
			LocalDateTime.now().plusDays(2));

		// member는 첫 번째 일정에만 참석
		joinUserToMeeting(joinedMeeting, member);

		// when
		CursorPageResponse<MeetingResponseDto> result = 
			meetingQueryService.getMyUpcomingMeetings(member.getId(), null, 10);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getTitle()).isEqualTo("참석한 일정");
	}

	// === Helper Methods ===

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

	private Meeting createAndSaveMeeting(Group group, User creator, MeetingType type, String title) {
		return createAndSaveMeeting(group, creator, type, title, LocalDateTime.now().plusDays(1));
	}

	private Meeting createAndSaveMeeting(Group group, User creator, MeetingType type, String title, LocalDateTime startAt) {
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(group.getId())
			.type(type)
			.title(title)
			.startAt(startAt)
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(creator.getId())
			.build();
		return meetingRepository.save(meeting);
	}

	private void joinUserToMeeting(Meeting meeting, User user) {
		UserMeeting userMeeting = UserMeeting.create(meeting, user);
		userMeetingRepository.save(userMeeting);
	}
} 