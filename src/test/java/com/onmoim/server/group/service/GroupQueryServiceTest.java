package com.onmoim.server.group.service;

import static org.assertj.core.api.Assertions.*;

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
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;

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

	@BeforeEach
	void setUp() {
		location = locationRepository.save(Location.create(null, null, null, null, null));
		category = categoryRepository.save(Category.builder().name("카테고리").build());
	}

	@Test
	@DisplayName("그룹 저장 성공")
	void groupSaveSuccess() {
		// given
		Group group = Group.groupCreateBuilder()
			.capacity(10)
			.category(category)
			.location(location)
			.description("설명")
			.name("모임이름")
			.build();

		// when
		groupQueryService.saveGroup(group);

		// then
		Group findGroup = groupQueryService.getById(group.getId());
		assertThat(findGroup).isEqualTo(group);
		assertThat(findGroup.getCategory()).isEqualTo(category);
		assertThat(findGroup.getLocation()).isEqualTo(location);
	}

	@Test
	@DisplayName("그룹 저장 실패 이미 존재하는 모임 이름")
	void groupSaveFailure() {
		// given
		Group group1 = Group.groupCreateBuilder()
			.capacity(10)
			.category(category)
			.location(location)
			.description("설명1")
			.name("모임이름1")
			.build();
		groupQueryService.saveGroup(group1);

		Group group2 = Group.groupCreateBuilder()
			.capacity(10)
			.category(category)
			.location(location)
			.description("설명2")
			.name("모임이름1")
			.build();

		// expected
		assertThatThrownBy(() -> groupQueryService.saveGroup(group2))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.ALREADY_EXISTS_GROUP.getDetail());
	}
}
