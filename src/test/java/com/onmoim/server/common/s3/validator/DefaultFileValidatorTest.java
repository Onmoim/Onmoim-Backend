package com.onmoim.server.common.s3.validator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;


import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class DefaultFileValidatorTest {

	@InjectMocks
	private DefaultFileValidator validator;

	private MockMultipartFile validFile;
	private MockMultipartFile emptyFile;
	private MockMultipartFile invalidTypeFile;

	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
	private static final List<String> ALLOWED_TYPES = Arrays.asList(
		"image/jpeg", "image/png", "text/plain"
	);

	@BeforeEach
	void setUp() throws Exception {
		// 유효한 파일 설정
		validFile = new MockMultipartFile(
			"file",
			"test.txt",
			"text/plain",
			"테스트 내용".getBytes()
		);

		// 빈 파일 설정
		emptyFile = new MockMultipartFile(
			"emptyFile",
			"empty.txt",
			"text/plain",
			new byte[0]
		);

		// 잘못된 유형의 파일 설정
		invalidTypeFile = new MockMultipartFile(
			"invalidTypeFile",
			"invalid.xyz",
			"application/xyz",
			"잘못된 유형".getBytes()
		);

		// Validator 필드 설정
		setField(validator, "maxFileSize", MAX_FILE_SIZE);
		setField(validator, "allowedFileTypes", ALLOWED_TYPES);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	@DisplayName("유효한 파일은 검증을 통과해야 함")
	void validateValidFile() {

		assertDoesNotThrow(() -> validator.validate(validFile));
	}

	@Test
	@DisplayName("빈 파일은 예외를 발생시켜야 함")
	void validateEmptyFile() {

		CustomException exception = assertThrows(CustomException.class, () -> {
			validator.validate(emptyFile);
		});
		assertEquals(ErrorCode.EMPTY_FILE, exception.getErrorCode());
	}

	@Test
	@DisplayName("너무 큰 파일은 예외를 발생시켜야 함")
	void validateLargeFile() {

		MockMultipartFile largeFile = spy(new MockMultipartFile(
			"largeFile",
			"large.txt",
			"text/plain",
			"대용량 파일".getBytes()
		));
		when(largeFile.getSize()).thenReturn(MAX_FILE_SIZE + 1);


		CustomException exception = assertThrows(CustomException.class, () -> {
			validator.validate(largeFile);
		});
		assertEquals(ErrorCode.FILE_SIZE_EXCEEDED, exception.getErrorCode());
	}

	@Test
	@DisplayName("잘못된 유형의 파일은 예외를 발생시켜야 함")
	void validateInvalidTypeFile() {

		CustomException exception = assertThrows(CustomException.class, () -> {
			validator.validate(invalidTypeFile);
		});
		assertEquals(ErrorCode.INVALID_FILE_TYPE, exception.getErrorCode());
	}

	@Test
	@DisplayName("파일 크기 검증")
	void isFileSizeValidTest() {

		MockMultipartFile largeFile = spy(new MockMultipartFile(
			"largeFile",
			"large.txt",
			"text/plain",
			"대용량 파일".getBytes()
		));
		when(largeFile.getSize()).thenReturn(MAX_FILE_SIZE + 1);


		assertTrue(validator.isFileSizeValid(validFile));
		assertFalse(validator.isFileSizeValid(largeFile));
	}

	@Test
	@DisplayName("파일 유형 검증")
	void isAllowedFileTypeTest() {

		assertTrue(validator.isAllowedFileType(validFile));
		assertFalse(validator.isAllowedFileType(invalidTypeFile));
	}

	@Test
	@DisplayName("허용된 파일 유형 설정")
	void setAllowedFileTypesTest() throws Exception {

		List<String> newAllowedTypes = Arrays.asList("application/pdf", "application/msword");

		validator.setAllowedFileTypes(newAllowedTypes);

		List<String> currentAllowedTypes = (List<String>) getField(validator, "allowedFileTypes");
		assertEquals(newAllowedTypes, currentAllowedTypes);
		assertFalse(validator.isAllowedFileType(validFile)); // 이제 text/plain은 허용되지 않음
	}

	@Test
	@DisplayName("최대 파일 크기 설정")
	void setMaxFileSizeTest() throws Exception {

		long newMaxSize = 1024; // 1KB

		validator.setMaxFileSize(newMaxSize);

		long currentMaxSize = (long) getField(validator, "maxFileSize");
		assertEquals(newMaxSize, currentMaxSize);
	}

	private Object getField(Object target, String fieldName) throws Exception {
		java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}
}
