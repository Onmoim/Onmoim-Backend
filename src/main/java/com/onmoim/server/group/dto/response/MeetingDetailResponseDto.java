package com.onmoim.server.group.dto.response;

import java.time.format.DateTimeFormatter;

import com.onmoim.server.meeting.dto.MeetingDetail;
import io.swagger.v3.oas.annotations.media.Schema;

/*
todo: 성휜님한테 어떤 포맷이 좋은지 물어보기
시작 시간 LocalDateTime: 2023-05-23T21:01:55.790389
시작 시간 String: 06/18 14:30
 */
public record MeetingDetailResponseDto(
	@Schema(description = "일정 대표 사진")
	String imgUrl,
	@Schema(description = "일정 제목")
	String title,
	@Schema(description = "참가 비용")
	int cost,
	@Schema(description = "최대 참석 인원")
	int capacity,
	@Schema(description = "현재 참석 인원")
	int joinCount,
	@Schema(description = "모임 시작 시간")
	String startDate,
	@Schema(description = "장소명")
	String placeName,
	@Schema(description = "장소 위도")
	double latitude,
	@Schema(description = "장소 경도")
	double longitude,
	@Schema(description = "현재 사용자 참석 여부")
	boolean attendance
)
{
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");

	public static MeetingDetailResponseDto of(MeetingDetail meetingDetail) {

		return new MeetingDetailResponseDto(
			meetingDetail.imgUrl(),
			meetingDetail.title(),
			meetingDetail.cost(),
			meetingDetail.capacity(),
			meetingDetail.joinCount(),
			meetingDetail.startAt().format(DATE_TIME_FORMATTER),
			meetingDetail.placeName(),
			meetingDetail.location().getX(),
			meetingDetail.location().getY(),
			meetingDetail.attendance()
		);
	}
}
