package com.onmoim.server.chat.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;

import lombok.RequiredArgsConstructor;

/**
 * CustomHandler를 이용하기 위핸 Configuration 입니다.
 * CustomHandler에서 소켓을 통한 예외 메시지 발송을 위해 사용되는 EventPublisher를 주입합니다.
 */
@Configuration
@RequiredArgsConstructor
public class DelegatingMessageHandlerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	protected SimpAnnotationMethodMessageHandler createAnnotationMethodMessageHandler(
		AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
		SimpMessagingTemplate brokerMessagingTemplate) {

		CustomWebSocketAnnotationMethodMessageHandler handler = new CustomWebSocketAnnotationMethodMessageHandler(
			clientInboundChannel, clientOutboundChannel, brokerMessagingTemplate, eventPublisher);

		handler.setPhase(getPhase());
		return handler;
	}

}
