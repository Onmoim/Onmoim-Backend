package com.onmoim.server.group.dto.response.cursor;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 커서 기반 응답
 */
public record CursorPageResponseDto<T, C> (
	@Schema(description = "데이터 목록")
	List<T> content,
	@Schema(description = "추가 정보")
	C extraInfo
)
{
	public static <T, C> CursorPageResponseDto<T, C> of(
		List<T> content,
		C extraInfo
	)
	{
		return new CursorPageResponseDto<>(
			content,
			extraInfo);
	}
}
