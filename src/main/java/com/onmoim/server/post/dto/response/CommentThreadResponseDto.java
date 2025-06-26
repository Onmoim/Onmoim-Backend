package com.onmoim.server.post.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "댓글 스레드 응답 (부모댓글 + 답글 목록)")
public class CommentThreadResponseDto {

    @Schema(description = "부모 댓글")
    private CommentResponseDto parentComment;

    @Schema(description = "답글 목록")
    private List<CommentResponseDto> replies;

    @Schema(description = "다음 커서 (답글 페이지네이션)")
    private Long nextCursor;

    @Schema(description = "답글이 더 있는지 여부")
    private boolean hasMore;

    public static CommentThreadResponseDto of(
            CommentResponseDto parentComment,
            List<CommentResponseDto> replies,
            Long nextCursor,
            boolean hasMore
    ) {
        return CommentThreadResponseDto.builder()
                .parentComment(parentComment)
                .replies(replies)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
