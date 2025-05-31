package com.onmoim.server.common.kakaomap;

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

	public List<GeoPoint> getGeoPoint(final String address){
		ResponseEntity<KAKAOMapResponse> response = restTemplate.exchange(
			createUri(address),
			HttpMethod.GET,
			new HttpEntity<>(getHttpHeaders()),
			KAKAOMapResponse.class);

		KAKAOMapResponse kakaoMapResponse = response.getBody();
		return kakaoMapResponse.getDocuments();
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
