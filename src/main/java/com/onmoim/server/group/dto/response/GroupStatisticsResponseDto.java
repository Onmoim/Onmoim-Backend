package com.onmoim.server.group.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// 모임 통계
public record GroupStatisticsResponseDto(
	@Schema(description = "연간 일정")
	Long annualSchedule,
	@Schema(description = "월간 일정")
	Long monthlySchedule
)
{
	public static GroupStatisticsResponseDto of(
		Long annualSchedule,
		Long monthlySchedule
	)
	{
		return new GroupStatisticsResponseDto(
			annualSchedule,
			monthlySchedule
		);
	}
}
