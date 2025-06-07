package com.onmoim.server.meeting.entity;

import java.time.LocalDateTime;

import com.onmoim.server.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMeeting {
	@EmbeddedId
	private UserMeetingId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("meetingId")
	@JoinColumn(name = "meeting_id")
	private Meeting meeting;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "joined_at")
	private LocalDateTime joinedAt;

	public static UserMeeting create(Meeting meeting, User user) {
		UserMeeting userMeeting = new UserMeeting();
		userMeeting.meeting = meeting;
		userMeeting.user = user;
		userMeeting.joinedAt = LocalDateTime.now();
		userMeeting.id = new UserMeetingId(meeting.getId(), user.getId());
		return userMeeting;
	}
} 