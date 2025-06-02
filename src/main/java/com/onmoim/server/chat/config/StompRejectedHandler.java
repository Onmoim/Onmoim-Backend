package com.onmoim.server.chat.config;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link RejectedExecutionHandler} 인터페이스의 커스텀 구현체로,
 * STOMP 관련 작업 실행을 위한 Inbound ThreadPoolExecutor에서 작업이 거부될 때 처리하는 용도입니다
 **/
@Slf4j
public class StompRejectedHandler implements RejectedExecutionHandler {
	@Override
	public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
		log.warn("스레드풀 작업 거부됨! 현재 poolSize={}, queueSize={}", executor.getPoolSize(), executor.getQueue().size());

		// custom metric increment
		// ...

		throw new RejectedExecutionException("STOMP executor rejected the task");
	}
}
