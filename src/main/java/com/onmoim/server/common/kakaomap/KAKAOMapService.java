package com.onmoim.server.common.kakaomap;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.onmoim.server.common.GeoPoint;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KAKAOMapService {
	private final RestTemplate restTemplate;
	private final String AUTHORIZATION_HEADER = "Authorization";
	@Value("${map.rest.api-key}")
	private String API_KEY;
	@Value("${map.rest.uri}")
	private String REST_URI;

	/**
	 * 로직 흐름: Location(.csv 기반) 조회 -> Location 정보 바탕으로 카카오 맵 조회 -> 정보 저장
	 * 200 성공 응답
	 * - 1개: success
	 * - 2개: 중복 이름 -> 어떻게 처리? (csv 파일 문제)
	 * - 0개: 존재하지 않는 장소 -> 어떻게 처리? (csv 파일 문제)
	 * 401 실패 응답
	 * - 서버 설정 문제: error log 남기기, 재시도 X
	 * 500 실패 응답(실제로 연속적으로 호출하면 발생)
	 * - 카카오 MAP API 응답 오류 -> 재시도
	 * 나머지 Exception -> 재시도
	 */
	public GeoPoint getGeoPoint(final String address){
		ResponseEntity<KAKAOMapResponse> response = restTemplate.exchange(
			createUri(address),
			HttpMethod.GET,
			new HttpEntity<>(getHttpHeaders()),
			KAKAOMapResponse.class);

		KAKAOMapResponse kakaoMapResponse = response.getBody();
		return kakaoMapResponse.getDocuments().get(0);
	}

	private URI createUri(final String address){
		return UriComponentsBuilder.fromHttpUrl(REST_URI)
			.queryParam("query", address)
			.build()
			.encode()
			.toUri();
	}

	private HttpHeaders getHttpHeaders(){
		HttpHeaders headers = new HttpHeaders();
		headers.set(AUTHORIZATION_HEADER, API_KEY);
		return headers;
	}
}
