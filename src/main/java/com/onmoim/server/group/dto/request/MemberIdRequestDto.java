package com.onmoim.server.group.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// 모임장 양도, 모임원 강퇴
public record MemberIdRequestDto(
	@Schema(description = "회원 ID", example = "1")
	@NotNull(message = "회원 ID는 필수입니다.")
	Long memberId
) {}
