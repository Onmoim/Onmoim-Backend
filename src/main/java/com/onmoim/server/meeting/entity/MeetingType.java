package com.onmoim.server.meeting.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum MeetingType {
	REGULAR("정기모임"),
	FLASH("번개모임");
	
	private final String description;
} 