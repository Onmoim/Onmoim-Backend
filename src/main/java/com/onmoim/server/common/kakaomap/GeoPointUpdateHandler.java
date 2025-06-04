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

	@Async("MapApiExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleGeoPointUpdate(GeoPointUpdateEvent event) {
		var groupId = event.groupId();
		var address = event.address();
		try {
			GeoPoint result = kakaoMapService.getGeoPoint(address);
			groupQueryService.updateGeoPoint(groupId, result);
		}
		catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e){
			log.error("4xx Unauthorized 서버 설정 문제:{}", e.getMessage());
		}
		catch (CustomException e){
			log.error("모임 업데이트 오류: 모임 ID:{}, 에러 메시지:{}", groupId, e.getMessage());
		}
		catch (Exception e) {
			kakaoMapRetryService.retryUpdate(groupId, address);
		}
	}
}
