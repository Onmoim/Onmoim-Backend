package com.onmoim.server.meeting.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 일정 관련 성능 모니터링 AOP
 * - Named Lock 통일 후 성능 추적
 * - Lock wait 시간 모니터링
 * - 타임아웃 패턴 분석
 */
@Slf4j
@Aspect
@Component
public class MeetingPerformanceMonitor {

	private static final long SLOW_QUERY_THRESHOLD_MS = 1000L; // 1초
	private static final long VERY_SLOW_QUERY_THRESHOLD_MS = 3000L; // 3초 (Named Lock 타임아웃)
	private static final long NAMED_LOCK_THRESHOLD_MS = 1500L; // Named Lock 성능 임계값

	/**
	 * 일정 참석/취소 관련 메서드 성능 모니터링
	 */
	@Around("execution(* com.onmoim.server.meeting.service.MeetingService.joinMeeting(..))" +
			" || execution(* com.onmoim.server.meeting.service.MeetingService.joinMeetingWithNamedLock(..))" +
			" || execution(* com.onmoim.server.meeting.service.MeetingService.leaveMeeting(..))" +
			" || execution(* com.onmoim.server.meeting.service.MeetingService.updateMeeting(..))")
	public Object monitorMeetingOperations(ProceedingJoinPoint joinPoint) throws Throwable {
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		long startTime = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();
			long executionTime = System.currentTimeMillis() - startTime;

			// 성능 로깅
			logPerformanceResult(methodName, executionTime, args);

			// Named Lock 성능 통계
			logNamedLockPerformanceStats(executionTime);

			return result;

		} catch (Exception e) {
			long executionTime = System.currentTimeMillis() - startTime;
			logErrorResult(methodName, executionTime, args, e);
			throw e;
		}
	}

	/**
	 * 성능 결과 로깅
	 */
	private void logPerformanceResult(String methodName, long executionTime, Object[] args) {
		if (executionTime >= VERY_SLOW_QUERY_THRESHOLD_MS) {
			log.warn("Named Lock timeout 가능성: {}() - {}ms (args: {})",
				methodName, executionTime, formatArgs(args));
		} else if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
			log.info("느린 일정 작업: {}() [NAMED_LOCK] - {}ms", methodName, executionTime);
		} else {
			log.debug("일정 작업 완료: {}() [NAMED_LOCK] - {}ms", methodName, executionTime);
		}
	}

	/**
	 * 에러 결과 로깅
	 */
	private void logErrorResult(String methodName, long executionTime, Object[] args, Exception e) {
		// Named Lock timeout 감지
		if (e.getMessage() != null && e.getMessage().contains("MEETING_LOCK_TIMEOUT")) {
			log.error("Named Lock timeout 발생: {}() - {}ms (args: {}) - {}",
				methodName, executionTime, formatArgs(args), e.getMessage());
		} else {
			log.error("일정 작업 실패: {}() [NAMED_LOCK] - {}ms (args: {}) - {}",
				methodName, executionTime, formatArgs(args), e.getMessage());
		}
	}

	/**
	 * Named Lock 성능 통계 로깅
	 */
	private void logNamedLockPerformanceStats(long executionTime) {
		// 실제 운영환경에서는 Micrometer 등을 통해 메트릭 수집
		log.debug("Named Lock 성능 통계: {}ms", executionTime);

		// Named Lock 성능 분석
		if (executionTime > NAMED_LOCK_THRESHOLD_MS) {
			log.info("Named Lock 성능 모니터링: {}ms (평균보다 느림)", executionTime);
		}

		// 타임아웃 근접 경고
		if (executionTime > 2500L) { // 3초 타임아웃 대비 83%
			log.warn("Named Lock 타임아웃 근접: {}ms (타임아웃: 3000ms)", executionTime);
		}
	}

	/**
	 * 인자 정보를 로그에 적합한 형태로 포맷팅
	 */
	private String formatArgs(Object[] args) {
		if (args == null || args.length == 0) {
			return "none";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			if (i > 0) sb.append(", ");

			Object arg = args[i];
			if (arg instanceof Long) {
				sb.append("id=").append(arg);
			} else if (arg != null) {
				sb.append(arg.getClass().getSimpleName());
			} else {
				sb.append("null");
			}
		}
		return sb.toString();
	}
}
