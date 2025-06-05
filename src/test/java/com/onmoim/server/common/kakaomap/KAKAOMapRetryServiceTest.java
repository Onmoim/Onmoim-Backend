package com.onmoim.server.common.kakaomap;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.group.implement.GroupQueryService;

@SpringBootTest
class KAKAOMapRetryServiceTest {
	@SpyBean
	private KAKAOMapRetryService retryService;
	@MockBean
	private KAKAOMapService mapService;
	@MockBean
	private GroupQueryService groupQueryService;

	@Test
	@DisplayName("바로 성공")
	void testRetry() {
		// given
		GeoPoint geoPoint = new GeoPoint(127.0, 37.5);
		when(mapService.getGeoPoint(any())).thenReturn(geoPoint);

		doNothing().when(groupQueryService).updateGeoPoint(any(), any());

		// when
		retryService.retryUpdate(1L, "");

		// then
		verify(retryService, times(1)).retryUpdate(any(), any());
	}

	@Test
	@DisplayName("2번의 시도 이후 성공")
	void testRetry2() {
		// given
		GeoPoint geoPoint = new GeoPoint(127.0, 37.5);
		when(mapService.getGeoPoint(any()))
			.thenThrow(new RuntimeException())
			.thenReturn(geoPoint);

		doNothing().doNothing()
			.when(groupQueryService).updateGeoPoint(any(), any());

		// when
		retryService.retryUpdate(1L,  "");

		// then
		verify(retryService, times(2)).retryUpdate(any(), any());
	}

	@Test
	@DisplayName("3번의 시도 성공")
	void testRetry3() {
		// given
		GeoPoint geoPoint = new GeoPoint(127.0, 37.5);
		when(mapService.getGeoPoint(any()))
			.thenThrow(new RuntimeException())
			.thenThrow(HttpClientErrorException.TooManyRequests.create(
				HttpStatus.TOO_MANY_REQUESTS, null, null, null, null))
			.thenReturn(geoPoint);

		doNothing().doNothing().doNothing()
			.when(groupQueryService).updateGeoPoint(any(), any());

		// when
		retryService.retryUpdate(1L,  "");

		// then
		verify(retryService, times(3)).retryUpdate(any(), any());
	}

	@Test
	@DisplayName("3번의 시도 이후 최종 실패 알림")
	void testRetry4() {
		// given
		when(mapService.getGeoPoint(any()))
			.thenThrow(new RuntimeException())
			.thenThrow(new RuntimeException())
			.thenThrow(HttpClientErrorException.TooManyRequests.create(
				HttpStatus.TOO_MANY_REQUESTS, null, null, null, null));

		doNothing().doNothing().doNothing()
			.when(groupQueryService).updateGeoPoint(any(), any());

		// when
		retryService.retryUpdate(1L,  "");

		// then
		verify(retryService, times(3)).retryUpdate(any(), any());
		verify(retryService, times(1)).recover(any(), any(), any());
	}
}
