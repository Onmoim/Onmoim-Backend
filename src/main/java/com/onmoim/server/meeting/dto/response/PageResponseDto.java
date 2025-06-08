package com.onmoim.server.meeting.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 커서 토큰 기반 페이지네이션 응답 DTO
 *
 * 클라이언트에게는 간단한 커서 토큰 인터페이스를 제공하면서,
 * 내부적으로는 Keyset-Filtering 방식의 Window를 활용하여 성능을 가져갑니다.
 */
@Getter
@Builder
@Schema(description = "페이지네이션 응답")
public class PageResponseDto<T> {

    @Schema(description = "현재 페이지 데이터 목록")
    private final List<T> content;

    @Schema(description = "다음 페이지 커서 토큰 (없으면 null)")
    private final String nextCursor;

    @Schema(description = "다음 페이지 존재 여부")
    private final boolean hasNext;

    @Schema(description = "현재 페이지 크기")
    private final int size;

    /**
     * Window로부터 PageResponseDto 생성
     *
     * @param window Spring Data JPA Window 객체
     * @param nextCursor 다음 페이지용 커서 토큰
     * @return PageResponseDto 인스턴스
     */
    public static <T> PageResponseDto<T> from(org.springframework.data.domain.Window<T> window, String nextCursor) {
        return PageResponseDto.<T>builder()
            .content(window.getContent())
            .nextCursor(window.hasNext() ? nextCursor : null)
            .hasNext(window.hasNext())
            .size(window.size())
            .build();
    }

    /**
     * 빈 페이지 응답 생성
     *
     * @return 빈 PageResponseDto 인스턴스
     */
    public static <T> PageResponseDto<T> empty() {
        return PageResponseDto.<T>builder()
            .content(List.of())
            .nextCursor(null)
            .hasNext(false)
            .size(0)
            .build();
    }
}
