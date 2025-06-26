package com.onmoim.server.post.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import com.onmoim.server.post.entity.Comment;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "댓글 응답")
public class CommentResponseDto {

    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "댓글 내용", example = "정말 좋은 게시글이네요!")
    private String content;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String authorName;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String authorProfileImg;

    @Schema(description = "답글 개수", example = "3")
    private Long replyCount;

    @Schema(description = "작성일시", example = "2024-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-01T12:30:00")
    private LocalDateTime updatedAt;

    /**
     * Comment 엔티티를 CommentResponseDto로 변환
     */
    public static CommentResponseDto from(Comment comment, Long replyCount) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthor().getName())
                .authorProfileImg(comment.getAuthor().getProfileImgUrl())
                .replyCount(replyCount)
                .createdAt(comment.getCreatedDate())
                .updatedAt(comment.getModifiedDate())
                .build();
    }

    /**
     * 답글용 변환 (답글 개수 없음)
     */
    public static CommentResponseDto fromReply(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthor().getName())
                .authorProfileImg(comment.getAuthor().getProfileImgUrl())
                .replyCount(0L) // 답글에는 답글 개수 없음
                .createdAt(comment.getCreatedDate())
                .updatedAt(comment.getModifiedDate())
                .build();
    }
} 