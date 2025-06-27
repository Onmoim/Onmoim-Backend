package com.onmoim.server.group.aop;

import static com.onmoim.server.common.exception.ErrorCode.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.onmoim.server.common.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(1)
@Deprecated
public class RetryAspect {
	private static final int RETRY_COUNT = 5;
	private static final long RETRY_DELAY_MS = 100L;

	@Around("@annotation(com.onmoim.server.group.aop.Retry)")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		int retryCount = RETRY_COUNT;
		while (retryCount > 0) {
			try {
				return joinPoint.proceed();
			}
			catch (OptimisticLockingFailureException | CustomException e) {
				if(e instanceof CustomException ce && ce.getErrorCode() != TOO_MANY_REQUEST){
					throw e;
				}
				if (--retryCount > 0) Thread.sleep(RETRY_DELAY_MS);
			}
		}
		log.warn("재시도 실패");
		throw new CustomException(TOO_MANY_REQUEST);
	}
}
