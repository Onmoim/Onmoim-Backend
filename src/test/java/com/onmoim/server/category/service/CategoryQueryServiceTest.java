package com.onmoim.server.category.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.onmoim.server.category.dto.CategoryResponseDto;
import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;

@SpringBootTest
public class CategoryQueryServiceTest {

	@MockBean
	private CategoryRepository categoryRepository;

	@Autowired
	private CategoryQueryService categoryQueryService;

	@Test
	@DisplayName("전체 카테고리 조회 성공")
	void findAllCategoriesSuccess() {
		// given
		Category category1 = Category.create("운동/스포츠", null);
		Category category2 = Category.create("음악/악기", null);
		List<Category> categories = List.of(category1, category2);

		when(categoryRepository.findAll()).thenReturn(categories);

		// when
		List<CategoryResponseDto> result = categoryQueryService.findAllCategories();

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("운동/스포츠");
		assertThat(result.get(1).getName()).isEqualTo("음악/악기");

		verify(categoryRepository).findAll();
	}

}
