package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "모임 카드 조회 응답")
public class GroupSummaryResponseDto {

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

	@Schema(description = "좋아요 상태", example = "LIKE, NONE")
	private String likeStatus;

	@Schema(description = "추천 상태", example = "RECOMMEND, NONE")
	private String recommendStatus;

	@Schema(description = "지역")
	private String location;

	@Schema(description = "멤버 수")
	private Long memberCount;

	@Schema(description = "다가오는 일정 수")
	private Long upcomingMeetingCount;

	public GroupSummaryResponseDto(Long groupId, String name, String imgUrl, String category,
								   Status status, String likeStatus, String recommendStatus,
								   String location, Long memberCount, Long upcomingMeetingCount) {
		this.groupId = groupId;
		this.name = name;
		this.imgUrl = imgUrl;
		this.category = category;
		this.status = status;
		this.likeStatus = likeStatus;
		this.recommendStatus = recommendStatus;
		this.location = location;
		this.memberCount = memberCount;
		this.upcomingMeetingCount = upcomingMeetingCount;
	}

}
