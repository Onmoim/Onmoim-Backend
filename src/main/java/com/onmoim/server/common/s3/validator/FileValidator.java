package com.onmoim.server.common.s3.validator;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 유효성 검증 작업을 위한 인터페이스.
 */
public interface FileValidator {

	/**
	 * 파일을 특정 기준에 따라 검증합니다.
	 *
	 * @param file 검증할 파일
	 * @throws com.onmoim.server.common.exception.CustomException 유효성 검증 실패 시
	 */
	void validate(MultipartFile file);
}
