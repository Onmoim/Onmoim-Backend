package com.onmoim.server.group.implement;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

@Transactional
@SpringBootTest
class GroupQueryServiceTest {
	@Autowired
	private GroupQueryService groupQueryService;
	@Autowired
	private LocationRepository locationRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	private Location location;
	private Category category;
	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private MeetingRepository meetingRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GroupUserRepository groupUserRepository;

	@BeforeEach
	void setUp() {
		location = locationRepository.save(Location.create(null, null, null, null, null));
		category = categoryRepository.save(Category.builder().name("카테고리").build());
	}

	@Test
	@DisplayName("모임 저장 성공")
	@Transactional
	void groupSaveSuccess() {
		// given
		var name = "모임이름";
		var description = "설명";
		var capacity = 100;

		// when
		Group group = groupQueryService.saveGroup(
			category,
			location,
			name,
			description,
			capacity
		);

		// then
		Group findGroup = groupQueryService.getById(group.getId());
		assertThat(findGroup).isEqualTo(group);
		assertThat(findGroup.getCategory()).isEqualTo(category);
		assertThat(findGroup.getLocation()).isEqualTo(location);
	}

	@Test
	@DisplayName("그룹 저장 실패 이미 존재하는 모임 이름")
	@Transactional
	void groupSaveFailure() {
		// given
		var name = "모임이름";
		var description = "설명";
		var capacity = 100;

		groupQueryService.saveGroup(
			category,
			location,
			name,
			description,
			capacity
		);

		// expected
		assertThatThrownBy(() ->
			groupQueryService.saveGroup(
			category,
			location,
			name,
			description,
			capacity
		))
		.isInstanceOf(CustomException.class)
		.hasMessage(ErrorCode.ALREADY_EXISTS_GROUP.getDetail());
	}

	@Test
	@DisplayName("활동이 활발한 모임 조회")
	void selectActiveGroups() {
		// given
		var description = "mock description";
		var capacity = 100;

		List<Group> groups = new ArrayList<>();
		// 모임 20개 생성
		for (int i = 0; i < 20; i++) {
			groups.add(Group.builder()
				.category(category)
				.location(location)
				.name("모임_" + i)
				.description(description)
				.capacity(capacity)
				.build());
		}
		groupRepository.saveAll(groups);

		// 미팅 생성
		for (int i = 0; i < 20; i++) {
			saveMeeting(groups.get(i), i + 1);
			if(true) continue;
			for (int j = 0; j < i + 1; j++) {
				meetingRepository.save(Meeting.meetingCreateBuilder()
					.group(groups.get(i))
					.startAt(LocalDateTime.now().plusDays(1))
					.build());
			}
		}
		// expected

		// 첫 번째 활동이 활발한 모임 조회
		int size = 10;
		List<ActiveGroup> groups1 = groupQueryService.readMostActiveGroups(null, null, size);
		assertThat(groups1.size()).isEqualTo(11);

		ActiveGroup last1 = groups1.get(size - 1);
		// 두 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups2 = groupQueryService.readMostActiveGroups(last1.groupId(), last1.upcomingMeetingCount(), size);
		assertThat(groups2.size()).isEqualTo(10);
	}

	@Test
	@DisplayName("활동이 활발한 모임 조회: 일정 개수가 같은 모임이 있는 경우")
	void selectActiveGroups2(){
		// given
		var description = "mock description";
		var capacity = 100;

		List<Group> groups = new ArrayList<>();
		// 모임 10개 생성
		for (int i = 0; i < 10; i++) {
			groups.add(Group.builder()
				.category(category)
				.location(location)
				.name("모임_" + i)
				.description(description)
				.capacity(capacity)
				.build());
		}
		groupRepository.saveAll(groups);

		for (int i = 0; i < 10; i++) {
			if(i == 0){
				saveMeeting(groups.get(i), 2);
				continue;
			}
			if(i < 6){
				saveMeeting(groups.get(i), 4);
				continue;
			}
			saveMeeting(groups.get(i), 3);
		}

		// expected
		int size = 3;

		// 첫 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups1 = groupQueryService.readMostActiveGroups(null, null, size);
		System.out.println("groups1 = " + groups1);
		ActiveGroup last1 = groups1.get(size - 1);
		assertThat(groups1.size()).isEqualTo(4);

		// 두 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups2 = groupQueryService.readMostActiveGroups(last1.groupId(), last1.upcomingMeetingCount(), size);
		System.out.println("groups2 = " + groups2);
		ActiveGroup last2 = groups2.get(size - 1);
		assertThat(groups2.size()).isEqualTo(4);

		// 세 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups3 = groupQueryService.readMostActiveGroups(last2.groupId(), last2.upcomingMeetingCount(), size);
		System.out.println("groups3 = " + groups3);
		assertThat(groups3.size()).isEqualTo(4);
		ActiveGroup last3 = groups3.get(size - 1);

		// 네 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups4 = groupQueryService.readMostActiveGroups(last3.groupId(), last3.upcomingMeetingCount(), size);
		System.out.println("groups4 = " + groups4);
		assertThat(groups4.size()).isEqualTo(1);
	}

	@Test
	@DisplayName("활동이 활발한 모임 조회: 모임 상세 조회")
	void selectActiveGroupsDetail1() {
		// 모임이 없는 경우
		List<ActiveGroupDetail> groupDetails = groupQueryService.readGroupsDetail(List.of());
		assertThat(groupDetails).isEmpty();
	}

	@Test
	@DisplayName("활동이 활발한 모임 조회: 모임 상세 조회")
	void selectActiveGroupsDetail2() {
		// given
		Location location1 = locationRepository.save(Location.create(null, null, null, "mock dong1", null));
		Location location2 = locationRepository.save(Location.create(null, null, null, "mock dong2", null));

		Category category1 = categoryRepository.save(Category.builder().name("mock category1").build());
		Category category2 = categoryRepository.save(Category.builder().name("mock category2").build());

		Group group1 = groupRepository.save(Group.builder()
			.category(category1)
			.location(location1)
			.name("mock group1")
			.description("mock description1")
			.capacity(100)
			.build());

		Group group2 = groupRepository.save(Group.builder()
			.category(category2)
			.location(location2)
			.name("mock group2")
			.description("mock description2")
			.capacity(200)
			.build());

		// 의미없는 모임 생성
		for (int i = 0; i < 20; i++) {
			groupRepository.save(Group.builder().name("useless mock group" + i).build());
		}

		List<User> users = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			User user = User.builder().build();
			userRepository.save(user);
			users.add(user);
		}

		// group1: 3명 group2: 7명
		for (int i = 0; i < 10; i++) {
			if(i < 3){
				if(i == 0){
					saveGroupUser(group1, users.get(i), Status.OWNER);
					continue;
				}
				saveGroupUser(group1, users.get(i), Status.MEMBER);
				continue;
			}
			if(i == 5){
				saveGroupUser(group2, users.get(i), Status.OWNER);
				continue;
			}
			saveGroupUser(group2, users.get(i), Status.MEMBER);
		}

		// when
		List<ActiveGroupDetail> activeGroupDetails = groupQueryService.readGroupsDetail(List.of(group1.getId(), group2.getId()));
		System.out.println(activeGroupDetails);

		// then
		assertThat(activeGroupDetails).hasSize(2);
		ActiveGroupDetail first = activeGroupDetails.getFirst();
		ActiveGroupDetail last = activeGroupDetails.getLast();
		assertThat(first.groupId()).isEqualTo(group1.getId());
		assertThat(last.groupId()).isEqualTo(group2.getId());
		assertThat(first.memberCount()).isEqualTo(3);
		assertThat(last.memberCount()).isEqualTo(7);
	}

	// todo: read group relation 테스트 추가




	private void saveGroupUser(Group group, User user, Status status) {
		GroupUser groupUser = GroupUser.create(group, user, status);
		groupUserRepository.save(groupUser);
	}

	private void saveMeeting(Group group, int count) {
		for (int i = 0; i < count; i++) {
			meetingRepository.save(Meeting.meetingCreateBuilder()
				.group(group)
				.startAt(LocalDateTime.now().plusDays(1))
				.build());
		}
	}
}
