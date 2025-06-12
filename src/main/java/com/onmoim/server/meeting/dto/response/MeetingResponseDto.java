package com.onmoim.server.meeting.dto.response;

import java.time.LocalDateTime;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingStatus;
import com.onmoim.server.meeting.entity.MeetingType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "일정 조회 응답")
public class MeetingResponseDto {

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

	@Schema(description = "장소 좌표")
	private GeoPoint geoPoint;

	@Schema(description = "최대 참석 인원", example = "10")
	private int capacity;

	@Schema(description = "현재 참석 인원", example = "7")
	private int joinCount;

	@Schema(description = "참가 비용 (0원 = 무료)", example = "15000")
	private int cost;

	@Schema(description = "일정 상태", example = "OPEN")
	private MeetingStatus status;

	@Schema(description = "작성자 ID", example = "1")
	private Long creatorId;

	@Schema(description = "일정 대표 이미지", example = "https://example.com/image.jpg")
	private String imgUrl;

	@Schema(description = "생성 시간", example = "2025-12-20T10:00:00")
	private LocalDateTime createdDate;

	public static MeetingResponseDto from(Meeting meeting) {
		return MeetingResponseDto.builder()
			.id(meeting.getId())
			.groupId(meeting.getGroupId())
			.type(meeting.getType())
			.title(meeting.getTitle())
			.startAt(meeting.getStartAt())
			.placeName(meeting.getPlaceName())
			.geoPoint(meeting.getGeoPoint())
			.capacity(meeting.getCapacity())
			.joinCount(meeting.getJoinCount())
			.cost(meeting.getCost())
			.status(meeting.getStatus())
			.creatorId(meeting.getCreatorId())
			.imgUrl(meeting.getImgUrl())
			.createdDate(meeting.getCreatedDate())
			.build();
	}
}
