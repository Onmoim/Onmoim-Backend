package com.onmoim.server.common.s3.validator;

import java.util.List;

import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * FileValidator의 기본 구현체.
 * 크기 및 유형에 따라 파일을 검증합니다.
 */
@Slf4j
public class DefaultFileValidator implements FileValidator {

	private final DataSize maxFileSize;
	private final List<String> allowedFileTypes;

	/**
	 * DefaultFileValidator 생성자.
	 *
	 * @param allowedFileTypes 허용된 파일 MIME 타입 목록
	 * @param maxFileSize 최대 파일 크기
	 */
	public DefaultFileValidator(List<String> allowedFileTypes, DataSize maxFileSize) {
		this.allowedFileTypes = List.copyOf(allowedFileTypes);
		this.maxFileSize = maxFileSize;
		log.info("FileValidator initialized. Allowed types: {}, Max size: {}", this.allowedFileTypes, this.maxFileSize);
	}

	@Override
	public void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			log.warn("빈 파일이 감지됨");
			throw new CustomException(ErrorCode.EMPTY_FILE);
		}

		if (!isFileSizeValidInternal(file)) {
			log.warn("파일 크기 초과: {} 바이트 (최대: {} 바이트)", file.getSize(), maxFileSize.toBytes());
			throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
		}

		if (!isAllowedFileTypeInternal(file)) {
			log.error("=== 파일 타입 검증 실패 ===");
			log.error("전송된 Content-Type: '{}'", file.getContentType());
			log.error("파일명: '{}'", file.getOriginalFilename());
			log.error("허용된 타입들: {}", allowedFileTypes);
			log.error("Content-Type이 허용된 타입에 포함되는가: {}", allowedFileTypes.contains(file.getContentType()));
			throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
		}

		log.debug("파일 유효성 검사 통과: {}", file.getOriginalFilename());
	}

	private boolean isAllowedFileTypeInternal(MultipartFile file) {
		String contentType = file.getContentType();
		String fileName = file.getOriginalFilename();
		
		// 기본 Content-Type 체크
		if (contentType != null && allowedFileTypes.contains(contentType)) {
			return true;
		}
		
		// PNG 파일의 경우 추가 체크 (일부 클라이언트가 잘못된 Content-Type을 보낼 수 있음)
		if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
			log.warn("PNG 파일이지만 Content-Type이 올바르지 않음: '{}', 파일명: '{}'", contentType, fileName);
			return allowedFileTypes.contains("image/png");
		}
		
		return false;
	}

	private boolean isFileSizeValidInternal(MultipartFile file) {
		return file.getSize() <= maxFileSize.toBytes();
	}
}
