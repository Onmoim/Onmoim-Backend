package com.onmoim.server.group.dto.request;

import static com.onmoim.server.group.dto.request.GroupRequestConstraints.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

// 모임 수정 요청
@Builder
public record GroupUpdateRequestDto (
	@Schema(description = "모임 설명", example = "모임 설명")
	@NotBlank(message = "모임 설명은 필수입니다.")
	String description,

	@Schema(description = "모임 최대 정원", example = "10")
	@Min(value = CREATE_MIN_CAPACITY, message = "모임 최대 정원은 5명 이상이어야 합니다.")
	@Max(value = CREATE_MAX_CAPACITY, message = "모임 최대 정원은 300명 이하이어야 합니다.")
	int capacity
) {}
