package com.onmoim.server.common.kakaomap;

import java.util.List;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.GeoPoint;
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
		noRetryFor = StopRetryException.class,
		maxAttempts = 3,
		backoff = @Backoff(delay = 100, multiplier = 1.5, maxDelay = 1000, random = true)
	)
	public void retryUpdate(Long groupId, Long locationId, String address) {
		List<GeoPoint> result = kakaoMapService.getGeoPoint(address);
		if(result.size() != 1) {
			log.error("카카오맵 API 결과 오류: 모임 ID:{}, 위치 ID:{}, 전송 주소:{}, 결과:{}", groupId, locationId, address, result);
			// 재시도 멈추기
			throw new StopRetryException();
		}
		groupQueryService.updateGeoPoint(groupId, locationId, result.get(0));
	}

	@Recover
	public void recover(Exception e, Long groupId, Long locationId, String address) {
		log.warn("모임 (x,y)업데이트 최종 실패: 모임 ID:{}, 위치 ID:{}, 전송 주소:{}", groupId, locationId, address);
	}

	private static class StopRetryException extends RuntimeException {

	}
}
