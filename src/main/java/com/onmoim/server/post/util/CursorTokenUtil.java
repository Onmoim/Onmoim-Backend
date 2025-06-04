package com.onmoim.server.post.util;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 커서 기반 페이지네이션을 위한 토큰 유틸리티
 * cursor ID를 Base64로 인코딩/디코딩하여 클라이언트에 노출되는 정보를 최소화
 */
@Slf4j
@UtilityClass
public class CursorTokenUtil {

    private static final String TOKEN_PREFIX = "cursor_";
    
    /**
     * cursor ID를 토큰으로 인코딩
     * @param cursorId 커서 ID
     * @return Base64 인코딩된 토큰
     */
    public static String encodeCursorToken(Long cursorId) {
        if (cursorId == null) {
            return null;
        }
        
        try {
            String rawToken = TOKEN_PREFIX + cursorId;
            return Base64.getEncoder().encodeToString(
                rawToken.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Failed to encode cursor token for cursorId: {}", cursorId, e);
            return null;
        }
    }
    
    /**
     * 토큰을 cursor ID로 디코딩
     * @param token Base64 인코딩된 토큰
     * @return cursor ID
     */
    public static Long decodeCursorToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(token);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            
            if (!decodedString.startsWith(TOKEN_PREFIX)) {
                log.warn("Invalid token format: {}", token);
                return null;
            }
            
            String cursorIdStr = decodedString.substring(TOKEN_PREFIX.length());
            return Long.parseLong(cursorIdStr);
            
        } catch (Exception e) {
            log.error("Failed to decode cursor token: {}", token, e);
            return null;
        }
    }
    
    /**
     * 토큰이 유효한지 검증
     * @param token 검증할 토큰
     * @return 유효성 여부
     */
    public static boolean isValidToken(String token) {
        return decodeCursorToken(token) != null;
    }
} 