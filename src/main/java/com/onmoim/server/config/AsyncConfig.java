package com.onmoim.server.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
	@Bean(name = "KakaoApiExecutor")
	public Executor kakaoApiExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);    // default 스레드 수
		executor.setMaxPoolSize(10);    // 최대 스레드 수
		executor.setQueueCapacity(100); // 대기 큐 크기
		executor.setThreadNamePrefix("KakaoApiExecutor-");
		executor.setRejectedExecutionHandler(
			(r, poolExecutor) -> {
			log.error("카카오 비동기 스레드풀 작업 거부 현재 poolSize:{}, queueSize:{}", poolExecutor.getPoolSize(), poolExecutor.getQueue().size());
		});
		executor.initialize();
		return executor;
	}
}
