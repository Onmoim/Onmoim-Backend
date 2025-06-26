package com.onmoim.server.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.PostImage;

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
    private List<String> imageUrls;
    private Long likeCount;
    private Boolean isLiked;

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
                .imageUrls(List.of())
                .likeCount(0L)
                .isLiked(false)
                .build();
    }

    public static GroupPostResponseDto fromEntityWithImages(GroupPost post, List<PostImage> postImages) {
        GroupPostResponseDto dto = fromEntity(post);
        dto.imageUrls = postImages.stream()
                .filter(pi -> pi.getDeletedDate() == null)
                .map(pi -> pi.getImage().getImageUrl())
                .toList();
        return dto;
    }

    public static GroupPostResponseDto fromEntityWithLikes(GroupPost post, Long likeCount, Boolean isLiked) {
        GroupPostResponseDto dto = fromEntity(post);
        dto.likeCount = likeCount;
        dto.isLiked = isLiked;
        return dto;
    }

    public static GroupPostResponseDto fromEntityWithImagesAndLikes(
            GroupPost post, 
            List<PostImage> postImages,
            Long likeCount,
            Boolean isLiked
    ) {
        GroupPostResponseDto dto = fromEntityWithImages(post, postImages);
        dto.likeCount = likeCount;
        dto.isLiked = isLiked;
        return dto;
    }


}