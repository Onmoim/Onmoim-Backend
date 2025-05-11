package com.onmoim.server.group.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class CreateGroupRequestDTO {
	@NotNull(message = "모임 지역 설정은 필수입니다.")
	private Long locationId;

	@NotBlank(message = "모임 이름은 필수입니다.")
	private String name;

	@NotBlank(message = "모임 설명은 필수입니다.")
	private String description;

	@Min(value = 5, message = "모임 최소 정원은 5명입니다.")
	@Max(value = 300, message = "모임 최대 정원은 300명입니다.")
	private int capacity;
}
