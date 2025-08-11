package com.onmoim.server.chat.domain.dto;

import com.onmoim.server.group.entity.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "채팅방 목록")
public class ChatRoomSummeryDto {

	@Schema(description = "모임 ID")
	private Long groupId;

	@Schema(description = "모임 이름")
	private String name;

	@Schema(description = "모임 대표 사진")
	private String imgUrl;

	@Schema(description = "카테고리")
	private String category;

	@Schema(description = "모임 상태(OWNER: 운영중, MEMBER: 가입중)")
	private Status status;

	@Schema(description = "찜 상태", example = "LIKE, NONE")
	private String likeStatus;

	@Schema(description = "마지막메시지")
	private ChatMessageDto message;

	@Schema(description = "추천 상태", example = "RECOMMEND, NONE")
	private String recommendStatus;

	@Schema(description = "지역")
	private String location;

	@Schema(description = "멤버 수")
	private Long memberCount;

	@Schema(description = "다가오는 일정 수")
	private Long upcomingMeetingCount;

	public ChatRoomSummeryDto(Long groupId, String name, String imgUrl, String category,
		Status status, String likeStatus,
		ChatMessageDto message,
		String location, Long memberCount, Long upcomingMeetingCount) {
		this.groupId = groupId;
		this.name = name;
		this.imgUrl = imgUrl;
		this.category = category;
		this.status = status;
		this.likeStatus = likeStatus;
		this.message = message;
		this.location = location;
		this.memberCount = memberCount;
		this.upcomingMeetingCount = upcomingMeetingCount;
	}

}
