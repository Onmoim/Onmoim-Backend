package com.onmoim.server.post.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 목록 조회용 페이지네이션 응답 DTO
 * 기존 CursorPageResponseDto보다 더 평면화된 구조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostListPageResponseDto {

    // 게시글 목록을 최상위에 배치
    private List<GroupPostListResponseDto> posts;
    
    // 페이지네이션 정보를 별도 객체로 분리하지 않고 평면화
    private boolean hasNext;
    
    // 토큰 기반 cursor 사용 - 클라이언트는 이 값만 알면 됨
    private String nextPageToken;  // Base64 인코딩된 토큰
    private int size;
    
    public static PostListPageResponseDto of(
            List<GroupPostListResponseDto> posts, 
            boolean hasNext, 
            String nextPageToken
    ) {
        return PostListPageResponseDto.builder()
                .posts(posts)
                .hasNext(hasNext)
                .nextPageToken(nextPageToken)
                .size(posts.size())
                .build();
    }
} 