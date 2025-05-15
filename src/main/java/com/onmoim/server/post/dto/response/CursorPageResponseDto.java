package com.onmoim.server.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponseDto<T> {
    private List<T> content;     // 데이터 목록
    private boolean hasNext;     // 다음 페이지 존재 여부
    private Long nextCursorId;   // 다음 페이지 요청 시 사용할 커서 ID
}
