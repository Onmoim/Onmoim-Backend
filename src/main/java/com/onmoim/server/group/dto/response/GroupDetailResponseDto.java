package com.onmoim.server.group.dto.response;

import java.util.List;

import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.meeting.dto.MeetingDetail;

import io.swagger.v3.oas.annotations.media.Schema;

// 모임 상세 조회
public record GroupDetailResponseDto(
	@Schema(description = "모임 ID")
	Long groupId,
	@Schema(description = "모임 이름")
	String title,
	@Schema(description = "모임 설명")
	String description,
	@Schema(description = "모임 주소")
	String address,     // todo: <full address>
	@Schema(description = "모임 카테고리")
	String category,
	@Schema(description = "모임 회원 수")
	Long memberCount,
	@Schema(description = "모임 일정")
	List<MeetingDetailResponseDto> list
)
{
	public static GroupDetailResponseDto of(
		GroupDetail groupDetail,
		Long memberCount,
		List<MeetingDetail> meetingDetails
	) {
		return new GroupDetailResponseDto(
			groupDetail.groupId(),
			groupDetail.title(),
			groupDetail.description(),
			groupDetail.address(),
			groupDetail.category(),
			memberCount,
			meetingDetails.stream().map(MeetingDetailResponseDto::of).toList()
		);
	}
}
