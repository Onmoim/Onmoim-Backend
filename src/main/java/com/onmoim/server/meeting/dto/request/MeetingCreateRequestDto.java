package com.onmoim.server.meeting.dto.request;

import java.time.LocalDateTime;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.meeting.entity.MeetingType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 생성 요청")
public class MeetingCreateRequestDto {

	@NotNull(message = "일정 유형은 필수입니다.")
	@Schema(description = "일정 유형", example = "REGULAR")
	private MeetingType type;

	@NotBlank(message = "일정 제목은 필수입니다.")
	@Schema(description = "일정 제목", example = "정기 모임")
	private String title;

	@NotNull(message = "일정 시작 시간은 필수입니다.")
	@Schema(description = "일정 시작 시간", example = "2025-12-25T19:00:00")
	private LocalDateTime startAt;

	@NotBlank(message = "장소명은 필수입니다.")
	@Schema(description = "장소명", example = "스타벅스 강남점")
	private String placeName;

	@NotNull(message = "장소 좌표는 필수입니다.")
	@Valid
	@Schema(description = "장소 좌표")
	private GeoPoint geoPoint;

	@Min(value = 2, message = "최대 참석 인원은 2 이상이어야 합니다.")
	@Schema(description = "최대 참석 인원", example = "10")
	private int capacity;

	@Min(value = 0, message = "참가 비용은 0 이상이어야 합니다.")
	@Schema(description = "참가 비용 (0원 = 무료)", example = "15000")
	private int cost;
}
