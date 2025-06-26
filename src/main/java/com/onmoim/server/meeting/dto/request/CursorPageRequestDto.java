package com.onmoim.server.meeting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageRequestDto {
    private Long cursorId; // 마지막으로 조회한 일정 ID
    private int size;      // 조회할 페이지 크기

    private static final int DEFAULT_PAGE_SIZE = 10;

    public int getSize() {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }
} 