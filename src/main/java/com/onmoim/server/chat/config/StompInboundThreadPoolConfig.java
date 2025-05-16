package com.onmoim.server.chat.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Inbound의 경우 특정 트래픽 이상은 메시지를 거절합니다. 스레드풀 조절과, 한계치를 초과했을 때 대응 방법은 고도화 대상으로 분류했습니다.
 */
@Configuration
public class StompInboundThreadPoolConfig {

	@Bean(name = "stompInboundExecutor")
	public ThreadPoolTaskExecutor stompInboundExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("stomp-inbound-");
		//추후 세부조정
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(20);
		executor.setQueueCapacity(500);
		executor.setKeepAliveSeconds(60);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.initialize();
		executor.setRejectedExecutionHandler(new StompRejectedHandler());
		return executor;
	}
}
