package com.onmoim.server.meeting.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum MeetingStatus {
	OPEN("모집중"),
	FULL("모집완료"),
	CLOSED("모집마감");
	
	private final String description;
} 