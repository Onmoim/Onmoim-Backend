package com.onmoim.server.group.dto.request;

import static com.onmoim.server.group.dto.request.GroupRequestConstraints.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreateGroupRequestDto {
	@Schema(description = "카테고리 ID", example = "1")
	@NotNull(message = "모임 카테고리 설정은 필수입니다.")
	private Long categoryId;

	@Schema(description = "지역 ID", example = "1")
	@NotNull(message = "모임 지역 설정은 필수입니다.")
	private Long locationId;

	@Schema(description = "모임 이름", example = "풋살 모임")
	@NotBlank(message = "모임 이름은 필수입니다.")
	private String name;

	@Schema(description = "모임 설명", example = "모임 설명")
	@NotBlank(message = "모임 설명은 필수입니다.")
	private String description;

	@Schema(description = "모임 최대 정원", example = "10")
	@Min(value = CREATE_MIN_CAPACITY, message = "모임 최소 정원은 5명입니다.")
	@Max(value = CREATE_MAX_CAPACITY, message = "모임 최대 정원은 300명입니다.")
	private int capacity;
}
