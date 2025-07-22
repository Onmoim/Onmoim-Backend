package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "가입한 모임 조회 응답")
public class JoinedGroupResponseDto {

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

	@Schema(description = "좋아요 상태")
	private GroupLikeStatus groupLikeStatus;

	@Schema(description = "지역")
	private String location;

	@Schema(description = "멤버 수")
	private Long memberCount;

	@Schema(description = "다가오는 일정 수")
	private Long upcomingMeetingCount;

	public JoinedGroupResponseDto(Long groupId, String name, String imgUrl, String category,
								  Status status, GroupLikeStatus groupLikeStatus, String location, Long memberCount, Long upcomingMeetingCount) {
		this.groupId = groupId;
		this.name = name;
		this.imgUrl = imgUrl;
		this.category = category;
		this.status = status;
		this.groupLikeStatus = groupLikeStatus;
		this.location = location;
		this.memberCount = memberCount;
		this.upcomingMeetingCount = upcomingMeetingCount;
	}

}
