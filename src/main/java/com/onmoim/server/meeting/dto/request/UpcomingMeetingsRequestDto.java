package com.onmoim.server.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingMeetingsRequestDto {

	@Schema(description = "날짜")
	private LocalDate date;

	@Schema(description = "이번주")
	private Boolean thisWeekYn;

	@Schema(description = "이번달")
	private Boolean thisMonthYn;

	@Schema(description = "내가 참석한")
	private Boolean joinedYn;

	@Schema(description = "정기모임")
	private Boolean regularYn;

	@Schema(description = "번개모임")
	private Boolean flashYn;

}
