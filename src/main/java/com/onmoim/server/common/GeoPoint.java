package com.onmoim.server.common;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 위도/경도 -> x/y
 * 해당 클래스를 서비스 <-> 서비스 메시지 전달 용도, 엔티티 임베디드 타입으로 사용하는게 괜찮을까요?
 */
@Getter
@Embeddable
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@ToString
public class GeoPoint {
	private final double x;
	private final double y;

	protected GeoPoint() {
		this(0, 0);
	}
}
