package com.onmoim.server.meeting.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMeetingId implements Serializable {
	private Long meetingId;
	private Long userId;

	public UserMeetingId(Long meetingId, Long userId) {
		this.meetingId = meetingId;
		this.userId = userId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserMeetingId that = (UserMeetingId) o;
		return Objects.equals(meetingId, that.meetingId) && Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(meetingId, userId);
	}
}
