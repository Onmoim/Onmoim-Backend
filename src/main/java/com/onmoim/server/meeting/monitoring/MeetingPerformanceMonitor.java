package com.onmoim.server.meeting.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 일정 관련 성능 모니터링 AOP
 * - 타입별 락 전략 성능 추적
 * - Lock wait 시간 모니터링
 * - 정기모임 vs 번개모임 성능 비교
 */
@Slf4j
@Aspect
@Component
public class MeetingPerformanceMonitor {

	private static final long SLOW_QUERY_THRESHOLD_MS = 1000L; // 1초
	private static final long VERY_SLOW_QUERY_THRESHOLD_MS = 3000L; // 3초
	private static final long REGULAR_MEETING_THRESHOLD_MS = 2000L; // 정기모임 임계값

	/**
	 * 일정 참석/취소 관련 메서드 성능 모니터링
	 */
	@Around("execution(* com.onmoim.server.meeting.service.MeetingService.joinMeeting(..))" +
			" || execution(* com.onmoim.server.meeting.service.MeetingService.joinMeetingWithPessimisticLock(..))" +
			" || execution(* com.onmoim.server.meeting.service.MeetingService.joinMeetingWithNamedLock(..))" +
			" || execution(* com.onmoim.server.meeting.service.MeetingService.leaveMeeting(..))" +
			" || execution(* com.onmoim.server.meeting.service.MeetingService.updateMeeting(..))")
	public Object monitorMeetingOperations(ProceedingJoinPoint joinPoint) throws Throwable {
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		long startTime = System.currentTimeMillis();
		
		// 락 전략 및 일정 타입 감지
		String lockStrategy = detectLockStrategy(methodName);
		
		try {
			Object result = joinPoint.proceed();
			long executionTime = System.currentTimeMillis() - startTime;
			
			// 성능 로깅
			logPerformanceResult(methodName, lockStrategy, executionTime, args);
			
			// 락별 성능 통계
			logLockPerformanceStats(lockStrategy, executionTime);
			
			return result;
			
		} catch (Exception e) {
			long executionTime = System.currentTimeMillis() - startTime;
			logErrorResult(methodName, lockStrategy, executionTime, args, e);
			throw e;
		}
	}

	/**
	 * 성능 결과 로깅
	 */
	private void logPerformanceResult(String methodName, String lockStrategy, long executionTime, Object[] args) {
		if (executionTime > VERY_SLOW_QUERY_THRESHOLD_MS) {
			log.warn("⚠️  매우 느린 일정 작업: {}() [{}] - {}ms (args: {})", 
				methodName, lockStrategy, executionTime, formatArgs(args));
		} else if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
			log.info("🐌 느린 일정 작업: {}() [{}] - {}ms", methodName, lockStrategy, executionTime);
		} else {
			log.debug("✅ 일정 작업 완료: {}() [{}] - {}ms", methodName, lockStrategy, executionTime);
		}
		
		// 정기모임 특별 모니터링
		if (lockStrategy.equals("PESSIMISTIC") && executionTime > REGULAR_MEETING_THRESHOLD_MS) {
			log.info("📊 정기모임 처리 시간: {}ms (300명 동시 접근 가능으로 인한 지연)", executionTime);
		}
	}

	/**
	 * 에러 결과 로깅
	 */
	private void logErrorResult(String methodName, String lockStrategy, long executionTime, Object[] args, Exception e) {
		if (e.getCause() instanceof org.springframework.dao.CannotAcquireLockException ||
			e.getCause() instanceof jakarta.persistence.PessimisticLockException) {
			log.error("🔒 Lock timeout 발생: {}() [{}] - {}ms (args: {}) - {}", 
				methodName, lockStrategy, executionTime, formatArgs(args), e.getMessage());
		} else {
			log.error("❌ 일정 작업 실패: {}() [{}] - {}ms (args: {}) - {}", 
				methodName, lockStrategy, executionTime, formatArgs(args), e.getMessage());
		}
	}

	/**
	 * 메서드명으로 락 전략 감지
	 */
	private String detectLockStrategy(String methodName) {
		if (methodName.contains("WithPessimisticLock") || methodName.equals("joinMeeting")) {
			return "PESSIMISTIC"; // 정기모임용
		} else if (methodName.contains("WithNamedLock")) {
			return "NAMED_LOCK"; // 번개모임용
		} else {
			return "MIXED"; // 타입별 자동 선택
		}
	}

	/**
	 * 락별 성능 통계 로깅
	 */
	private void logLockPerformanceStats(String lockStrategy, long executionTime) {
		// 실제 운영환경에서는 Micrometer 등을 통해 메트릭 수집
		log.debug("📊 락 성능 통계: {} - {}ms", lockStrategy, executionTime);
		
		// 락 전략별 성능 분석
		if (lockStrategy.equals("PESSIMISTIC") && executionTime > 1500) {
			log.info("📈 비관적 락 성능 모니터링: {}ms (정기모임 고충돌 상황)", executionTime);
		} else if (lockStrategy.equals("NAMED_LOCK") && executionTime > 500) {
			log.info("📈 네임드 락 성능 모니터링: {}ms (번개모임 경량 처리)", executionTime);
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