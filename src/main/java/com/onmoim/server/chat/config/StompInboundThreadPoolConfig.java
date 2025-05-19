package com.onmoim.server.chat.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.RequiredArgsConstructor;

/**
 * Inbound의 경우 특정 트래픽 이상은 메시지를 거절합니다. 스레드풀 조절과, 한계치를 초과했을 때 대응 방법은 고도화 대상으로 분류했습니다.
 */
@Configuration
@RequiredArgsConstructor
public class StompInboundThreadPoolConfig {

	private final StompInboundThreadProperties properties;

	@Bean(name = "stompInboundExecutor")
	public ThreadPoolTaskExecutor stompInboundExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("stomp-inbound-");
		//추후 세부조정
		executor.setCorePoolSize(properties.getCorePoolSize());
		executor.setMaxPoolSize(properties.getMaxPoolSize());
		executor.setQueueCapacity(properties.getQueueCapacity());
		executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
		executor.setRejectedExecutionHandler(new StompRejectedHandler());
		executor.initialize();
		return executor;
	}
}
