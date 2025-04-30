package com.onmoim.server.common.s3.service;

import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.s3.dto.FileUploadResponseDto;

/**
 * 파일 저장소 작업을 위한 인터페이스.
 * 다양한 구현체를 허용하기 위해 저장 메커니즘을 추상화합니다.
 */
public interface FileStorageService {

	/**
	 * 파일을 저장소에 업로드합니다.
	 *
	 * @param file 업로드할 파일
	 * @param directory 파일을 저장할 디렉토리 (선택적)
	 * @return 업로드된 파일에 대한 정보를 담은 DTO
	 */
	FileUploadResponseDto uploadFile(MultipartFile file, String directory);

	/**
	 * 파일을 저장소의 기본 디렉토리에 업로드합니다.
	 *
	 * @param file 업로드할 파일
	 * @return 업로드된 파일에 대한 정보를 담은 DTO
	 */
	FileUploadResponseDto uploadFile(MultipartFile file);

	/**
	 * 저장소에서 파일을 삭제합니다.
	 * 삭제에 실패하면 CustomException을 발생시킵니다.
	 *
	 * @param fileUrl 삭제할 파일의 URL
	 */
	void deleteFile(String fileUrl);

	/**
	 * 파일에 대한 고유한 파일명을 생성합니다.
	 *
	 * @param originalFilename 원본 파일명
	 * @return 고유한 파일명
	 */
	String generateUniqueFilename(String originalFilename);
}
