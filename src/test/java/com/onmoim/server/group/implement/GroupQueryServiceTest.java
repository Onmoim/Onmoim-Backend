package com.onmoim.server.group.implement;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static com.onmoim.server.group.entity.GroupLikeStatus.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.parameters.P;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupLike;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupLikeRepository;
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
	@Autowired
	private GroupRepository groupRepository;
	@Autowired
	private MeetingRepository meetingRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GroupUserRepository groupUserRepository;
	@Autowired
	private GroupLikeRepository groupLikeRepository;
	private Location location;
	private Category category;

	@BeforeEach
	void setUp() {
		location = locationRepository.save(Location.create(null, null, null, "동동동", null));
		category = categoryRepository.save(Category.builder().name("카테고리").iconUrl("http://s3/mock/image/1").build());
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
		.hasMessage(ALREADY_EXISTS_GROUP.getDetail());
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
		ActiveGroup last1 = groups1.get(size - 1);
		assertThat(groups1.size()).isEqualTo(4);

		// 두 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups2 = groupQueryService.readMostActiveGroups(last1.groupId(), last1.upcomingMeetingCount(), size);
		ActiveGroup last2 = groups2.get(size - 1);
		assertThat(groups2.size()).isEqualTo(4);

		// 세 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups3 = groupQueryService.readMostActiveGroups(last2.groupId(), last2.upcomingMeetingCount(), size);
		assertThat(groups3.size()).isEqualTo(4);
		ActiveGroup last3 = groups3.get(size - 1);

		// 네 번째 활동이 활발한 모임 조회
		List<ActiveGroup> groups4 = groupQueryService.readMostActiveGroups(last3.groupId(), last3.upcomingMeetingCount(), size);
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

		// then
		assertThat(activeGroupDetails).hasSize(2);
		ActiveGroupDetail first = activeGroupDetails.getFirst();
		ActiveGroupDetail last = activeGroupDetails.getLast();
		assertThat(first.groupId()).isEqualTo(group1.getId());
		assertThat(last.groupId()).isEqualTo(group2.getId());
		assertThat(first.memberCount()).isEqualTo(3);
		assertThat(last.memberCount()).isEqualTo(7);
	}

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

	@Test
	@DisplayName("활동이 활발한 모임 조회: 모임과 사용자 관계 조회1")
	void readGroupsRelation1() {
		// given
		Long mockUserId = 1L;
		List<Long> groupIds = List.of();

		// when
		List<ActiveGroupRelation> relations = groupQueryService.readGroupsRelation(groupIds, mockUserId);

		// then
		assertThat(relations).isEmpty();
	}

	@Test
	@DisplayName("활동이 활발한 모임 조회: 모임과 사용자 관계 조회2")
	void readGroupsRelation2() {
		// given
		User currentUser = User.builder().name("online user").build();
		userRepository.save(currentUser);

		// 유저 20명 저장
		List<User> users = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			User user = User.builder().name("mock user" + i).build();
			users.add(userRepository.save(user));
		}

		// 모임 20개 생성
		List<Group> groups = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			Group group = Group.builder().name("mock group " + i)
				.category(category)
				.location(location)
				.capacity(100)
				.build();
			groups.add(groupRepository.save(group));
		}

		// 모임과 사용자 관계 생성
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 20; j++) {
				GroupUser groupUser = GroupUser.create(groups.get(j), users.get(j), Status.MEMBER);
				groupUserRepository.save(groupUser);
			}
		}

		// 현재 유저와 10개의 모임 관계 생성
		for (int i = 0; i < 10; i++) {
			if(i < 2) {
				GroupUser groupUser = GroupUser.create(groups.get(i), currentUser, Status.BAN);
				GroupLike groupLike = GroupLike.create(groups.get(i), currentUser, LIKE);
				groupUserRepository.save(groupUser);
				groupLikeRepository.save(groupLike);
				continue;
			}
			if(i < 4) {
				GroupUser groupUser = GroupUser.create(groups.get(i), currentUser, Status.OWNER);
				GroupLike groupLike = GroupLike.create(groups.get(i), currentUser, LIKE);
				groupUserRepository.save(groupUser);
				groupLikeRepository.save(groupLike);
				continue;
			}
			if(i < 8) {
				GroupUser groupUser = GroupUser.create(groups.get(i), currentUser, Status.MEMBER);
				GroupLike groupLike = GroupLike.create(groups.get(i), currentUser, PENDING);
				groupUserRepository.save(groupUser);
				groupLikeRepository.save(groupLike);
				continue;
			}
			GroupUser groupUser = GroupUser.create(groups.get(i), currentUser, Status.PENDING);
			groupUserRepository.save(groupUser);
		}
		// 모임 20개 id 추출
		List<Long> groupIds = groups.stream().map(Group::getId).collect(Collectors.toList());

		// when
		List<ActiveGroupRelation> relations = groupQueryService.readGroupsRelation(groupIds, currentUser.getId());

		// then
		assertThat(relations).hasSize(20);
		assertThat(relations).containsExactly(
			new ActiveGroupRelation(groups.get(0).getId(), currentUser.getId(), Status.BAN, LIKE),
			new ActiveGroupRelation(groups.get(1).getId(), currentUser.getId(), Status.BAN, LIKE),
			new ActiveGroupRelation(groups.get(2).getId(), currentUser.getId(), Status.OWNER, LIKE),
			new ActiveGroupRelation(groups.get(3).getId(), currentUser.getId(), Status.OWNER, LIKE),
			new ActiveGroupRelation(groups.get(4).getId(), currentUser.getId(), Status.MEMBER, PENDING),
			new ActiveGroupRelation(groups.get(5).getId(), currentUser.getId(), Status.MEMBER, PENDING),
			new ActiveGroupRelation(groups.get(6).getId(), currentUser.getId(), Status.MEMBER, PENDING),
			new ActiveGroupRelation(groups.get(7).getId(), currentUser.getId(), Status.MEMBER, PENDING),
			new ActiveGroupRelation(groups.get(8).getId(), currentUser.getId(), Status.PENDING, null),
			new ActiveGroupRelation(groups.get(9).getId(), currentUser.getId(), Status.PENDING, null),
			new ActiveGroupRelation(groups.get(10).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(11).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(12).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(13).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(14).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(15).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(16).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(17).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(18).getId(), null, null, null),
			new ActiveGroupRelation(groups.get(19).getId(), null, null, null));
	}

	@Test
	@DisplayName("내 주변 인기 모임 조회: 내 주변 모임이 없는 경우")
	void readPopularGroupsNearMe1() {
		// given
		Location myLocation = Location.builder().dong("my location").build();
		locationRepository.save(myLocation);

		// 로케이션 10개 저장
		List<Location> locations = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Location location = Location.builder()
				.dong("mock dong" + i)
				.build();
			locations.add(locationRepository.save(location));
		}

		// 모임 10개 저장
		for (int i = 0; i < 10; i++) {
			Group group = Group.builder()
				.name("mock group" + i)
				.location(locations.get(i))
				.category(category)
				.capacity(100)
				.build();
			groupRepository.save(group);
		}

		// when
		List<PopularGroupSummary> groupSummaries = groupQueryService.readPopularGroupsNearMe(
			myLocation.getId(), null, null, 10);

		// then
		assertThat(groupSummaries).isEmpty();
	}

	@Test
	@DisplayName("내 주변 인기 모임 조회: 파라미터 조건이 없는 경우")
	void readPopularGroupsNearMe2() {
		// given
		Location location = Location.builder().dong("my location").build();
		locationRepository.save(location);

		Category category = Category.builder().name("mock category").build();
		categoryRepository.save(category);

		// 모임 10개 저장
		List<Group> groups = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Group group = Group.builder()
				.name("mock group" + i)
				.location(location)
				.category(category)
				.capacity(100)
				.build();
			groups.add(groupRepository.save(group));
		}

		// 유저 10명 저장
		List<User> users = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			User user = User.builder().name("mock user" + i).build();
			users.add(userRepository.save(user));
		}

		// 모임과 사용자 관계 생성
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < i; j++) {
				GroupUser groupUser = GroupUser.create(groups.get(i), users.get(j), Status.MEMBER);
				groupUserRepository.save(groupUser);
			}
		}

		// when
		List<PopularGroupSummary> groupSummaries = groupQueryService.readPopularGroupsNearMe(
			location.getId(), null, null, 5);

		// then
		assertThat(groupSummaries.size()).isEqualTo(6);
		assertThat(groupSummaries).containsExactly(
			new PopularGroupSummary(
				groups.get(9).getId(),
				groups.get(9).getImgUrl(),
				groups.get(9).getName(),
				groups.get(9).getLocation().getDong(),
				groups.get(9).getCategory().getName(),
				9L
			),
			new PopularGroupSummary(
				groups.get(8).getId(),
				groups.get(8).getImgUrl(),
				groups.get(8).getName(),
				groups.get(8).getLocation().getDong(),
				groups.get(8).getCategory().getName(),
				8L
			),
			new PopularGroupSummary(
				groups.get(7).getId(),
				groups.get(7).getImgUrl(),
				groups.get(7).getName(),
				groups.get(7).getLocation().getDong(),
				groups.get(7).getCategory().getName(),
				7L
			),
			new PopularGroupSummary(
				groups.get(6).getId(),
				groups.get(6).getImgUrl(),
				groups.get(6).getName(),
				groups.get(6).getLocation().getDong(),
				groups.get(6).getCategory().getName(),
				6L
			),
			new PopularGroupSummary(
				groups.get(5).getId(),
				groups.get(5).getImgUrl(),
				groups.get(5).getName(),
				groups.get(5).getLocation().getDong(),
				groups.get(5).getCategory().getName(),
				5L
			),
			new PopularGroupSummary(
				groups.get(4).getId(),
				groups.get(4).getImgUrl(),
				groups.get(4).getName(),
				groups.get(4).getLocation().getDong(),
				groups.get(4).getCategory().getName(),
				4L
			)
		);
	}

	@Test
	@DisplayName("내 주변 인기 모임 조회: 파라미터 조건이 있는 경우")
	void readPopularGroupsNearMe3() {
		// given
		Location location = Location.builder().dong("my location").build();
		locationRepository.save(location);

		Category category = Category.builder().name("mock category").build();
		categoryRepository.save(category);

		// 모임 10개 저장
		List<Group> groups = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Group group = Group.builder()
				.name("mock group" + i)
				.location(location)
				.category(category)
				.capacity(100)
				.build();
			groups.add(groupRepository.save(group));
		}

		// 유저 10명 저장
		List<User> users = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			User user = User.builder().name("mock user" + i).build();
			users.add(userRepository.save(user));
		}

		// 모임과 사용자 관계 생성
		// 0,1: 2명, 2,3: 5명, 4,5: 1명 6,7,8,9: 0명
		for (int i = 0; i < 10; i++) {
			if (i < 2) {
				for (int j = 0; j < 2; j++) {
					groupUserRepository.save(GroupUser.create(groups.get(i), users.get(j), Status.MEMBER));
				}
				continue;
			}
			if (i < 4) {
				for (int j = 0; j < 5; j++) {
					groupUserRepository.save(GroupUser.create(groups.get(i), users.get(j), Status.MEMBER));
				}
				continue;
			}
			if (i < 6) {
				for (int j = 0; j < 1; j++) {
					groupUserRepository.save(GroupUser.create(groups.get(i), users.get(j), Status.MEMBER));
				}
			}
		}

		// when
		// 첫 번째 페이지 조회
		List<PopularGroupSummary> groupSummaries1 = groupQueryService.readPopularGroupsNearMe(
			location.getId(), null, null, 4);

		PopularGroupSummary last = groupSummaries1.get(groupSummaries1.size() - 2);

		// 두 번째 페이지 조회
		List<PopularGroupSummary> groupSummaries2 = groupQueryService.readPopularGroupsNearMe(
			location.getId(), last.groupId(), last.memberCount(), 4);

		// then
		// 2, 3, 0, 1, 4
		assertThat(groupSummaries1.size()).isEqualTo(5);
		// 4, 5, 6, 7, 8
		assertThat(groupSummaries2.size()).isEqualTo(5);
		assertThat(groupSummaries1).containsExactly(
			new PopularGroupSummary(
				groups.get(2).getId(),
				groups.get(2).getImgUrl(),
				groups.get(2).getName(),
				groups.get(2).getLocation().getDong(),
				groups.get(2).getCategory().getName(),
				5L
			),
			new PopularGroupSummary(
				groups.get(3).getId(),
				groups.get(3).getImgUrl(),
				groups.get(3).getName(),
				groups.get(3).getLocation().getDong(),
				groups.get(3).getCategory().getName(),
				5L
			),
			new PopularGroupSummary(
				groups.get(0).getId(),
				groups.get(0).getImgUrl(),
				groups.get(0).getName(),
				groups.get(0).getLocation().getDong(),
				groups.get(0).getCategory().getName(),
				2L
			),
			new PopularGroupSummary(
				groups.get(1).getId(),
				groups.get(1).getImgUrl(),
				groups.get(1).getName(),
				groups.get(1).getLocation().getDong(),
				groups.get(1).getCategory().getName(),
				2L
			),
			new PopularGroupSummary(
				groups.get(4).getId(),
				groups.get(4).getImgUrl(),
				groups.get(4).getName(),
				groups.get(4).getLocation().getDong(),
				groups.get(4).getCategory().getName(),
				1L
			)
		);
		assertThat(groupSummaries2).containsExactly(
			new PopularGroupSummary(
				groups.get(4).getId(),
				groups.get(4).getImgUrl(),
				groups.get(4).getName(),
				groups.get(4).getLocation().getDong(),
				groups.get(4).getCategory().getName(),
				1L
			),
			new PopularGroupSummary(
				groups.get(5).getId(),
				groups.get(5).getImgUrl(),
				groups.get(5).getName(),
				groups.get(5).getLocation().getDong(),
				groups.get(5).getCategory().getName(),
				1L
			),
			new PopularGroupSummary(
				groups.get(6).getId(),
				groups.get(6).getImgUrl(),
				groups.get(6).getName(),
				groups.get(6).getLocation().getDong(),
				groups.get(6).getCategory().getName(),
				0L
			),
			new PopularGroupSummary(
				groups.get(7).getId(),
				groups.get(7).getImgUrl(),
				groups.get(7).getName(),
				groups.get(7).getLocation().getDong(),
				groups.get(7).getCategory().getName(),
				0L
			),
			new PopularGroupSummary(
				groups.get(8).getId(),
				groups.get(8).getImgUrl(),
				groups.get(8).getName(),
				groups.get(8).getLocation().getDong(),
				groups.get(8).getCategory().getName(),
				0L
			)
		);
	}

	@Test
	@DisplayName("내 주변 인기 모임 조회: 모임과 사용자 관계 조회 없음")
	void readPopularGroupRelation() {
		// given
		List<Long> groupIds = List.of();
		Long userId = 1L;

		// when
		List<PopularGroupRelation> popularGroupRelations = groupQueryService.readPopularGroupRelation(groupIds, userId);

		// then
		assertThat(popularGroupRelations).isEmpty();
	}

	@Test
	@DisplayName("내 주변 인기 모임 조회: 모임과 사용자 관계 조회")
	void readPopularGroupRelation2() {
		// given
		User onlineUser = User.builder().name("current user").build();
		userRepository.save(onlineUser);

		// 모임 10개 생성
		List<Group> groups = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Group group = Group.builder().name("mock group" + i).build();
			groups.add(groupRepository.save(group));
		}

		// 모임과 사용자 현재 사용자 관계 설정
		for (int i = 0; i < 5; i++) {
			if (i < 1) {
				GroupUser groupUser = GroupUser.create(groups.get(i), onlineUser, Status.BAN);
				groupUserRepository.save(groupUser);
				continue;
			}
			if (i < 2) {
				GroupUser groupUser = GroupUser.create(groups.get(i), onlineUser, Status.OWNER);
				groupUserRepository.save(groupUser);
				continue;
			}
			GroupUser groupUser = GroupUser.create(groups.get(i), onlineUser, Status.MEMBER);
			groupUserRepository.save(groupUser);
		}

		// 모임 일정 생성
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < i + 1; j++) {
				meetingRepository.save(Meeting.meetingCreateBuilder()
					.group(groups.get(i))
					.startAt(LocalDateTime.now().plusDays(1))
					.build());
			}
		}

		// 모임 10개 id 추출
		List<Long> groupIds = groups.stream().map(Group::getId)
			.collect(Collectors.toList());

		// when
		List<PopularGroupRelation> result = groupQueryService.readPopularGroupRelation(groupIds, onlineUser.getId());

		// then
		assertThat(result).hasSize(10);
		assertThat(result).containsExactly(
			new PopularGroupRelation(groups.get(0).getId(), Status.BAN, 1L, null),
			new PopularGroupRelation(groups.get(1).getId(), Status.OWNER, 2L, null),
			new PopularGroupRelation(groups.get(2).getId(), Status.MEMBER, 3L, null),
			new PopularGroupRelation(groups.get(3).getId(), Status.MEMBER, 4L, null),
			new PopularGroupRelation(groups.get(4).getId(), Status.MEMBER,5L, null),
			new PopularGroupRelation(groups.get(5).getId(), null, 0L, null),
			new PopularGroupRelation(groups.get(6).getId(), null, 0L, null),
			new PopularGroupRelation(groups.get(7).getId(), null, 0L, null),
			new PopularGroupRelation(groups.get(8).getId(), null, 0L, null),
			new PopularGroupRelation(groups.get(9).getId(), null, 0L, null)
		);
	}

	@Test
	@DisplayName("모임 조회: 모임이 존재하지 않는 경우")
	void readGroup1() {
		// given
		Group group = Group.builder()
			.name("mock group")
			.description("mock description")
			.capacity(100)
			.category(category)
			.location(location)
			.build();
		groupRepository.save(group);
		groupQueryService.deleteGroup(group);
		// expected
		assertThatThrownBy(() -> groupQueryService.readGroupDetail(group.getId(), 1L))
			.isInstanceOf(CustomException.class)
			.hasMessage(NOT_EXISTS_GROUP.getDetail());
	}

	@Test
	@DisplayName("모임 조회: 모임이 존재하는 경우, 현재 사용자와 관계가 없는 경우, 좋아요 관계 없는 경우")
	void readGroup2() {
		// given
		Group group = Group.builder()
			.name("mock group")
			.description("mock description")
			.capacity(100)
			.category(category)
			.location(location)
			.build();
		groupRepository.save(group);

		// when
		GroupDetail groupDetail = groupQueryService.readGroupDetail(group.getId(), 1L);

		// then
		assertThat(groupDetail.groupId()).isEqualTo(group.getId());
		assertThat(groupDetail.status()).isNull();
		assertThat(groupDetail.likeStatus()).isNull();
	}

	@Test
	@DisplayName("모임 조회: 모임이 존재하는 경우, 현재 사용자와 관계가 있는 경우, 좋아요 상태")
	void readGroup3() {
		// given
		User user = User.builder().name("mock user").build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("mock group")
			.description("mock description")
			.capacity(100)
			.category(category)
			.location(location)
			.build();

		groupRepository.save(group);

		GroupUser groupUser = GroupUser.create(group, user, Status.MEMBER);
		groupUserRepository.save(groupUser);
		groupLikeRepository.save(GroupLike.create(group, user, LIKE));

		// when
		GroupDetail groupDetail = groupQueryService.readGroupDetail(group.getId(), user.getId());

		// then
		assertThat(groupDetail.groupId()).isEqualTo(group.getId());
		assertThat(groupDetail.status()).isEqualTo(Status.MEMBER);
		assertThat(groupDetail.likeStatus()).isEqualTo(GroupLikeStatus.LIKE);
		assertThat(groupDetail.capacity()).isEqualTo(100);
		assertThat(groupDetail.category()).isEqualTo(category.getName());
		assertThat(groupDetail.iconUrl()).isEqualTo(category.getIconUrl());
		assertThat(groupDetail.address()).isEqualTo(location.getDong());
	}
}
