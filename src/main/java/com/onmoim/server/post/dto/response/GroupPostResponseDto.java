package com.onmoim.server.post.dto.response;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.PostImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모임 게시글 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostResponseDto {

    private Long id;
    private Long groupId;
    private Long authorId;
    private String authorName;
    private String authorProfileImage;
    private String title;
    private String content;
    private GroupPostType type;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private List<PostImageDto> images;

    public static GroupPostResponseDto fromEntity(GroupPost post) {
        return GroupPostResponseDto.builder()
                .id(post.getId())
                .groupId(post.getGroup().getId())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getName())
                .authorProfileImage(post.getAuthor().getProfileImgUrl())
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType())
                .createdDate(post.getCreatedDate())
                .modifiedDate(post.getModifiedDate())
				.images(post.getPostImages().stream()
				.filter(postImage -> !postImage.isDeleted())
				.map(PostImageDto::fromEntity)
				.toList())
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostImageDto {
        private Long id;
        private String imageUrl;

        public static PostImageDto fromEntity(PostImage postImage) {
            return PostImageDto.builder()
                    .id(postImage.getImage().getId())
                    .imageUrl(postImage.getImage().getImageUrl())
                    .build();
        }
    }
}
