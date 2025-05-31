package com.onmoim.server.common.kakaomap;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.HttpClientErrorException;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.group.service.GroupQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 모임 생성, 모임 수정 GeoPoint(x, y) 업데이트 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeoPointUpdateHandler {

	private final KAKAOMapService kakaoMapService;
	private final KAKAOMapRetryService kakaoMapRetryService;
    private final GroupQueryService groupQueryService;

	@Async("KakaoApiExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleGeoPointUpdate(GeoPointUpdateEvent event) {
		var groupId = event.groupId();
		var address = event.address();
		var locationId = event.locationId();
		try {
			List<GeoPoint> result = kakaoMapService.getGeoPoint(address);
			// 결과가 없거나 결과가 2개 이상이라면 Location 정보에 문제
			if(result.size() != 1) {
				log.error("카카오맵 API 결과 오류: 모임 ID:{}, 위치 ID:{}, 전송 주소:{}, 결과:{}", groupId, locationId, address, result);
				return;
			}
			// x,y 업데이트: 외부 API -> 트랜잭션 시작(최소화)
			groupQueryService.updateGeoPoint(groupId, locationId, result.get(0));
		}
		catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e){
			log.error("401 Unauthorized 서버 설정 문제:{}", e.getMessage());
		}
		catch (CustomException e){
			log.error("모임 업데이트 오류: 모임 ID:{}, 에러 메시지:{}", groupId, e.getMessage());
		}
		catch (Exception e) {
			kakaoMapRetryService.retryUpdate(groupId, locationId, address);
		}
	}
}
