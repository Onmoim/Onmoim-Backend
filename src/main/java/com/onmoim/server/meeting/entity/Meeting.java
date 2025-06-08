package com.onmoim.server.meeting.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.GroupUser;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
//3개 인덱스 구현
//
// 인덱스 1: idx_meeting_group_start_id (group_id, start_at, id)
//    - 대상 쿼리: findUpcomingMeetings (타입 필터 X)
//    - 효과: 그룹별 모임을 시간순으로 빠르게 조회 (커버링 인덱스)
//    - 사용 케이스: "특정 그룹의 모든 예정된 모임"
//
// 인덱스 2: idx_meeting_group_type_start_id (group_id, type, start_at, id)
//    - 대상 쿼리: findUpcomingMeetings (타입 필터 O)
//    - 효과: 그룹+타입 조건으로 필터링된 조회 최적화
//    - 사용 케이스: "특정 그룹의 정기모임만", "특정 그룹의 번개모임만"
//
// 인덱스 3: idx_meeting_id_deleted (id, deleted_date)
//    - 대상 쿼리: findByIdAndNotDeleted, findMeetingTypeById
//    - 효과: Soft Delete 패턴의 단건 조회 최적화
//    - 사용 케이스: 모임 상세 조회, AOP 락을 위한 타입 조회
//

@Table(name = "meeting", indexes = {
	@Index(name = "idx_meeting_group_start_id", columnList = "group_id, start_at, id"),
	@Index(name = "idx_meeting_group_type_start_id", columnList = "group_id, type, start_at, id"),
	@Index(name = "idx_meeting_id_deleted", columnList = "id, deleted_date")
})
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
	@Column(name = "group_id")
	private Long groupId;

	@Comment("일정 유형")
	@Enumerated(EnumType.STRING)
	private MeetingType type;

	@Comment("일정 제목")
	private String title;

	@Comment("일정 시작 시간")
	@Column(name = "start_at")
	private LocalDateTime startAt;

	@Comment("장소명")
	@Column(name = "place_name")
	private String placeName;

	@Comment("장소 좌표")
	@Embedded
	private GeoPoint geoPoint;

	@Comment("최대 참석 인원")
	private int capacity;

	@Comment("현재 참석 인원")
	@Column(name = "join_count")
	@Builder.Default
	private int joinCount = 0;

	@Comment("참가 비용")
	private int cost;

	@Comment("일정 상태")
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private MeetingStatus status = MeetingStatus.OPEN;

	@Comment("작성자 ID (논리 FK)")
	@Column(name = "creator_id")
	private Long creatorId;

	@Comment("일정 대표 이미지")
	private String imgUrl;

	// 비즈니스 로직 (도메인 규칙)

	/**
	 * 일정 생성 권한 확인
	 */
	public boolean canBeCreatedBy(GroupUser groupUser) {
		return switch (this.type) {
			case REGULAR -> groupUser.isOwner();     // 정기모임: 모임장만
			case FLASH -> groupUser.isJoined();      // 번개모임: 모임원
		};
	}

	/**
	 * 일정 관리 권한 확인 (수정/삭제)
	 */
	public boolean canBeManagedBy(GroupUser groupUser) {
		// 모든 타입에서 모임장만 관리 가능
		return groupUser.isOwner();
	}

	/**
	 * 이미지 업로드 권한 확인
	 */
	public boolean canUpdateImageBy(GroupUser groupUser) {
		return switch (this.type) {
			case REGULAR -> groupUser.isOwner();     // 정기모임: 모임장만
			case FLASH -> groupUser.isOwner() ||
						  groupUser.getUser().getId().equals(this.creatorId); // 번개모임: 모임장 또는 주최자
		};
	}

	/**
	 * 이미지 삭제 권한 확인
	 */
	public boolean canDeleteImageBy(GroupUser groupUser) {
		// 모든 타입에서 모임장만 삭제 가능
		return groupUser.isOwner();
	}

	/**
	 * 일정 참석 가능 여부 확인
	 */
	public boolean canJoin() {
		return this.status == MeetingStatus.OPEN &&
			   this.joinCount < this.capacity &&
			   !isStarted();
	}

	/**
	 * 일정 참석 취소 가능 여부 확인
	 */
	public boolean canLeave() {
		return !isStarted();
	}

	/**
	 * 일정 수정 가능 여부 확인 (24시간 전까지)
	 */
	public boolean canBeUpdated() {
		return LocalDateTime.now().isBefore(this.startAt.minusHours(24));
	}

	// 상태 변경 메서드

	/**
	 * 일정 참석 처리
	 */
	public void join() {
		// 1. 일정 시작 여부 확인
		if (isStarted()) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}
		// 2. 정원 초과 또는 마감 상태 확인
		if (this.status == MeetingStatus.FULL || this.joinCount >= this.capacity) {
			throw new CustomException(ErrorCode.GROUP_CAPACITY_EXCEEDED);
		}

		this.joinCount++;
		updateStatusIfFull();
	}

	/**
	 * 생성자 자동 참석 처리 (시간 제약 없음)
	 */
	public void creatorJoin() {
		if (this.joinCount >= this.capacity) {
			throw new CustomException(ErrorCode.GROUP_CAPACITY_EXCEEDED);
		}

		this.joinCount++;
		updateStatusIfFull();
	}


	/**
	 * 일정 참석 취소 처리
	 */
	public void leave() {
		if (!canLeave()) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}

		if (this.joinCount > 0) {
			this.joinCount--;
			if (this.status == MeetingStatus.FULL && this.joinCount < this.capacity) {
				this.status = MeetingStatus.OPEN;
			}
		}
	}

	/**
	 * 일정이 시작되었는지 확인
	 */
	public boolean isStarted() {
		return LocalDateTime.now().isAfter(this.startAt);
	}

	/**
	 * 자동 삭제 대상인지 확인
	 */
	public boolean shouldBeAutoDeleted() {
		return this.joinCount <= 1;
	}

	/**
	 * 일정 정보 수정
	 */
	public void updateMeetingInfo(String title, LocalDateTime startAt, String placeName,
					   GeoPoint geoPoint, int capacity, int cost) {
		if (!canBeUpdated()) {
			throw new CustomException(ErrorCode.MEETING_UPDATE_TIME_EXCEEDED);
		}
		if (capacity < this.joinCount) {
			throw new CustomException(ErrorCode.MEETING_CAPACITY_CANNOT_REDUCE);
		}

		this.title = title;
		this.startAt = startAt;
		this.placeName = placeName;
		this.geoPoint = geoPoint;
		this.capacity = capacity;
		this.cost = cost;

		updateStatusBasedOnCapacity();
	}

	/**
	 * 일정 이미지 업데이트
	 */
	public void updateImage(String imgUrl) {
		this.imgUrl = imgUrl;
	}


	private void updateStatusIfFull() {
		if (this.joinCount >= this.capacity) {
			this.status = MeetingStatus.FULL;
		}
	}

	private void updateStatusBasedOnCapacity() {
		if (this.joinCount >= this.capacity) {
			this.status = MeetingStatus.FULL;
		} else if (this.status == MeetingStatus.FULL) {
			this.status = MeetingStatus.OPEN;
		}
	}
}
