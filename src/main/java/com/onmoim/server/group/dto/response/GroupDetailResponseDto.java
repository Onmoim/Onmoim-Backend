package com.onmoim.server.group.dto.response;

import java.util.List;

import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.meeting.dto.MeetingDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// 모임 상세 조회
@Builder
public record GroupDetailResponseDto(
	@Schema(description = "모임 ID")
	Long groupId,
	@Schema(description = "모임 이름")
	String title,
	@Schema(description = "모임 설명")
	String description,
	@Schema(description = "모임 최대 인원")
	int capacity,
	@Schema(description = "모임 주소", example = "연남동")
	String address,
	@Schema(description = "모임 카테고리", example = "인문학/책/글")
	String category,
	@Schema(description = "모임 카테고리 아이콘 URL")
	String categoryIconUrl,
	@Schema(description = "모임 회원 수")
	Long memberCount,
	@Schema(description = "현재 사용자와 모임 관계", example = "OWNER, MEMBER, BAN, NONE")
	String status,
	@Schema(description = "현재 사용자와 모임 좋아요 상태", example = "LIKE, NONE")
	String likeStatus,
	@Schema(description = "모임 이미지 URL")
	String imageUrl,
	@Schema(description = "모임 일정")
	List<MeetingDetailResponseDto> list
)
{
	public static GroupDetailResponseDto of(
		GroupDetail groupDetail,
		Long memberCount,
		List<MeetingDetail> meetingDetails
	) {
		return GroupDetailResponseDto.builder()
			.groupId(groupDetail.groupId())
			.title(groupDetail.title())
			.description(groupDetail.description())
			.capacity(groupDetail.capacity())
			.address(groupDetail.address())
			.category(groupDetail.category())
			.categoryIconUrl(groupDetail.iconUrl())
			.memberCount(memberCount)
			.status(convertStatus(groupDetail.status()))
			.likeStatus(convertStatus(groupDetail.likeStatus()))
			.imageUrl(groupDetail.imgUrl())
			.list(meetingDetails.stream().map(MeetingDetailResponseDto::of).toList())
			.build();
	}

	private static String convertStatus(Status status) {
		if (status == null) return "NONE";
		return switch (status) {
			case OWNER, MEMBER, BAN -> status.name();
			default -> "NONE";
		};
	}

	private static String convertStatus(GroupLikeStatus status) {
		if (status == null) return "NONE";
		if(status == GroupLikeStatus.LIKE) return "LIKE";
		return "NONE";
	}
}
