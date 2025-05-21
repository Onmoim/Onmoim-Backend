package com.onmoim.server.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "stomp.inbound.thread")
@Data
public class StompInboundThreadProperties {
	private String namePrefix;
	private int corePoolSize;
	private int maxPoolSize;
	private int queueCapacity;
	private int keepAliveSeconds;
}
