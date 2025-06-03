package com.onmoim.server.common.kakaomap;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.service.GroupQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 카카오맵 재시도 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class KAKAOMapRetryService {
	private final KAKAOMapService kakaoMapService;
	private final GroupQueryService groupQueryService;

	@Retryable(
		retryFor = Exception.class,
		noRetryFor = CustomException.class,
		maxAttempts = 3,
		backoff = @Backoff(delay = 100, multiplier = 1.5, maxDelay = 1000, random = true)
	)
	public void retryUpdate(Long groupId, Long locationId, String address) {
		GeoPoint result = kakaoMapService.getGeoPoint(address);
		groupQueryService.updateGeoPoint(groupId, locationId, result);
	}

	@Recover
	public void recover(Exception e, Long groupId, Long locationId, String address) {
		log.warn("모임 (x,y) 업데이트 최종 실패: 모임 ID:{}, 위치 ID:{}, 전송 주소:{}", groupId, locationId, address);
	}

}
