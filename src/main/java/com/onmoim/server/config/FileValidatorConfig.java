package com.onmoim.server.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import com.onmoim.server.common.s3.validator.DefaultFileValidator;
import com.onmoim.server.common.s3.validator.FileValidator;

@Configuration
public class FileValidatorConfig {

    @Value("${file.upload.max-size:10MB}")
    private String maxFileSize;

    @Value("${file.upload.allowed-types:image/jpeg,image/png,image/gif,application/pdf}")
    private List<String> allowedFileTypes;

    @Bean
    public FileValidator fileValidator() {
        return new DefaultFileValidator(allowedFileTypes, DataSize.parse(maxFileSize));
    }
} 