package com.onmoim.server.common.s3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.common.s3.service.S3Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/s3")
@Tag(name = "S3", description = "S3 파일 업로드 API")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping
    @Operation(summary = "파일 업로드", description = "파일을 S3에 업로드합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 업로드 성공"),
        @ApiResponse(responseCode = "400", description = "파일 업로드 실패")
    })
    public ResponseEntity<ResponseHandler<FileUploadResponseDto>> uploadFile(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "파일이 저장될 S3 디렉토리 경로 (선택)")
            @RequestParam(value = "directory", required = false) String directory) {

        log.info("파일 업로드 요청: 파일명={}, 크기={}bytes", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        String fileUrl = s3Service.uploadFile(file, directory);

        FileUploadResponseDto responseDto = FileUploadResponseDto.builder()
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        log.info("파일 업로드 성공: URL={}", fileUrl);

        return ResponseEntity.ok(ResponseHandler.response(responseDto));
    }
}
