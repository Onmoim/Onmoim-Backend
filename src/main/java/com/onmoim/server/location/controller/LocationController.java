package com.onmoim.server.location.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.location.dto.LocationResponseDto;
import com.onmoim.server.location.service.LocationQueryService;

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
@Tag(name = "Location", description = "지역 조회용 공통 API")
public class LocationController {

	private final LocationQueryService locationQueryService;

	@GetMapping("/v1/location")
	@Operation(
		summary = "지역 조회",
		description = "동/읍/면으로 지역을 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "지역 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)),
		@ApiResponse(
			responseCode = "500",
			description = "서버 내부 오류")})
	public ResponseEntity<ResponseHandler<List<LocationResponseDto>>> findLocationByDong(
		@Parameter(
			description = "동/읍/면",
			required = true
		)
		@RequestParam("dong") String dong) {
		List<LocationResponseDto> response = locationQueryService.findByDong(dong);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

}

