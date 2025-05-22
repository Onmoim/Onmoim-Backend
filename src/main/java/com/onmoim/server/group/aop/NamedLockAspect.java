package com.onmoim.server.group.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.group.repository.GroupRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(2)
public class NamedLockAspect {
	private static final int LOCK_TIMEOUT_SECOND = 3;
	private final GroupRepository groupRepository;

	@Transactional
	@Around("@annotation(com.onmoim.server.group.aop.NamedLock)")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		Long groupId = (Long) joinPoint.getArgs()[0];
		try {
			groupRepository.getLock("group" + groupId, LOCK_TIMEOUT_SECOND);
			return joinPoint.proceed();
		} finally {
			groupRepository.releaseLock("group" + groupId);
		}
	}
}
