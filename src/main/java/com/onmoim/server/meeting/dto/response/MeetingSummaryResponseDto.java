package com.onmoim.server.meeting.dto.response;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.meeting.entity.MeetingStatus;
import com.onmoim.server.meeting.entity.MeetingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(description = "일정 카드 조회 응답")
public class MeetingSummaryResponseDto {

	@Schema(description = "일정 ID", example = "1")
	private Long id;

	@Schema(description = "모임 ID", example = "1")
	private Long groupId;

	@Schema(description = "일정 유형", example = "REGULAR")
	private MeetingType type;

	@Schema(description = "일정 제목", example = "정기 모임")
	private String title;

	@Schema(description = "일정 시작 시간", example = "2025-12-25T19:00:00")
	private LocalDateTime startAt;

	@Schema(description = "장소명", example = "스타벅스 여의도브라이튼점")
	private String placeName;

//	@Schema(description = "위도")
//	private Double latitude;
//
//	@Schema(description = "경도")
//	private Double longitude;

	@Schema(description = "장소 x, y")
	private GeoPoint location;

	@Schema(description = "최대 참석 인원", example = "10")
	private int capacity;

	@Schema(description = "현재 참석 인원", example = "7")
	private int joinCount;

	@Schema(description = "참가 비용 (0원 = 무료)", example = "15000")
	private int cost;

	@Schema(description = "일정 상태", example = "OPEN")
	private MeetingStatus status;

	@Schema(description = "일정 대표 이미지", example = "https://example.com/image.jpg")
	private String imgUrl;

	@Schema(description = "현재 사용자 참석 여부(true: 참석 / false: 미참석)")
	private Boolean attendance;

	public MeetingSummaryResponseDto(Long id, Long groupId, MeetingType type, String title, LocalDateTime startAt,
									 String placeName, GeoPoint location, int capacity, int joinCount, int cost,
									 MeetingStatus status, String imgUrl, Boolean attendance) {
		this.id = id;
		this.groupId = groupId;
		this.type = type;
		this.title = title;
		this.startAt = startAt;
		this.placeName = placeName;
		this.location = location;
		this.capacity = capacity;
		this.joinCount = joinCount;
		this.cost = cost;
		this.status = status;
		this.imgUrl = imgUrl;
		this.attendance = attendance;
	}
}
