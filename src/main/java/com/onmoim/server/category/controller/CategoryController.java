package com.onmoim.server.category.controller;

import java.util.List;

import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryByCategoryResponseDto;
import com.onmoim.server.group.service.GroupService;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "Category", description = "카테고리 관련 API")
public class CategoryController {

	private final CategoryQueryService categoryQueryService;
	private final GroupService groupService;

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

	/**
	 * 카테고리 - 카테고리별 모임 조회
	 */
	@GetMapping("/v1/category/{categoryId}/groups")
	@Operation(
		summary = "카테고리 - 카테고리별 모임 조회",
		description = "카테고리를 조건으로 모임을 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CommonCursorPageResponseDto.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<CommonCursorPageResponseDto<GroupSummaryByCategoryResponseDto>>> getGroupsByCategory(
		@Parameter(description = "카테고리 ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long categoryId,
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략, 이전 페이지의 nextCursorId)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		CommonCursorPageResponseDto<GroupSummaryByCategoryResponseDto> response = groupService.getGroupsByCategory(categoryId, cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

}
