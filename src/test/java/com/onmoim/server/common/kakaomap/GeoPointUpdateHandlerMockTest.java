package com.onmoim.server.common.kakaomap;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.service.GroupQueryService;

/**
 * 이벤트 처리 - 핸들러 테스트:{카카오 API, 재시도 시도 확인}
 */
@Transactional
@SpringBootTest
class GeoPointUpdateHandlerMockTest {
	@SpyBean
	private GeoPointUpdateHandler geoPointUpdateHandler;
	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@MockBean
	private KAKAOMapService kakaoMapService;
	@MockBean
	private KAKAOMapRetryService kakaoMapRetryService;
	@MockBean
	private GroupQueryService groupQueryService;

	GeoPointUpdateEvent geoPointUpdateEvent;

	@BeforeEach
	void setUp() {
		geoPointUpdateEvent =
			new GeoPointUpdateEvent(1L, "서울시 강남구");
	}

	@Test
	void checkTransaction(){
		assertThat(TestTransaction.isActive()).isTrue();
	}

	@Test
	@DisplayName("트랜잭션 롤백 시 이벤트 처리 X")
	void afterRollback(){
		eventPublisher.publishEvent(geoPointUpdateEvent); // 이벤트 발행
		TestTransaction.flagForRollback(); // 트랜잭션 롤백
		TestTransaction.end();             // 트랜잭션 종료

		Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(geoPointUpdateHandler, times(0)).
				handleGeoPointUpdate(any(GeoPointUpdateEvent.class));
		});
	}

	@Test
	@DisplayName("트랜잭션 커밋 시 이벤트 처리 O")
	void afterCommit(){
		eventPublisher.publishEvent(geoPointUpdateEvent); // 이벤트 발행
		TestTransaction.flagForCommit(); // 트랜잭션 커밋
		TestTransaction.end();           // 트랜잭션 종료

		Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(geoPointUpdateHandler, times(1)).
				handleGeoPointUpdate(any(GeoPointUpdateEvent.class));
		});
	}

	@Test
	@DisplayName("재시도 X: 카카오 맵 API 호출 권한 문제로 실패")
	void noRetry3(){
		when(kakaoMapService.getGeoPoint(any()))
			.thenThrow(HttpClientErrorException.Unauthorized.create(
				HttpStatus.UNAUTHORIZED, null, null, null, null));

		eventPublisher.publishEvent(geoPointUpdateEvent); // 이벤트 발행
		TestTransaction.flagForCommit(); // 트랜잭션 커밋
		TestTransaction.end();           // 트랜잭션 종료

		Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(kakaoMapRetryService, times(0)).retryUpdate(any(), any());
		});
	}

	@Test
	@DisplayName("재시도 X: 모임 업데이트 오류 존재하지 않는 모임")
	void noRetry4(){
		// given
		GeoPoint geoPoint = new GeoPoint(127.0, 37.5);
		when(kakaoMapService.getGeoPoint(any())).thenReturn(geoPoint);
		doThrow(new CustomException(ErrorCode.NOT_EXISTS_GROUP))
			.when(groupQueryService).updateGeoPoint(any(), any());

		// when
		eventPublisher.publishEvent(geoPointUpdateEvent); // 이벤트 발행
		TestTransaction.flagForCommit(); // 트랜잭션 커밋
		TestTransaction.end();           // 트랜잭션 종료

		// then
		Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(kakaoMapRetryService, times(0)).retryUpdate(any(), any());
		});
	}

	@Test
	@DisplayName("재시도 O: 카카오 API 500 응답 이후 성공")
	void retry1(){
		// given
		when(kakaoMapService.getGeoPoint(any()))
			.thenThrow(HttpClientErrorException.TooManyRequests.create(
				HttpStatus.TOO_MANY_REQUESTS, null, null, null, null));

		doNothing().when(kakaoMapRetryService).retryUpdate(any(), any());

		// when
		eventPublisher.publishEvent(geoPointUpdateEvent); // 이벤트 발행
		TestTransaction.flagForCommit(); // 트랜잭션 커밋
		TestTransaction.end();           // 트랜잭션 종료

		// then
		Awaitility.await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(kakaoMapRetryService, times(1)).retryUpdate(any(), any());
		});
	}

	@Test
	@DisplayName("재시도 O: 카카오 API 429 응답")
	void retry2(){
		// given
		when(kakaoMapService.getGeoPoint(any()))
			.thenThrow(HttpClientErrorException.TooManyRequests.create(
				HttpStatus.TOO_MANY_REQUESTS, null, null, null, null));

		doNothing().when(kakaoMapRetryService).retryUpdate(any(), any());

		// when
		eventPublisher.publishEvent(geoPointUpdateEvent); // 이벤트 발행
		TestTransaction.flagForCommit(); // 트랜잭션 커밋
		TestTransaction.end();           // 트랜잭션 종료

		// then
		Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(kakaoMapRetryService, times(1)).retryUpdate(any(), any());
		});
	}

}
