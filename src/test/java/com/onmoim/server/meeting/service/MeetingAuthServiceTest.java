package com.onmoim.server.meeting.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

@SpringBootTest
class MeetingAuthServiceTest {

	@Autowired
	private MeetingAuthService meetingAuthService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GroupRepository groupRepository;
	@Autowired
	private GroupUserRepository groupUserRepository;
	@Autowired
	private MeetingRepository meetingRepository;

	@Test
	@DisplayName("정기모임 생성 권한 검증 성공 - 모임장")
	@Transactional
	void validateCreatePermission_RegularMeeting_Success_Owner() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		Meeting meeting = createRegularMeeting(group.getId(), owner.getId());

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateCreatePermission(group.getId(), owner.getId(), meeting)
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("정기모임 생성 권한 검증 실패 - 모임원")
	@Transactional
	void validateCreatePermission_RegularMeeting_Fail_Member() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = createRegularMeeting(group.getId(), member.getId());

		// when & then
		assertThatThrownBy(() ->
			meetingAuthService.validateCreatePermission(group.getId(), member.getId(), meeting)
		).isInstanceOf(CustomException.class)
		.hasMessage(ErrorCode.GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("번개모임 생성 권한 검증 성공 - 모임원")
	@Transactional
	void validateCreatePermission_FlashMeeting_Success_Member() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = createFlashMeeting(group.getId(), member.getId());

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateCreatePermission(group.getId(), member.getId(), meeting)
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("번개모임 생성 권한 검증 성공 - 모임장")
	@Transactional
	void validateCreatePermission_FlashMeeting_Success_Owner() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		Meeting meeting = createFlashMeeting(group.getId(), owner.getId());

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateCreatePermission(group.getId(), owner.getId(), meeting)
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("일정 생성 권한 검증 실패 - 그룹 멤버 아님")
	@Transactional
	void validateCreatePermission_Fail_NotGroupMember() {
		// given
		User owner = createUser("모임장");
		User outsider = createUser("외부인");
		Group group = createGroup("테스트 모임", owner);
		Meeting meeting = createRegularMeeting(group.getId(), outsider.getId());

		// when & then
		assertThatThrownBy(() ->
			meetingAuthService.validateCreatePermission(group.getId(), outsider.getId(), meeting)
		).isInstanceOf(CustomException.class)
		.hasMessage(ErrorCode.NOT_GROUP_MEMBER.getDetail());
	}

	@Test
	@DisplayName("일정 관리 권한 검증 성공 - 모임장")
	@Transactional
	void validateManagePermission_Success_Owner() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = saveMeeting(createFlashMeeting(group.getId(), member.getId()));

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateManagePermission(meeting, owner.getId())
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("일정 관리 권한 검증 실패 - 모임원")
	@Transactional
	void validateManagePermission_Fail_Member() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = saveMeeting(createFlashMeeting(group.getId(), member.getId()));

		// when & then
		assertThatThrownBy(() ->
			meetingAuthService.validateManagePermission(meeting, member.getId())
		).isInstanceOf(CustomException.class)
		.hasMessage(ErrorCode.GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("정기모임 이미지 업로드 권한 검증 성공 - 모임장")
	@Transactional
	void validateImageUploadPermission_RegularMeeting_Success_Owner() {
		// given
		User owner = createUser("모임장");
		Group group = createGroup("테스트 모임", owner);
		Meeting meeting = saveMeeting(createRegularMeeting(group.getId(), owner.getId()));

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateImageUploadPermission(meeting, owner.getId())
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("정기모임 이미지 업로드 권한 검증 실패 - 모임원")
	@Transactional
	void validateImageUploadPermission_RegularMeeting_Fail_Member() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = saveMeeting(createRegularMeeting(group.getId(), owner.getId()));

		// when & then
		assertThatThrownBy(() ->
			meetingAuthService.validateImageUploadPermission(meeting, member.getId())
		).isInstanceOf(CustomException.class)
		.hasMessage(ErrorCode.GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("번개모임 이미지 업로드 권한 검증 성공 - 주최자")
	@Transactional
	void validateImageUploadPermission_FlashMeeting_Success_Creator() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = saveMeeting(createFlashMeeting(group.getId(), member.getId()));

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateImageUploadPermission(meeting, member.getId())
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("번개모임 이미지 업로드 권한 검증 성공 - 모임장")
	@Transactional
	void validateImageUploadPermission_FlashMeeting_Success_Owner() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = saveMeeting(createFlashMeeting(group.getId(), member.getId()));

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateImageUploadPermission(meeting, owner.getId())
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("번개모임 이미지 업로드 권한 검증 실패 - 다른 모임원")
	@Transactional
	void validateImageUploadPermission_FlashMeeting_Fail_OtherMember() {
		// given
		User owner = createUser("모임장");
		User creator = createUser("주최자");
		User otherMember = createUser("다른 모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, creator, Status.MEMBER);
		addMemberToGroup(group, otherMember, Status.MEMBER);
		Meeting meeting = saveMeeting(createFlashMeeting(group.getId(), creator.getId()));

		// when & then
		assertThatThrownBy(() ->
			meetingAuthService.validateImageUploadPermission(meeting, otherMember.getId())
		).isInstanceOf(CustomException.class)
		.hasMessage(ErrorCode.GROUP_FORBIDDEN.getDetail());
	}

	@Test
	@DisplayName("이미지 삭제 권한 검증 성공 - 모임장")
	@Transactional
	void validateImageDeletePermission_Success_Owner() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = saveMeeting(createFlashMeeting(group.getId(), member.getId()));

		// when & then
		assertThatCode(() ->
			meetingAuthService.validateImageDeletePermission(meeting, owner.getId())
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("이미지 삭제 권한 검증 실패 - 주최자")
	@Transactional
	void validateImageDeletePermission_Fail_Creator() {
		// given
		User owner = createUser("모임장");
		User member = createUser("모임원");
		Group group = createGroup("테스트 모임", owner);
		addMemberToGroup(group, member, Status.MEMBER);
		Meeting meeting = saveMeeting(createFlashMeeting(group.getId(), member.getId()));

		// when & then
		assertThatThrownBy(() ->
			meetingAuthService.validateImageDeletePermission(meeting, member.getId())
		).isInstanceOf(CustomException.class)
		.hasMessage(ErrorCode.GROUP_FORBIDDEN.getDetail());
	}

	// === Helper Methods ===

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

	private Meeting createRegularMeeting(Long groupId, Long creatorId) {
		Group group = groupRepository.findById(groupId).orElseThrow();
		User creator = userRepository.findById(creatorId).orElseThrow();
		
		return Meeting.meetingCreateBuilder()
			.group(group)
			.type(MeetingType.REGULAR)
			.title("정기모임 테스트")
			.startAt(LocalDateTime.now().plusDays(1))
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creator(creator)
			.build();
	}

	private Meeting createFlashMeeting(Long groupId, Long creatorId) {
		Group group = groupRepository.findById(groupId).orElseThrow();
		User creator = userRepository.findById(creatorId).orElseThrow();
		
		return Meeting.meetingCreateBuilder()
			.group(group)
			.type(MeetingType.FLASH)
			.title("번개모임 테스트")
			.startAt(LocalDateTime.now().plusDays(1))
			.placeName("테스트 장소")
			.capacity(5)
			.cost(0)
			.creator(creator)
			.build();
	}

	private Meeting saveMeeting(Meeting meeting) {
		return meetingRepository.save(meeting);
	}
}
