package com.onmoim.server.common.s3.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/s3")
@Tag(name = "S3", description = "S3 파일 관리 API")
public class S3Controller {

	private final FileStorageService fileStorageService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "파일 업로드",
		description = "파일을 S3에 업로드합니다. 업로드 성공 시 파일의 URL과 정보를 반환합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "파일 업로드 성공",
				content = @Content(
						mediaType = "application/json",
						schema = @Schema(implementation = ResponseHandler.class)
				)
			),
		@ApiResponse(
				responseCode = "400",
				description = "파일 업로드 실패 - 빈 파일이거나 업로드 과정에서 오류 발생"
			),
		@ApiResponse(
				responseCode = "500",
				description = "서버 오류"
			)
	})
	public ResponseEntity<ResponseHandler<FileUploadResponseDto>> uploadFile(
		@Parameter(
			description = "업로드할 파일",
			required = true,
			content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
		)
		@RequestParam("file") MultipartFile file,

		@Parameter(
			description = "파일이 저장될 S3 디렉토리 경로 (선택적, 예: 'images/profile')",
			required = false
		)
		@RequestParam(value = "directory", required = false) String directory) {

		log.info("파일 업로드 요청: 파일명={}, 크기={}bytes", file.getOriginalFilename(), file.getSize());

		FileUploadResponseDto responseDto = fileStorageService.uploadFile(file, directory);

		log.info("파일 업로드 성공: URL={}", responseDto.getFileUrl());

		return ResponseEntity.ok(ResponseHandler.response(responseDto));
	}

	@DeleteMapping
	@Operation(
		summary = "파일 삭제",
		description = "S3에서 파일을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "파일 삭제 성공"
			),
		@ApiResponse(
				responseCode = "400",
				description = "파일 삭제 실패 - 잘못된 URL 또는 삭제 과정에서 오류 발생"
			),
		@ApiResponse(
				responseCode = "500",
				description = "서버 오류"
			)
	})
	public ResponseEntity<ResponseHandler<Void>> deleteFile(
		@Parameter(
			description = "삭제할 파일의 URL",
			required = true
		)
		@RequestParam("fileUrl") String fileUrl) {

		log.info("파일 삭제 요청: URL={}", fileUrl);
		fileStorageService.deleteFile(fileUrl);
		log.info("파일 삭제 성공: URL={}", fileUrl);

		return ResponseEntity.ok(ResponseHandler.response(null));
	}
}
