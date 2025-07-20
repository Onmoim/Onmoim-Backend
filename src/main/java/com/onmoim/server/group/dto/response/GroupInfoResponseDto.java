package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.entity.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * - 내 주변 인기 모임 리스트
 * - 활동이 활발한 모임 리스트
 */
@Builder
public record GroupInfoResponseDto(
	@Schema(description = "모임 ID")
	Long groupId,
	@Schema(description = "모임 이미지")
	String imageUrl,
	@Schema(description = "모임 이름")
	String name,
	@Schema(description = "모임 회원 수")
	Long memberCount,
	@Schema(description = "모임 주소", example = "연산동")
	String dong,
	@Schema(description = "모임 카테고리", example = "인문학/책/글")
	String category,
	@Schema(description = "현재 사용자와 모임 관계", example = "OWNER, MEMBER, BOOKMARK, BAN, NONE")
	String status,
	@Schema(description = "다가오는 일정 개수")
	Long upcomingMeetingCount
)
{
	public static GroupInfoResponseDto of(
		PopularGroupSummary summary,
		PopularGroupRelation commonInfo
	)
	{
		return GroupInfoResponseDto.builder()
			.groupId(summary.groupId())
			.imageUrl(summary.imageUrl())
			.name(summary.name())
			.memberCount(summary.memberCount())
			.dong(summary.dong())
			.category(summary.category())
			.status(convertStatus(commonInfo.status()))
			.upcomingMeetingCount(commonInfo.upcomingMeetingCount())
			.build();
	}

	public static GroupInfoResponseDto of(
		ActiveGroup group,
		ActiveGroupDetail groupDetail,
		ActiveGroupRelation groupRelation
	)
	{

		return GroupInfoResponseDto.builder()
			.groupId(group.groupId())
			.imageUrl(groupDetail.imgUrl())
			.name(groupDetail.name())
			.memberCount(groupDetail.memberCount())
			.dong(groupDetail.dong())
			.category(groupDetail.category())
			.status(convertStatus(groupRelation.status()))
			.upcomingMeetingCount(group.upcomingMeetingCount())
			.build();
	}

	private static String convertStatus(Status status) {
		if (status == null) return "NONE";
		return switch (status) {
			case OWNER, MEMBER, BOOKMARK, BAN -> status.name();
			default -> "NONE";
		};
	}
}
