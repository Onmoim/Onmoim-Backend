package com.onmoim.server.meeting.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.user.entity.User;

class MeetingTest {

	@Test
	@DisplayName("정기모임 생성 권한 확인 - 모임장만 가능")
	void canBeCreatedBy_RegularMeeting_OwnerOnly() {
		// given
		Meeting regularMeeting = createRegularMeeting();
		User user = createUser();
		GroupUser owner = createGroupUser(user, Status.OWNER);
		GroupUser member = createGroupUser(user, Status.MEMBER);

		// when & then
		assertThat(regularMeeting.canBeCreatedBy(owner)).isTrue();
		assertThat(regularMeeting.canBeCreatedBy(member)).isFalse();
	}

	@Test
	@DisplayName("번개모임 생성 권한 확인 - 모임원도 가능")
	void canBeCreatedBy_FlashMeeting_MemberAllowed() {
		// given
		Meeting flashMeeting = createFlashMeeting();
		User user = createUser();
		GroupUser owner = createGroupUser(user, Status.OWNER);
		GroupUser member = createGroupUser(user, Status.MEMBER);

		// when & then
		assertThat(flashMeeting.canBeCreatedBy(owner)).isTrue();
		assertThat(flashMeeting.canBeCreatedBy(member)).isTrue();
	}

	@Test
	@DisplayName("일정 관리 권한 확인 - 모임장만 가능")
	void canBeManagedBy_OwnerOnly() {
		// given
		Meeting meeting = createRegularMeeting();
		User user = createUser();
		GroupUser owner = createGroupUser(user, Status.OWNER);
		GroupUser member = createGroupUser(user, Status.MEMBER);

		// when & then
		assertThat(meeting.canBeManagedBy(owner)).isTrue();
		assertThat(meeting.canBeManagedBy(member)).isFalse();
	}

	@Test
	@DisplayName("정기모임 이미지 업로드 권한 확인 - 모임장만 가능")
	void canUpdateImageBy_RegularMeeting_OwnerOnly() {
		// given
		Meeting regularMeeting = createRegularMeeting();
		User user = createUser();
		GroupUser owner = createGroupUser(user, Status.OWNER);
		GroupUser member = createGroupUser(user, Status.MEMBER);

		// when & then
		assertThat(regularMeeting.canUpdateImageBy(owner)).isTrue();
		assertThat(regularMeeting.canUpdateImageBy(member)).isFalse();
	}

	@Test
	@DisplayName("번개모임 이미지 업로드 권한 확인 - 모임장 또는 주최자")
	void canUpdateImageBy_FlashMeeting_OwnerOrCreator() {
		// given
		User creator = createUser(1L);
		User otherUser = createUser(2L);
		Meeting flashMeeting = createFlashMeetingWithCreator(creator.getId());

		GroupUser owner = createGroupUser(otherUser, Status.OWNER);
		GroupUser creatorAsGroupUser = createGroupUser(creator, Status.MEMBER);
		GroupUser otherMember = createGroupUser(otherUser, Status.MEMBER);

		// when & then
		assertThat(flashMeeting.canUpdateImageBy(owner)).isTrue(); // 모임장
		assertThat(flashMeeting.canUpdateImageBy(creatorAsGroupUser)).isTrue(); // 주최자
		assertThat(flashMeeting.canUpdateImageBy(otherMember)).isFalse(); // 다른 모임원
	}

	@Test
	@DisplayName("이미지 삭제 권한 확인 - 모임장만 가능")
	void canDeleteImageBy_OwnerOnly() {
		// given
		Meeting meeting = createFlashMeeting();
		User user = createUser();
		GroupUser owner = createGroupUser(user, Status.OWNER);
		GroupUser member = createGroupUser(user, Status.MEMBER);

		// when & then
		assertThat(meeting.canDeleteImageBy(owner)).isTrue();
		assertThat(meeting.canDeleteImageBy(member)).isFalse();
	}

	@Test
	@DisplayName("일정 참석 가능 여부 확인 - 정상 상황")
	void canJoin_Success() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(7)) // 충분히 미래로 설정
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		// when & then
		assertThat(meeting.canJoin()).isTrue();
	}

	@Test
	@DisplayName("일정 참석 불가 - 정원 초과")
	void canJoin_Fail_CapacityFull() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(7))
			.placeName("테스트 장소")
			.capacity(2)
			.cost(0)
			.creatorId(1L)
			.build();

		// 정원까지 참석 처리
		meeting.join();
		meeting.join();

		// when & then
		assertThat(meeting.canJoin()).isFalse();
		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.FULL);
	}

	@Test
	@DisplayName("일정 참석 불가 - 이미 시작됨")
	void canJoin_Fail_AlreadyStarted() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().minusHours(1)) // 과거 일정
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		// when & then
		assertThat(meeting.canJoin()).isFalse();
		assertThat(meeting.isStarted()).isTrue();
	}

	@Test
	@DisplayName("일정 참석 처리 성공")
	void join_Success() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(7))
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		int initialCount = meeting.getJoinCount();

		// when
		meeting.join();

		// then
		assertThat(meeting.getJoinCount()).isEqualTo(initialCount + 1);
		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.OPEN);
	}

	@Test
	@DisplayName("일정 참석 처리 - 정원 가득 차면 FULL 상태로 변경")
	void join_StatusChangesToFull() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(7)) // 충분히 미래로 설정
			.placeName("테스트 장소")
			.capacity(2)
			.cost(0)
			.creatorId(1L)
			.build();

		// when
		meeting.join();
		meeting.join(); // 정원 가득

		// then
		assertThat(meeting.getJoinCount()).isEqualTo(2);
		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.FULL);
	}

	@Test
	@DisplayName("일정 참석 처리 실패 - 정원 초과")
	void join_Fail_CapacityExceeded() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(7)) // 충분히 미래로 설정
			.placeName("테스트 장소")
			.capacity(1)
			.cost(0)
			.creatorId(1L)
			.build();

		meeting.join(); // 정원 가득

		// when & then
		assertThatThrownBy(() -> meeting.join())
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.GROUP_CAPACITY_EXCEEDED.getDetail());
	}

	@Test
	@DisplayName("일정 참석 취소 성공")
	void leave_Success() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(7)) // 충분히 미래로 설정
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		meeting.join();
		meeting.join();
		int beforeCount = meeting.getJoinCount();

		// when
		meeting.leave();

		// then
		assertThat(meeting.getJoinCount()).isEqualTo(beforeCount - 1);
	}

	@Test
	@DisplayName("일정 참석 취소 - FULL에서 OPEN으로 상태 변경")
	void leave_StatusChangesToOpen() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(7)) // 충분히 미래로 설정
			.placeName("테스트 장소")
			.capacity(2)
			.cost(0)
			.creatorId(1L)
			.build();

		meeting.join();
		meeting.join(); // FULL 상태

		// when
		meeting.leave();

		// then
		assertThat(meeting.getJoinCount()).isEqualTo(1);
		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.OPEN);
	}

	@Test
	@DisplayName("일정 참석 취소 실패 - 이미 시작되었고 참석자가 2명 이상")
	void leave_Fail_AlreadyStarted() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().minusHours(1)) // 과거 일정
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		// 참석자 2명 추가 (자동 삭제 조건을 벗어나도록)
		meeting.creatorJoin(); // 1명
		meeting.creatorJoin(); // 2명

		// when & then
		assertThatThrownBy(() -> meeting.leave())
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.MEETING_ALREADY_CLOSED.getDetail());
	}

	@Test
	@DisplayName("일정 참석 취소 성공 - 이미 시작되었지만 참석자가 1명 이하 (자동 삭제 조건)")
	void leave_Success_AlreadyStartedButAutoDeleteCondition() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().minusHours(1)) // 과거 일정
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		// 참석자 1명만 추가 (자동 삭제 조건)
		meeting.creatorJoin(); // 1명

		// when
		meeting.leave();

		// then
		assertThat(meeting.getJoinCount()).isEqualTo(0);
	}

	@Test
	@DisplayName("자동 삭제 대상 확인 - 참석자 1명 이하이고, 일정이 시작된 경우")
	void shouldBeAutoDeleted_True() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(1)) // 미래 시간으로 생성
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		meeting.join(); // 1명 참석 (미래 모임이므로 정상적으로 참여 가능)

		// 참석 후 시작 시간을 과거로 변경 (이미 시작된 상태로 만들기)
		try {
			java.lang.reflect.Field startAtField = Meeting.class.getDeclaredField("startAt");
			startAtField.setAccessible(true);
			startAtField.set(meeting, LocalDateTime.now().minusDays(1));
		} catch (Exception e) {
			throw new RuntimeException("테스트 설정 실패", e);
		}

		// when & then
		assertThat(meeting.shouldBeAutoDeleted()).isTrue();
	}

	@Test
	@DisplayName("자동 삭제 대상 확인 실패 - 일정이 아직 시작되지 않은 경우")
	void shouldBeAutoDeleted_False_NotStarted() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(1)) // 일정이 미래
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		meeting.join(); // 1명 참석

		// when & then
		assertThat(meeting.shouldBeAutoDeleted()).isFalse();
	}

	@Test
	@DisplayName("일정 수정 가능 여부 확인 - 24시간 전")
	void canBeUpdated_Success() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.REGULAR)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusDays(2)) // 48시간 후
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		// when & then
		assertThat(meeting.canBeUpdated()).isTrue();
	}

	@Test
	@DisplayName("일정 수정 불가 - 24시간 이내")
	void canBeUpdated_Fail_TooLate() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.REGULAR)
			.title("테스트 일정")
			.startAt(LocalDateTime.now().plusHours(12)) // 12시간 후
			.placeName("테스트 장소")
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();

		// when & then
		assertThat(meeting.canBeUpdated()).isFalse();
	}

	@Test
	@DisplayName("일정 정보 수정")
	void updateMeetingInfo_Success() {
		// given
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.REGULAR)
			.title("원래 제목")
			.startAt(LocalDateTime.now().plusDays(2))
			.placeName("원래 장소")
			.geoPoint(new GeoPoint(37.498, 127.027))
			.capacity(10)
			.cost(1000)
			.creatorId(1L)
			.build();

		// when
		LocalDateTime newStartAt = LocalDateTime.now().plusDays(3);
		GeoPoint newGeoPoint = new GeoPoint(37.555, 126.936);

		meeting.updateMeetingInfo(
			"새로운 제목",
			newStartAt,
			"새로운 장소",
			newGeoPoint,
			15,
			2000
		);

		// then
		assertThat(meeting.getTitle()).isEqualTo("새로운 제목");
		assertThat(meeting.getStartAt()).isEqualTo(newStartAt);
		assertThat(meeting.getPlaceName()).isEqualTo("새로운 장소");
		assertThat(meeting.getGeoPoint()).isEqualTo(newGeoPoint);
		assertThat(meeting.getCapacity()).isEqualTo(15);
		assertThat(meeting.getCost()).isEqualTo(2000);
	}

	@Test
	@DisplayName("일정 이미지 업데이트")
	void updateImage_Success() {
		// given
		Meeting meeting = createRegularMeeting();
		String newImageUrl = "https://example.com/new-image.jpg";

		// when
		meeting.updateImage(newImageUrl);

		// then
		assertThat(meeting.getImgUrl()).isEqualTo(newImageUrl);
	}

	@Test
	@DisplayName("소프트 삭제")
	void softDelete_Success() {
		// given
		Meeting meeting = createRegularMeeting();

		// when
		meeting.softDelete();

		// then
		assertThat(meeting.getDeletedDate()).isNotNull();
	}

	//Helper Methods

	private Meeting createRegularMeeting() {
		return Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.REGULAR)
			.title("정기모임 테스트")
			.startAt(LocalDateTime.now().plusDays(1))
			.placeName("테스트 장소")
			.geoPoint(new GeoPoint(37.498, 127.027))
			.capacity(10)
			.cost(0)
			.creatorId(1L)
			.build();
	}

	private Meeting createFlashMeeting() {
		return Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("번개모임 테스트")
			.startAt(LocalDateTime.now().plusDays(1)) // 충분히 미래로 설정
			.placeName("테스트 장소")
			.geoPoint(new GeoPoint(37.498, 127.027))
			.capacity(5)
			.cost(0)
			.creatorId(1L)
			.build();
	}

	private Meeting createFlashMeetingWithCreator(Long creatorId) {
		return Meeting.meetingCreateBuilder()
			.groupId(1L)
			.type(MeetingType.FLASH)
			.title("번개모임 테스트")
			.startAt(LocalDateTime.now().plusDays(1)) // 충분히 미래로 설정
			.placeName("테스트 장소")
			.geoPoint(new GeoPoint(37.498, 127.027))
			.capacity(5)
			.cost(0)
			.creatorId(creatorId)
			.build();
	}

	private User createUser() {
		return createUser(1L);
	}

	private User createUser(Long id) {
		User user = User.builder()
			.name("테스트 사용자")
			.build();

		try {
			java.lang.reflect.Field idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(user, id);
		} catch (Exception e) {
		}

		return user;
	}

	private GroupUser createGroupUser(User user, Status status) {
		// Dummy Group 객체 생성 (테스트용)
		Group dummyGroup = Group.groupCreateBuilder()
			.name("테스트 그룹")
			.capacity(100)
			.build();

		return GroupUser.create(dummyGroup, user, status);
	}
}
