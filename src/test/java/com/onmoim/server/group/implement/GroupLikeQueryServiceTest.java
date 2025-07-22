package com.onmoim.server.group.implement;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupLike;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.repository.GroupLikeRepository;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.repository.UserCategoryRepository;
import com.onmoim.server.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class GroupLikeQueryServiceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserCategoryRepository userCategoryRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupLikeRepository groupLikeRepository;

	@Autowired
	private GroupLikeQueryService groupLikeQueryService;

	private Location location;
	private Category category;
	private User user;

	@BeforeEach
	void setUp() {
		location = locationRepository.save(Location.create("100000", "서울특별시", "강남구", "역삼동", null));
		category = categoryRepository.save(Category.create("운동/스포츠", null));

		user = userRepository.save(User.builder()
			.oauthId("1234567890")
			.provider("google")
			.email("test@test.com")
			.name("홍길동")
			.gender("F")
			.birth(LocalDateTime.now())
			.location(location)
			.profileImgUrl("https://cdn.example.com/profile/old.jpg")
			.build()
		);

		// 인증 정보 설정
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		CustomUserDetails userDetails = new CustomUserDetails(user.getId());
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("찜한 모임 조회")
	void getLikedGroups() {
		// given
		userCategoryRepository.save(UserCategory.create(user, category));

		// 2. 찜한 모임 생성 & 모임 찜하기
		Group matchedGroup = groupRepository.save(
			Group.builder()
				.name("찜한 모임")
				.category(category)
				.location(location)
				.imgUrl("https://cdn.example.com/group/matchedGroup.jpg")
				.build()
		);

		groupLikeRepository.save(GroupLike.create(matchedGroup, user, GroupLikeStatus.LIKE));

		// 3. 찜하지 않은 모임 생성
		Group unmatchedGroup = groupRepository.save(
			Group.builder()
				.name("찜하지 않은 모임")
				.category(category)
				.location(location)
				.imgUrl("https://cdn.example.com/group/unmatchedGroup.jpg")
				.build()
		);

		// when
		CommonCursorPageResponseDto<GroupSummaryResponseDto> response =
			groupLikeQueryService.getLikedGroups(null, 10);

		// then
		List<GroupSummaryResponseDto> content = response.getContent();

		List<Long> returnedGroupIds = response.getContent()
			.stream()
			.map(GroupSummaryResponseDto::getGroupId)
			.toList();

		assertThat(content).hasSize(1);
		assertThat(content.get(0).getGroupId()).isEqualTo(matchedGroup.getId());
		assertThat(content.get(0).getLikeStatus()).isEqualTo("LIKE");
		assertThat(returnedGroupIds).doesNotContain(unmatchedGroup.getId());
	}

}
