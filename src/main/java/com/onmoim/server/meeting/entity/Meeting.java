package com.onmoim.server.meeting.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(builderClassName = "MeetingCreateBuilder", builderMethodName = "meetingCreateBuilder")
public class Meeting extends BaseEntity {
	@Id
	@Column(name = "meeting_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("모임 ID (논리 FK)")
	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Comment("일정 유형")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MeetingType type;

	@Comment("일정 제목")
	@Column(nullable = false)
	private String title;

	@Comment("일정 시작 시간")
	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Comment("장소명")
	@Column(name = "place_name", nullable = false)
	private String placeName;

	@Comment("위도")
	@Column(precision = 9, scale = 6, nullable = false)
	private BigDecimal lat;

	@Comment("경도")
	@Column(precision = 9, scale = 6, nullable = false)
	private BigDecimal lng;

	@Comment("최대 참석 인원")
	@Column(nullable = false)
	private int capacity;

	@Comment("현재 참석 인원")
	@Column(name = "join_count", nullable = false)
	private int joinCount = 0;

	@Comment("참가 비용")
	@Column(nullable = false)
	private int cost;

	@Comment("일정 상태")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MeetingStatus status = MeetingStatus.OPEN;

	@Comment("작성자 ID (논리 FK)")
	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	public void join() {
		if (this.joinCount >= this.capacity) {
			throw new CustomException(ErrorCode.GROUP_CAPACITY_EXCEEDED);
		}
		this.joinCount++;
		updateStatusIfFull();
	}

	public void leave() {
		if (this.joinCount > 0) {
			this.joinCount--;
			if (this.status == MeetingStatus.FULL && this.joinCount < this.capacity) {
				this.status = MeetingStatus.OPEN;
			}
		}
	}

	public void close() {
		this.status = MeetingStatus.CLOSED;
	}

	public boolean isOpen() {
		return this.status == MeetingStatus.OPEN;
	}

	public boolean isFull() {
		return this.status == MeetingStatus.FULL;
	}

	public boolean isClosed() {
		return this.status == MeetingStatus.CLOSED;
	}

	public boolean isStarted() {
		return LocalDateTime.now().isAfter(this.startAt);
	}

	public boolean canEdit() {
		return LocalDateTime.now().isBefore(this.startAt.minusHours(24));
	}

	private void updateStatusIfFull() {
		if (this.joinCount >= this.capacity) {
			this.status = MeetingStatus.FULL;
		}
	}
} 