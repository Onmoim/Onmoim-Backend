package com.onmoim.server.category.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.category.dto.CategoryResponseDto;
import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.location.dto.LocationResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Category", description = "카테고리 조회용 공통 API")
public class CategoryController {

	private final CategoryQueryService categoryQueryService;

	@GetMapping("/v1/category")
	@Operation(
		summary = "카테고리 조회",
		description = "카테고리를 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "카테고리 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)),
		@ApiResponse(
			responseCode = "500",
			description = "서버 내부 오류")})
	public ResponseEntity<ResponseHandler<List<CategoryResponseDto>>> findAllCategories() {
		List<CategoryResponseDto> response = categoryQueryService.findAllCategories();
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

}
