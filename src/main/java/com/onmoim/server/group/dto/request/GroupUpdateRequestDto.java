package com.onmoim.server.group.dto.request;

import static com.onmoim.server.group.dto.request.GroupRequestConstraints.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

// 모임 수정 요청
@Builder
public record GroupUpdateRequestDto (
	@Schema(description = "모임 설명", example = "모임 설명")
	@NotBlank(message = "모임 설명은 필수입니다.")
	String description,

	@Schema(description = "모임 최대 정원", example = "10")
	@Size(min = CREATE_MIN_CAPACITY, max = CREATE_MAX_CAPACITY, message = "모임 정원은 5명 이상 300명 이하로 설정해주세요.")
	int capacity
) {}
