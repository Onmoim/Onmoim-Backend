package com.onmoim.server.common.s3.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponseDto {
	private String fileName;
	private String fileUrl;
	private String fileType;
	private long fileSize;
}
