package com.onmoim.server.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.onmoim.server.post.entity.GroupPostType;

/**
 * 모임 게시글 생성/수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostRequestDto {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 255, message = "제목은 최대 255자까지 입력 가능합니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입력 항목입니다.")
    private String content;

    @NotNull(message = "게시글 유형은 필수 항목입니다.")
    private GroupPostType type;
}
