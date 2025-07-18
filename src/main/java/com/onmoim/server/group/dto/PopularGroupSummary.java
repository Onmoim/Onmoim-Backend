package com.onmoim.server.group.dto;

import java.util.Comparator;
import java.util.List;

/**
 * @param groupId - 모임 id
 * @param imageUrl - 모임 이미지
 * @param name - 모임 이름
 * @param memberCount - 모임 인원
 * @param dong - 모임 지역
 * @param category - 모임 카테고리
 */
public record PopularGroupSummary (
	Long groupId,
	String imageUrl,
	String name,
	Long memberCount,
	String dong,
	String category
)
{
	private static PopularGroupSummary EMPTY_CURSOR = new PopularGroupSummary(
		0L, null, null, 0L, null, null);

	/**
	 * 회원 수 DESC, 모임 ID ASC (커서 추출)
	 */
	public static PopularGroupSummary extractCursor(List<PopularGroupSummary> list) {
		if (list.isEmpty()) return EMPTY_CURSOR;

		long memberCountMin = list.stream()
			.mapToLong(PopularGroupSummary::memberCount)
			.min().orElseThrow();

		return list.stream()
			.filter(gm -> gm.memberCount == memberCountMin)
			.max(Comparator.comparing(PopularGroupSummary::groupId))
			.orElse(EMPTY_CURSOR);
	}
}
