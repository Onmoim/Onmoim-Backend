package com.onmoim.server.meeting.dto.request;

import java.time.LocalDateTime;

import com.onmoim.server.common.GeoPoint;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 수정 요청")
public class MeetingUpdateRequestDto {

	@NotBlank(message = "일정 제목은 필수입니다")
	@Size(max = 100, message = "일정 제목은 100자 이하여야 합니다")
	@Schema(description = "일정 제목", example = "정기 모임 - 수정된 제목")
	private String title;

	@NotNull(message = "일정 시작 시간은 필수입니다")
	@Future(message = "일정 시작 시간은 현재 시간 이후여야 합니다")
	@Schema(description = "일정 시작 시간", example = "2025-12-25T19:00:00")
	private LocalDateTime startAt;

	@NotBlank(message = "장소명은 필수입니다")
	@Size(max = 100, message = "장소명은 100자 이하여야 합니다")
	@Schema(description = "장소명", example = "스타벅스 강남점")
	private String placeName;

	@NotNull(message = "장소 좌표는 필수입니다")
	@Valid
	@Schema(description = "장소 좌표")
	private GeoPoint geoPoint;

	@NotNull(message = "정원은 필수입니다")
	@Min(value = 1, message = "정원은 1명 이상이어야 합니다")
	@Schema(description = "최대 참석 인원", example = "10")
	private Integer capacity;

	@Min(value = 0, message = "참가 비용은 0 이상이어야 합니다")
	@Schema(description = "참가 비용 (0원 = 무료)", example = "15000")
	private int cost;
}
