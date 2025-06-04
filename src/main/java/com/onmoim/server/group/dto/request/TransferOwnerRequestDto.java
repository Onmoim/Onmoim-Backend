package com.onmoim.server.group.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TransferOwnerRequestDto(
	@Schema(description = "회원 ID", example = "1")
	@NotNull(message = "모임장 권한을 위임할 회원 ID는 필수입니다.")
	Long memberId
) {}
