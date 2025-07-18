package com.onmoim.server.common.s3.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URL;

import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.validator.FileValidator;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

	@Mock
	private AmazonS3 amazonS3;

	@Mock
	private FileValidator fileValidator;

	@InjectMocks
	private S3FileStorageService s3FileStorageService;

	private MockMultipartFile testFile;
	private final String bucket = "test-bucket";
	private final String testUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-file.txt";
	private final String testDomain = "https://cdn.example.com";
	private final String testFileName = "test-file.txt";

	@BeforeEach
	void setUp() throws Exception {
		// S3 버킷명 설정
		setField(s3FileStorageService, "bucket", bucket);

		// CloudFront 도메인 설정
		setField(s3FileStorageService, "domain", testDomain);

		// 테스트 파일 생성
		testFile = new MockMultipartFile(
			"file",
			testFileName,
			"text/plain",
			"테스트 파일 내용".getBytes()
		);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	@DisplayName("파일 업로드 성공 테스트 - CloudFront url 기반")
	void uploadFileSuccess() throws Exception {

		String directory = "images/test-directory";
		String keyName = directory + "/" + testFileName;
		String expectedUrl = testDomain + "/" + keyName.replaceFirst("^images/", "");

		doNothing().when(fileValidator).validate(any(MultipartFile.class));
		when(amazonS3.putObject(any())).thenReturn(new PutObjectResult());

		// generateUniqueFilename()을 고정된 값으로 설정
		S3FileStorageService spyService = spy(s3FileStorageService);
		doReturn(testFileName).when(spyService).generateUniqueFilename(anyString());

		FileUploadResponseDto result = spyService.uploadFile(testFile, "test-directory");

		assertNotNull(result);
		assertEquals("test-file.txt", result.getFileName());
		assertEquals(expectedUrl, result.getFileUrl());
		assertEquals("text/plain", result.getFileType());
		assertEquals(testFile.getSize(), result.getFileSize());

		verify(fileValidator).validate(testFile);
		verify(amazonS3).putObject(any());
	}

	@Test
	@DisplayName("디렉토리 없이 파일 업로드 성공 테스트")
	void uploadFileWithoutDirectorySuccess() throws Exception {

		String expectedUrl = testDomain + "/" + testFileName;

		doNothing().when(fileValidator).validate(any(MultipartFile.class));
		when(amazonS3.putObject(any())).thenReturn(new PutObjectResult());

		// generateUniqueFilename()을 고정된 값으로 설정
		S3FileStorageService spyService = spy(s3FileStorageService);
		doReturn(testFileName).when(spyService).generateUniqueFilename(anyString());

		FileUploadResponseDto result = spyService.uploadFile(testFile);

		assertNotNull(result);
		assertEquals(testFile.getOriginalFilename(), result.getFileName());
		assertEquals(expectedUrl, result.getFileUrl());

		verify(fileValidator).validate(testFile);
		verify(amazonS3).putObject(any());
	}

	@Test
	@DisplayName("파일 업로드 실패 테스트 - IOException")
	void uploadFileFailureIoException() throws IOException {

		doNothing().when(fileValidator).validate(any(MultipartFile.class));

		MultipartFile errorFile = mock(MultipartFile.class);
		when(errorFile.getOriginalFilename()).thenReturn("error-file.txt");
		when(errorFile.getContentType()).thenReturn("text/plain");
		when(errorFile.getSize()).thenReturn(100L);
		when(errorFile.getInputStream()).thenThrow(new IOException("테스트 예외"));


		CustomException exception = assertThrows(CustomException.class, () -> {
			s3FileStorageService.uploadFile(errorFile, "test-directory");
		});

		assertEquals(ErrorCode.FILE_UPLOAD_FAILED, exception.getErrorCode());

		verify(fileValidator).validate(errorFile);
		verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
	}

	@Test
	@DisplayName("파일 삭제 성공 테스트 - CloudFront url 기반")
	void deleteFileSuccess() throws Exception {
		String directory = "images/test-directory";
		String keyName = directory + "/" + testFileName;
		String fileUrl = testDomain + "/" + keyName.replaceFirst("^images/", "");

		doNothing().when(amazonS3).deleteObject(any(DeleteObjectRequest.class));

		assertDoesNotThrow(() -> {
			s3FileStorageService.deleteFile(fileUrl);
		});

		ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(amazonS3).deleteObject(captor.capture());

		DeleteObjectRequest deleteRequest = captor.getValue();
		assertEquals(keyName, deleteRequest.getKey());
	}

	@Test
	@DisplayName("파일 삭제 실패 테스트 - CloudFront url 기반")
	void deleteFileFailure() throws Exception {

		String directory = "images/test-directory";
		String keyName = directory + "/" + testFileName;
		String fileUrl = testDomain + "/" + keyName.replaceFirst("^images/", "");

		doThrow(new RuntimeException("삭제 실패")).when(amazonS3).deleteObject(any(DeleteObjectRequest.class));

		CustomException exception = assertThrows(CustomException.class, () -> {
			s3FileStorageService.deleteFile(fileUrl);
		});

		assertEquals(ErrorCode.FILE_DELETE_FAILED, exception.getErrorCode());
		verify(amazonS3).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("고유 파일명 생성 테스트")
	void generateUniqueFilenameTest() {

		String result1 = s3FileStorageService.generateUniqueFilename("test.jpg");
		String result2 = s3FileStorageService.generateUniqueFilename("test.jpg");

		assertNotNull(result1);
		assertNotNull(result2);
		assertNotEquals(result1, result2); // UUID를 사용하므로 두 결과는 달라야 함
		assertTrue(result1.endsWith(".jpg"));
		assertTrue(result2.endsWith(".jpg"));
	}
}
