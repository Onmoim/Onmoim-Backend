package com.onmoim.server.common.s3.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.validator.FileValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AWS S3를 위한 FileStorageService 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final AmazonS3 amazonS3;
    private final FileValidator fileValidator;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public FileUploadResponseDto uploadFile(MultipartFile file, String directory) {
        // 파일 유효성 검증
        fileValidator.validate(file);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed-file";
        }

        // 고유한 파일명 생성
        String storedFileName = generateUniqueFilename(originalFilename);

        // 키 이름 준비 (S3 내 경로)
        String keyName = (directory != null && !directory.isBlank())
                ? directory + "/" + storedFileName
                : storedFileName;

        try {
            // 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            // S3에 업로드
            amazonS3.putObject(new PutObjectRequest(
                    bucket,
                    keyName,
                    file.getInputStream(),
                    metadata
            ));

            // 업로드된 파일의 URL 가져오기
            String fileUrl = amazonS3.getUrl(bucket, keyName).toString();

            log.info("파일 업로드 성공: {}", fileUrl);

            // 응답 DTO 생성 및 반환
            return FileUploadResponseDto.builder()
                    .fileName(originalFilename)
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public FileUploadResponseDto uploadFile(MultipartFile file) {
        return uploadFile(file, "");
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // URL에서 키 추출
            String key = extractKeyFromUrl(fileUrl);

            // 객체 삭제
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, key));

            log.info("파일 삭제 성공: {}", fileUrl);
            return true;
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateUniqueFilename(String originalFilename) {
        // 파일 확장자 추출
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // UUID 생성 후 확장자 추가
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * URL에서 S3 키(경로)를 추출합니다.
     *
     * @param fileUrl S3 URL
     * @return S3에서의 키(경로)
     */
    private String extractKeyFromUrl(String fileUrl) {
        // 기본 URL 부분을 제거하여 키 추출
        String baseUrl = "https://" + bucket + ".s3." + amazonS3.getRegionName() + ".amazonaws.com/";
        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        }

        // 대체 형식
        baseUrl = "https://s3." + amazonS3.getRegionName() + ".amazonaws.com/" + bucket + "/";
        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        }

        // URL 형식이 인식되지 않으면 URL의 마지막 부분만 반환
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
}
