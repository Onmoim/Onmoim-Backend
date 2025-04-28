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

    /**
     * 파일 유형이 허용되는지 확인합니다.
     *
     * @param file 확인할 파일
     * @return 파일 유형이 허용되면 true, 그렇지 않으면 false
     */
    boolean isAllowedFileType(MultipartFile file);

    /**
     * 파일 크기가 제한 내에 있는지 확인합니다.
     *
     * @param file 확인할 파일
     * @return 파일 크기가 제한 내에 있으면 true, 그렇지 않으면 false
     */
    boolean isFileSizeValid(MultipartFile file);
}
