package com.onmoim.server.common.kakaomap;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.onmoim.server.common.GeoPoint;

@SpringBootTest
class KAKAOMapServiceTest {
	@Autowired
	KAKAOMapService kakaoMapService;

	@Test
	@DisplayName("기본적인 카카오 맵 API 호출 테스트")
	void basicApiCall(){
		GeoPoint geoPoint = kakaoMapService.getGeoPoint("서울특별시 강남구");
		assertThat(geoPoint).isNotNull();
		assertThat(geoPoint.getX()).isGreaterThan(0.0);
		assertThat(geoPoint.getY()).isGreaterThan(0.0);
	}
}
