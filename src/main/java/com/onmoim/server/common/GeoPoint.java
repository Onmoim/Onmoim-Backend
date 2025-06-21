package com.onmoim.server.common;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

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
