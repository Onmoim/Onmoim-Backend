package com.onmoim.server.meeting.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 커서 토큰 인코딩/디코딩 유틸리티
 *
 * Keyset-Filtering 방식의 ScrollPosition을 클라이언트쪽에서 편하도록 커서 토큰으로 변환
 * 내부적으로는 여전히 (startAt, id) 기반의 Keyset-Filtering 사용합니다.
 * 우선 meeting도메인에서만 사용되기 때문에 common쪽으로 놓지 않았습니다.
 */

@Slf4j
@Component
public class CursorUtils {

    // 정규식 특수문자와 충돌하지 않는 구분자 사용
    private static final String DELIMITER = "###";

    /**
     * ScrollPosition을 커서 토큰으로 인코딩
     *
     * @param startAt 마지막 조회된 모임의 시작 시간
     * @param id 마지막 조회된 모임의 ID
     * @return Base64로 인코딩된 커서 토큰
     */
    public String encodeCursor(LocalDateTime startAt, Long id) {
        if (startAt == null || id == null) {
            return null;
        }

        String data = startAt.toString() + DELIMITER + id;
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    /**
     * 커서 토큰을 ScrollPosition으로 디코딩
     *
     * @param cursor 클라이언트로부터 받은 커서 토큰
     * @return Keyset-Filtering용 ScrollPosition (오류 시 첫 페이지용 keyset 반환)
     */
    public ScrollPosition decodeCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return ScrollPosition.keyset(); // 첫 번째 페이지
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            String[] parts = decoded.split(Pattern.quote(DELIMITER));

            if (parts.length != 2) {
                log.warn("잘못된 커서 형식 - 구분자 개수: {}개, 커서: {}", parts.length, cursor);
                return ScrollPosition.keyset();
            }

            LocalDateTime startAt;
            try {
                startAt = LocalDateTime.parse(parts[0]);
            } catch (DateTimeParseException e) {
                log.warn("날짜 파싱 실패: {}, 커서: {}", parts[0], cursor);
                return ScrollPosition.keyset();
            }
            
            Long id;
            try {
                id = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("ID 파싱 실패: {}, 커서: {}", parts[1], cursor);
                return ScrollPosition.keyset();
            }

            // Keyset-Filtering: Sort 순서와 일치하는 키 사용
            return ScrollPosition.forward(Map.of("startAt", startAt, "id", id));
        } catch (Exception e) {
            log.warn("커서 디코딩 실패: {}, 첫 페이지로 처리", cursor, e);
            return ScrollPosition.keyset(); // 오류 시 첫 페이지로 fallback
        }
    }
}
