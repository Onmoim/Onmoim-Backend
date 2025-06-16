package com.onmoim.server.meeting.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MySQL Named Lock 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedLock {

    /**
     * 락 키를 SpEL로 지정
     * 예: "#meetingId", "'meeting_' + #meetingId"
     */
    String keySpEL() default "";

    /**
     * 락 대기 시간(초)
     * -1: 동적 타임아웃 (일정 타입별 자동 결정)
     * 양수: 명시적 타임아웃
     */
    int timeout() default -1;

    /**
     * 락 키 접두사
     * 기본값: "meeting_"
     */
    String lockPrefix() default "meeting_";
}
