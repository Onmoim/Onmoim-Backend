package com.onmoim.server.group.dto.response;

import com.onmoim.server.meeting.dto.MeetingDetail;
import io.swagger.v3.oas.annotations.media.Schema;

public record MeetingDetailResponseDto (
	@Schema(description = "일정 ID")
	Long meetingId,
	@Schema(description = "일정 대표 사진")
	String imgUrl,
	@Schema(description = "일정 제목")
	String title,
	@Schema(description = "일정 타입", example = "번개모임")
	String type,
	@Schema(description = "참가 비용")
	int cost,
	@Schema(description = "최대 참석 인원")
	int capacity,
	@Schema(description = "현재 참석 인원")
	int joinCount,
	@Schema(description = "일정 시작 시간", example = "2025-06-18T20:58:30.974851")
	String startDate,
	@Schema(description = "일정 장소명")
	String placeName,
	@Schema(description = "장소 위도")
	double latitude,
	@Schema(description = "장소 경도")
	double longitude,
	@Schema(description = "현재 사용자 참석 여부")
	boolean attendance
)
{
	public static MeetingDetailResponseDto of(MeetingDetail meetingDetail) {
		return new MeetingDetailResponseDto(
			meetingDetail.meetingId(),
			meetingDetail.imgUrl(),
			meetingDetail.title(),
			meetingDetail.type(),
			meetingDetail.cost(),
			meetingDetail.capacity(),
			meetingDetail.joinCount(),
			meetingDetail.startAt().toString(),
			meetingDetail.placeName(),
			meetingDetail.location().getX(),
			meetingDetail.location().getY(),
			meetingDetail.attendance()
		);
	}
}
