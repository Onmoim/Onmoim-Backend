package com.onmoim.server.common.s3.validator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class DefaultFileValidatorTest {

	private DefaultFileValidator validator;

	private MockMultipartFile validFile;
	private MockMultipartFile emptyFile;
	private MockMultipartFile invalidTypeFile;

	private static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(5); // 5MB
	private static final List<String> ALLOWED_TYPES = Arrays.asList(
		"image/jpeg", "image/png", "text/plain"
	);

	@BeforeEach
	void setUp() {
		validator = new DefaultFileValidator(ALLOWED_TYPES, MAX_FILE_SIZE);

		validFile = new MockMultipartFile(
			"file",
			"test.txt",
			"text/plain",
			"테스트 내용".getBytes()
		);

		emptyFile = new MockMultipartFile(
			"emptyFile",
			"empty.txt",
			"text/plain",
			new byte[0]
		);

		invalidTypeFile = new MockMultipartFile(
			"invalidTypeFile",
			"invalid.xyz",
			"application/xyz",
			"잘못된 유형".getBytes()
		);
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
		when(largeFile.getSize()).thenReturn(MAX_FILE_SIZE.toBytes() + 1);

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
}
