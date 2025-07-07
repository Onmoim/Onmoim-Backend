package com.onmoim.server.common.s3.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
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

	@Value("${cloud.aws.cloudfront.domain}")
	private String domain;

	@Override
	public FileUploadResponseDto uploadFile(MultipartFile file, String directory) {
		// 파일 유효성 검증
		fileValidator.validate(file);

		String originalFilename = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFilename)) {
			originalFilename = "unnamed-file";
		}

		// 고유한 파일명 생성
		String storedFileName = generateUniqueFilename(originalFilename);

		// 키 이름 준비 (S3 내 경로)
		String keyName = StringUtils.hasText(directory)
				? directory + "/" + storedFileName
				: storedFileName;

		log.info("keyName = {}", keyName);

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
			String pathWithoutPrefix = keyName.replaceFirst("^images/", "");
			String fileUrl = domain + "/" + pathWithoutPrefix;

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
	public void deleteFile(String fileUrl) {
		try {
			// URL에서 키 추출
			String key = extractKeyFromUrl(fileUrl);

			// 객체 삭제
			amazonS3.deleteObject(new DeleteObjectRequest(bucket, key));

			log.info("파일 삭제 성공: {}", fileUrl);
		} catch (Exception e) {
			log.error("파일 삭제 실패: {}", e.getMessage(), e);
			throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
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
	 * AWS SDK의 AmazonS3URI를 기본으로 사용하며, 실패 시 수동 파싱으로 대체합니다.
	 *
	 * @param fileUrl S3 URL
	 * @return S3에서의 키(경로)
	 */
	private String extractKeyFromUrl(String fileUrl) {
		try {
			// AWS SDK를 사용한 URL 파싱
			AmazonS3URI s3Uri = new AmazonS3URI(fileUrl);

			// URL에서 추출한 버킷이 현재 설정된 버킷과 일치하는지 확인
			if (!bucket.equals(s3Uri.getBucket())) {
				log.warn("URL의 버킷({})이 설정된 버킷({})과 일치하지 않습니다.", s3Uri.getBucket(), bucket);
			}

			return s3Uri.getKey();
		} catch (IllegalArgumentException e) {
			log.warn("AWS SDK로 URL 파싱 실패: {}, 수동 파싱 방식을 대체로 사용합니다.", fileUrl);

			// AWS SDK 파싱 실패 시 수동 파싱 방식 사용 (대체 로직)

			// 1. 가상 호스팅 스타일 URL 파싱 시도 (https://bucket-name.s3.region.amazonaws.com/key)
			String baseUrl = "https://" + bucket + ".s3." + amazonS3.getRegionName() + ".amazonaws.com/";
			if (fileUrl.startsWith(baseUrl)) {
				return fileUrl.substring(baseUrl.length());
			}

			// 2. 경로 스타일 URL 파싱 시도 (https://s3.region.amazonaws.com/bucket-name/key)
			baseUrl = "https://s3." + amazonS3.getRegionName() + ".amazonaws.com/" + bucket + "/";
			if (fileUrl.startsWith(baseUrl)) {
				return fileUrl.substring(baseUrl.length());
			}

			// 3. 그 외 URL 형식의 경우 마지막 경로 부분만 사용
			log.warn("인식 가능한 S3 URL 형식이 아닙니다. 마지막 경로 부분만 사용합니다: {}", fileUrl);
			return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
		}
	}
}
