package com.onmoim.server.config;

import static java.util.concurrent.ThreadPoolExecutor.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AsyncConfig {
	@Bean(name = "MapApiExecutor")
	public Executor kakaoApiExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(32);              // default 스레드 수
		executor.setMaxPoolSize(32);               // 최대 스레드 수
		executor.setQueueCapacity(1000000);        // 대기 큐 크기
		executor.setThreadNamePrefix("MapApiExecutor-");
		executor.setRejectedExecutionHandler(new MapPolicy());
		executor.initialize();
		executor.getThreadPoolExecutor().prestartAllCoreThreads(); // 요청 이전에 미리 스레드 생성
		return executor;
	}
	private static class MapPolicy extends CallerRunsPolicy {
		private MapPolicy() {}
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
			log.error("카카오 스레드풀 큐 용량 부족 호출 스레드가 진행: poolSize:{}, queueSize:{}", e.getPoolSize(), e.getQueue().size());
			super.rejectedExecution(r, e);
		}
	}
}