package com.onmoim.server.group.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;
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
}
