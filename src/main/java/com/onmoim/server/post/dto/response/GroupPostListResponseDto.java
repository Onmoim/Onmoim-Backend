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
 * 게시글 목록 조회용 평면화된 응답 DTO
 * 기존 중첩 구조를 평면화하여 depth를 줄임
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostListResponseDto {

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
    
    // 이미지 정보를 평면화 - 중첩 객체 대신 문자열 배열 사용
    private List<String> imageUrls;
    private int imageCount;

    public static GroupPostListResponseDto fromEntity(GroupPost post) {
        return GroupPostListResponseDto.builder()
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
                .imageCount(0)
                .build();
    }

    public static GroupPostListResponseDto fromEntityWithImages(GroupPost post, List<PostImage> postImages) {
        List<String> imageUrls = postImages.stream()
                .filter(pi -> pi.getDeletedDate() == null)
                .map(pi -> pi.getImage().getImageUrl())
                .toList();
                
        return GroupPostListResponseDto.builder()
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
                .imageUrls(imageUrls)
                .imageCount(imageUrls.size())
                .build();
    }
} 