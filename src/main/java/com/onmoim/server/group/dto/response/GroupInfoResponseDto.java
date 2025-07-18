package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.entity.Status;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * - 내 주변 인기 모임 리스트
 * - 활동이 활발한 모임 리스트
 */
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
	@Schema(description = "다가 오는 일정 개수")
	Long upcomingMeetingCount
)
{
	public static GroupInfoResponseDto of(
		PopularGroupSummary summary,
		PopularGroupRelation commonInfo
	)
	{
		return new GroupInfoResponseDto(
			summary.groupId(),
			summary.imageUrl(),
			summary.name(),
			summary.memberCount(),
			summary.dong(),
			summary.category(),
			convertStatus(commonInfo.status()),
			commonInfo.upcomingMeetingCount()
		);
	}

	public static GroupInfoResponseDto of(
		ActiveGroup group,
		ActiveGroupDetail groupDetail,
		ActiveGroupRelation groupRelation
	)
	{
		return new GroupInfoResponseDto(
			group.groupId(),
			groupDetail.imgUrl(),
			groupDetail.name(),
			groupDetail.memberCount(),
			groupDetail.dong(),
			groupDetail.category(),
			convertStatus(groupRelation.status()),
			group.upcomingMeetingCount()
		);
	}

	private static String convertStatus(String status) {
		if(status == null || status.equals(Status.PENDING.name())){
			return "NONE";
		}
		return status;
	}
}
