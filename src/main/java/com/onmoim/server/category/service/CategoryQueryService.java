package com.onmoim.server.category.service;

import static com.onmoim.server.common.exception.ErrorCode.*;

import org.springframework.stereotype.Service;

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
}
