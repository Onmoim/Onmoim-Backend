package com.onmoim.server.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor  
@AllArgsConstructor
@Schema(description = "댓글 작성/수정 요청")
public class CommentRequestDto {

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 1000, message = "댓글은 1000자 이하로 작성해주세요")
    @Schema(description = "댓글 내용", example = "정말 좋은 게시글이네요!")
    private String content;
} 