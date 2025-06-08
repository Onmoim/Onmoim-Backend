package com.onmoim.server.meeting.aop;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.repository.MeetingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Meeting 도메인 전용 Named Lock AOP
 * 특징:
 * - Group AOP와 분리된 독립적인 구조
 * - 패키지별 포인트컷으로 충돌 방지
 * - 타입별 다른 타임아웃 적용
 * - Order(0)으로 최우선 실행
 */
@Slf4j
@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class MeetingLockAspect {

    private final MeetingRepository meetingRepository;

    /**
     * Meeting 패키지 내의 @MeetingLock 어노테이션에만 적용
     * - 포인트컷을 패키지로 제한하여 Group AOP와 충돌 방지
     */
    @Around("@annotation(meetingLock) && execution(* com.onmoim.server.meeting..*(..))")
    public Object executeWithLock(ProceedingJoinPoint joinPoint, MeetingLock meetingLock) throws Throwable {
        // 1. 메서드 파라미터에서 meetingId 추출
        Long meetingId = extractMeetingId(joinPoint);
        if (meetingId == null) {
            log.warn("@MeetingLock: meetingId를 찾을 수 없습니다. 락 없이 실행합니다.");
            return joinPoint.proceed();
        }

        // 2. 일정 타입 조회 및 타임아웃 결정
        int timeoutSeconds = determineLockTimeout(meetingId);
        String lockKey = "meeting_" + meetingId;

        log.debug("Meeting Named Lock 시작 - 키: {}, 타임아웃: {}초", lockKey, timeoutSeconds);

        // 3. 락 획득 시도
        boolean lockAcquired = false;
        try {
            Integer lockResult = meetingRepository.getLock(lockKey, timeoutSeconds);
            lockAcquired = (lockResult != null && lockResult == 1);

            if (!lockAcquired) {
                log.warn("Meeting Named Lock 획득 실패 - 키: {}, 타임아웃: {}초, 결과: {}", lockKey, timeoutSeconds, lockResult);
                throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
            }

            log.debug("Meeting Named Lock 획득 성공 - 키: {}", lockKey);

            // 4. 비즈니스 로직 실행
            return joinPoint.proceed();

        } finally {
            // 5. 락 해제
            if (lockAcquired) {
                try {
                    Integer releaseResult = meetingRepository.releaseLock(lockKey);
                    log.debug("Meeting Named Lock 해제 완료 - 키: {}, 결과: {}", lockKey, releaseResult);
                } catch (Exception e) {
                    log.error("Meeting Named Lock 해제 실패 - 키: {}", lockKey, e);
                }
            }
        }
    }

    /**
     * 메서드 파라미터에서 meetingId 추출
     */
    private Long extractMeetingId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = {"meetingId", "id"};

        // 첫 번째 Long 타입 파라미터를 meetingId로 간주
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        log.warn("@MeetingLock: Long 타입의 meetingId를 찾을 수 없습니다.");
        return null;
    }

    /**
     * 일정 타입에 따른 락 타임아웃 결정
     * - 정기모임: 1초
     * - 번개모임: 3초 (경합성이 더 높을 거라 예상)
     * - 조회 실패: 2초 (기본값)
     */
    private int determineLockTimeout(Long meetingId) {
        try {
            MeetingType meetingType = meetingRepository.findMeetingTypeById(meetingId);

            if (meetingType == null) {
                log.warn("일정 타입 조회 실패 - meetingId: {}, 기본 타임아웃 적용", meetingId);
                return 2; // 기본값
            }

            switch (meetingType) {
                case REGULAR:
                    log.debug("정기모임 락 타임아웃: 1초 - meetingId: {}", meetingId);
                    return 1;
                case FLASH:
                    log.debug("번개모임 락 타임아웃: 3초 - meetingId: {}", meetingId);
                    return 3;
                default:
                    log.warn("알 수 없는 일정 타입: {} - meetingId: {}, 기본 타임아웃 적용", meetingType, meetingId);
                    return 2;
            }
        } catch (Exception e) {
            log.error("일정 타입 조회 중 오류 발생 - meetingId: {}, 기본 타임아웃 적용", meetingId, e);
            return 2;
        }
    }
}
