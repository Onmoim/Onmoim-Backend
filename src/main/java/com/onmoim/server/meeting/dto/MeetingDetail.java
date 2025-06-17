package com.onmoim.server.meeting.dto;

import java.time.LocalDateTime;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.meeting.entity.Meeting;

public record MeetingDetail(
	// 일정 대표 사진
	String imgUrl,
	// 일정 제목
	String title,
	// 참석 비용
	int cost,
	// 최대 참석 인원
	int capacity,
	// 현재 참석 인원
	int joinCount,
	// 일시
	LocalDateTime startAt,
	// 장소명
	String placeName,
	// 장소 x, y
	GeoPoint location,
	// 현재 사용자 참석 여부
	boolean attendance
)
{
	public static MeetingDetail of(
		Meeting meeting,
		boolean attendance
	)
	{
		return new MeetingDetail(
			meeting.getImgUrl(),
			meeting.getTitle(),
			meeting.getCost(),
			meeting.getCapacity(),
			meeting.getJoinCount(),
			meeting.getStartAt(),
			meeting.getPlaceName(),
			meeting.getGeoPoint(),
			attendance
		);
	}
}
