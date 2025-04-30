package com.onmoim.server.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import com.onmoim.server.common.s3.validator.DefaultFileValidator;
import com.onmoim.server.common.s3.validator.FileValidator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FileValidatorConfig {

	@Value("${file.upload.max-size}")
	private String maxFileSize;

	@Value("${file.upload.allowed-types}")
	private String allowedFileTypesStr;

	@Bean
	public FileValidator fileValidator() {
		List<String> allowedFileTypes = Arrays.asList(allowedFileTypesStr.split(","));
		log.info("FileValidator 빈 생성: 허용 타입 = {}, 최대 크기 = {}", allowedFileTypes, maxFileSize);
		return new DefaultFileValidator(allowedFileTypes, DataSize.parse(maxFileSize));
	}
}
