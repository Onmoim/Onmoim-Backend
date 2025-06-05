package com.onmoim.server.common.kakaomap;

import java.util.ArrayList;
import java.util.List;

import com.onmoim.server.common.GeoPoint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class KAKAOMapResponse {
	private List<GeoPoint> documents = new ArrayList();
}
