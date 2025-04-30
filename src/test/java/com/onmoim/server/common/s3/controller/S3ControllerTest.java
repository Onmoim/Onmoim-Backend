package com.onmoim.server.common.s3.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;

@WebMvcTest(S3Controller.class)
class S3ControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FileStorageService fileStorageService;

	private MockMultipartFile testFile;
	private FileUploadResponseDto responseDto;

	@BeforeEach
	void setUp() {

		testFile = new MockMultipartFile(
				"file",
				"test-file.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"테스트 파일 내용".getBytes()
		);

		responseDto = FileUploadResponseDto.builder()
				.fileName("test-file.txt")
				.fileUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-file.txt")
				.fileType(MediaType.TEXT_PLAIN_VALUE)
				.fileSize(testFile.getSize())
				.build();
	}

	@Test
	@DisplayName("파일 업로드 성공 테스트")
	void uploadFileSuccess() throws Exception {

		given(fileStorageService.uploadFile(any(), anyString())).willReturn(responseDto);

		mockMvc.perform(multipart("/api/v1/s3")
				.file(testFile)
				.param("directory", "test-directory"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("SUCCESS"))
				.andExpect(jsonPath("$.data.fileName").value("test-file.txt"))
				.andExpect(jsonPath("$.data.fileUrl").exists())
				.andExpect(jsonPath("$.data.fileType").value(MediaType.TEXT_PLAIN_VALUE))
				.andExpect(jsonPath("$.data.fileSize").exists());

		verify(fileStorageService).uploadFile(any(), eq("test-directory"));
	}

	@Test
	@DisplayName("디렉토리 없이 파일 업로드 성공 테스트")
	void uploadFileWithoutDirectorySuccess() throws Exception {

		given(fileStorageService.uploadFile(any(), isNull())).willReturn(responseDto);

		mockMvc.perform(multipart("/api/v1/s3")
				.file(testFile))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("SUCCESS"))
				.andExpect(jsonPath("$.data.fileName").value("test-file.txt"));

		verify(fileStorageService).uploadFile(any(), isNull());
	}

	@Test
	@DisplayName("파일 업로드 실패 테스트 - 빈 파일")
	void uploadFileFailureEmptyFile() throws Exception {

		given(fileStorageService.uploadFile(any(), anyString()))
				.willThrow(new CustomException(ErrorCode.EMPTY_FILE));

		mockMvc.perform(multipart("/api/v1/s3")
				.file(testFile)
				.param("directory", "test-directory"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("EMPTY_FILE"));
	}

	@Test
	@DisplayName("파일 삭제 성공 테스트")
	void deleteFileSuccess() throws Exception {

		String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-file.txt";
		willDoNothing().given(fileStorageService).deleteFile(fileUrl);

		mockMvc.perform(delete("/api/v1/s3")
				.param("fileUrl", fileUrl))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("SUCCESS"))
				.andExpect(jsonPath("$.data").isEmpty());

		verify(fileStorageService).deleteFile(fileUrl);
	}

	@Test
	@DisplayName("파일 삭제 실패 테스트")
	void deleteFileFailure() throws Exception {

		String fileUrl = "https://invalid-url.com/file.txt";
		willThrow(new CustomException(ErrorCode.FILE_DELETE_FAILED))
			.given(fileStorageService).deleteFile(fileUrl);

		mockMvc.perform(delete("/api/v1/s3")
				.param("fileUrl", fileUrl))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("FILE_DELETE_FAILED"));

		verify(fileStorageService).deleteFile(fileUrl);
	}
}
