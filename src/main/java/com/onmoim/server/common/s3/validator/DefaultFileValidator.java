package com.onmoim.server.common.s3.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * FileValidator의 기본 구현체.
 * 크기 및 유형에 따라 파일을 검증합니다.
 */
@Slf4j
@Component
public class DefaultFileValidator implements FileValidator {

    // 기본 허용 파일 유형 (애플리케이션 속성에서 재정의 가능)
    private static final List<String> DEFAULT_ALLOWED_FILE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    // 최대 파일 크기(DataSize로 표현, 기본 10MB)
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private DataSize maxFileSize;

    @PostConstruct
    public void init() {
        log.info("최대 파일 크기 설정: {}", maxFileSize);
    }

    // 허용된 파일 유형 목록 (애플리케이션 속성에서 구성 가능)
    private List<String> allowedFileTypes = DEFAULT_ALLOWED_FILE_TYPES;

    @Override
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("빈 파일이 감지됨");
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        if (!isFileSizeValid(file)) {
            log.warn("파일 크기 초과: {} 바이트 (최대: {} 바이트)", file.getSize(), maxFileSize.toBytes());
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        if (!isAllowedFileType(file)) {
            log.warn("잘못된 파일 유형: {}", file.getContentType());
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        log.debug("파일 유효성 검사 통과: {}", file.getOriginalFilename());
    }

    @Override
    public boolean isAllowedFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && allowedFileTypes.contains(contentType);
    }

    @Override
    public boolean isFileSizeValid(MultipartFile file) {
        return file.getSize() <= maxFileSize.toBytes();
    }

    public void setAllowedFileTypes(List<String> allowedFileTypes) {
		this.allowedFileTypes = new ArrayList<>(allowedFileTypes);
    }

    /**
     * 최대 파일 크기를 설정합니다.
     *
     * @param maxFileSize 최대 파일 크기(DataSize 형식)
     */
    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
        log.info("최대 파일 크기 변경: {}", maxFileSize);
    }
}
