package com.onmoim.server.common.kakaomap;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KAKAOMapService {
	private final RestTemplate restTemplate;
	private final String AUTHORIZATION_HEADER = "Authorization";
	@Value("${map.rest.api-key}")
	private String API_KEY;
	@Value("${map.rest.uri}")
	private String REST_URI;

	public GeoPoint getGeoPoint(final String address){
		ResponseEntity<KAKAOMapResponse> response = restTemplate.exchange(
			createUri(address),
			HttpMethod.GET,
			new HttpEntity<>(getHttpHeaders()),
			KAKAOMapResponse.class);

		return validateGeoPoint(response);
	}

	private GeoPoint validateGeoPoint(ResponseEntity<KAKAOMapResponse> response){
		var kakaoMapResponse = response.getBody();
		// 응답 포멧이 바뀐 경우 아니면 전부 RestClientException
		if(kakaoMapResponse == null) {
			log.error("카카오맵 API 결과 오류: 응답 포멧 변경");
			throw new CustomException(MAP_API_ERROR);
		}
		List<GeoPoint> documents = kakaoMapResponse.getDocuments();
		// Location 데이터 문제
		if(documents.size() != 1) {
			throw new CustomException(INVALID_LOCATION);
		}
		GeoPoint geoPoint = documents.get(0);
		validateGeoPoint(geoPoint);
		return geoPoint;
	}

	// 좌표 검증 -> 카카오 오류
	private void validateGeoPoint(GeoPoint geoPoint){
		double longitude = geoPoint.getX();
		double latitude = geoPoint.getY();
		if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
			log.error("카카오맵 API 결과 오류: 경도={} 위도={}", longitude, latitude);
			throw new CustomException(MAP_API_ERROR);
		}
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
