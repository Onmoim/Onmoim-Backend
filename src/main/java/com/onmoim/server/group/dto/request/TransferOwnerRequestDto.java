package com.onmoim.server.group.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferOwnerRequestDto {

	@Schema(description = "회원 ID", example = "1")
	@NotNull(message = "모임장 권한을 위임할 회원 ID는 필수입니다.")
	private Long memberId;
}
