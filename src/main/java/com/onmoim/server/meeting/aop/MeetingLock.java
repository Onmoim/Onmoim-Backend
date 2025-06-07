package com.onmoim.server.meeting.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meeting 도메인 Named Lock 어노테이션
 * - 트랜잭션 시작 전 락 획득
 * - 트랜잭션 종료 후 락 해제
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MeetingLock {
    /**
     * 락 키 접두사 (meetingId와 조합하여 사용)
     */
    String lockPrefix() default "meeting";
    
    /**
     * 타임아웃 타입 (DYNAMIC: 일정 타입에 따라 동적 결정)
     */
    TimeoutType timeoutType() default TimeoutType.DYNAMIC;
    
    enum TimeoutType {
        DYNAMIC,  // 일정 타입에 따라 동적 결정
        FIXED     // 고정 타임아웃
    }
} 