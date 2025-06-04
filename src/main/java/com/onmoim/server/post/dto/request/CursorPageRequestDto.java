package com.onmoim.server.post.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.onmoim.server.post.util.CursorTokenUtil;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageRequestDto {
    private String pageToken;  // 클라이언트가 보내는 토큰
    private int size;          // 조회할 페이지 크기

    private static final int DEFAULT_PAGE_SIZE = 10;

    public int getSize() {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }
    
    /**
     * 토큰을 cursor ID로 변환
     * @return cursor ID (첫 페이지의 경우 null)
     */
    public Long getCursorId() {
        return CursorTokenUtil.decodeCursorToken(pageToken);
    }
    
    /**
     * 호환성을 위한 기존 방식 지원
     * @param cursorId cursor ID
     * @param size 페이지 크기
     * @return CursorPageRequestDto
     */
    public static CursorPageRequestDto ofCursorId(Long cursorId, int size) {
        return CursorPageRequestDto.builder()
                .pageToken(CursorTokenUtil.encodeCursorToken(cursorId))
                .size(size)
                .build();
    }
}
