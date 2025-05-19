package com.onmoim.server.chat.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring 애플리케이션에서 WebSocket 메시징과 STOMP 메시지 브로커를 활성화하고
 * 커스터마이징하기 위한 설정 클래스입니다. 중요 옵션을 미리 구성했습니다.
 * <p>
 * 주요 기능은 다음과 같습니다:
 * 1. WebSocket 엔드포인트 등록
 * 2. 애플리케이션 목적지와 브로커 목적지를 포함한 STOMP 메시지 브로커 설정
 * 3. STOMP 수신 및 발신 채널에 대한 인터셉터 설정 (로깅 및 작업 실행 등)
 * 4. WebSocket 메시지 크기 제한 설정
 * 5. 고유한 세션 식별자를 부여하기 위한 커스텀 핸드셰이크 핸들러 제공
 */

@Configuration
@Slf4j
@EnableWebSocketMessageBroker

public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final ThreadPoolTaskExecutor stompInboundExecutor;
	@Value("${websocket.cors.pattern.string:}")
	private String corsPattern; //test에서는 cors=*, 프러덕션에서는 ='';

	public WebSocketConfig(@Qualifier("stompInboundExecutor") ThreadPoolTaskExecutor inboundExecutor) {
		this.stompInboundExecutor = inboundExecutor;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// prefix 기준으로 SimpleBroker로 라우팅
		// STOMP 컨벤션: /topic/..은  pub/sub(일대다)를, /queue/..는 pointToPoint(일대일)
		config.enableSimpleBroker("/topic", "/queue");

		//SendToUser()를 위한 prefix, 기본값=user, error 및 exception 발송 용도로 사용됩니다.
		config.setUserDestinationPrefix("/system");

		// 클라이언트에서 서버로 메시지를 보낼 때 "/app" 접두사 사용, @MessageMapping 에서 처리
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 웹소켓 연결 엔드포인트 등록
		registry.addEndpoint("/ws-chat")
			.setHandshakeHandler(handshakeHandler())
			.setAllowedOriginPatterns(corsPattern); //cors,
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) { // request Channel
		// 클라이언트로부터 들어오는 메시지 처리에 대한 설정
		registration.interceptors(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				log.debug(" clientInboundChannel - 수신된 메시지: {}", message);
				return message;
			}
		}).taskExecutor(stompInboundExecutor);

	}

	@Override
	public void configureClientOutboundChannel(ChannelRegistration registration) { // response Channel
		// 클라이언트로 나가는 메시지 처리에 대한 설정
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				log.debug(" clientOutboundChannel - 발송된 메시지: {}", message);
				return message;
			}
		}).taskExecutor();
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
		registry.setMessageSizeLimit(256 * 1024); // 메시지 크기제한 : 256KB까지 허용
	}

	//아하 Principal 관련 Spring Security와 통합 예정입니다.
	@Bean
	public DefaultHandshakeHandler handshakeHandler() {
		return new DefaultHandshakeHandler() {
			@Override
			protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
				Map<String, Object> attributes) {

				String sessionId = java.util.UUID.randomUUID().toString(); // 고유한 ID 부여

				log.info("Handshake 연결됨. sessionId = {}", sessionId);
				return new StompPrincipal(sessionId); // 아래에서 구현
			}
		};
	}

	public static class StompPrincipal implements Principal {
		private final String name;

		public StompPrincipal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return "StompPrincipal[name=" + name + "]";
		}
	}
}
