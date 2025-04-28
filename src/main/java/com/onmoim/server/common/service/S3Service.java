package com.onmoim.server.common.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에 파일을 업로드합니다.
     * @param file 업로드할 파일
     * @param dirName 파일이 저장될 S3 디렉토리 이름 (선택적)
     * @return 업로드된 파일의 URL
     */
    public String uploadFile(MultipartFile file, String dirName) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed-file";
        }

        // 파일 확장자 확인
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        
        // 고유한 파일명 생성
        String storedFileName = UUID.randomUUID().toString() + ext;
        
        // 디렉토리 이름이 제공된 경우, 디렉토리 경로를 추가
        String keyName = (dirName != null && !dirName.isBlank()) 
                        ? dirName + "/" + storedFileName 
                        : storedFileName;

        try {
            // 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            // S3에 파일 업로드
            amazonS3.putObject(new PutObjectRequest(
                    bucket,
                    keyName,
                    file.getInputStream(),
                    metadata
            ));

            // 업로드된 파일의 URL 반환
            return amazonS3.getUrl(bucket, keyName).toString();
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3에 파일을 업로드합니다. (기본 디렉토리 사용)
     * @param file 업로드할 파일
     * @return 업로드된 파일의 URL
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, "");
    }
} 