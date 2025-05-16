package com.onmoim.server.category.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.onmoim.server.category.dto.CategoryResponseDto;
import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryQueryService {
	private final CategoryRepository categoryRepository;

	public Category getById(Long id) {
		return categoryRepository.findById(id)
			.orElseThrow(() -> new CustomException(INVALID_CATEGORY));
	}

	public List<CategoryResponseDto> findAllCategories() {
		return categoryRepository.findAll().stream()
			.map(CategoryResponseDto::from)
			.toList();
	}
}
