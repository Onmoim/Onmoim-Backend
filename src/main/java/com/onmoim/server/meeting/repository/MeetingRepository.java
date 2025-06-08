package com.onmoim.server.meeting.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	/**
	 * 그룹의 예정된 모임 목록 조회 (타입 필터링 가능)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.groupId = :groupId AND m.startAt > :now AND (:type IS NULL OR m.type = :type) AND (:lastId IS NULL OR m.id > :lastId) AND m.deletedDate IS NULL ORDER BY m.startAt ASC, m.id ASC")
	Slice<Meeting> findUpcomingMeetings(@Param("groupId") Long groupId, @Param("now") LocalDateTime now, @Param("type") MeetingType type, @Param("lastId") Long lastId, Pageable pageable);

	/**
	 * 사용자가 참여한 예정된 모임 목록 조회
	 */
	@Query("SELECT m FROM Meeting m JOIN UserMeeting um ON m.id = um.meeting.id WHERE um.user.id = :userId AND m.startAt > :now AND (:lastId IS NULL OR m.id > :lastId) AND m.deletedDate IS NULL ORDER BY m.startAt ASC, m.id ASC")
	Slice<Meeting> findUpcomingMeetingsByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("lastId") Long lastId, Pageable pageable);

	/**
	 * 일정 단건 조회 (삭제되지 않은 것만)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.id = :id AND m.deletedDate IS NULL")
	Optional<Meeting> findByIdAndNotDeleted(@Param("id") Long id);

	/**
	 * 일정 타입만 조회
	 */
	@Query("SELECT m.type FROM Meeting m WHERE m.id = :meetingId AND m.deletedDate IS NULL")
	MeetingType findMeetingTypeById(@Param("meetingId") Long meetingId);

	// 네임드 락 관련

	/**
	 * MySQL 네임드 락 획득
	 */
	@Query(value = "SELECT GET_LOCK(:lockKey, :timeoutSeconds)", nativeQuery = true)
	Integer getLock(@Param("lockKey") String lockKey, @Param("timeoutSeconds") int timeoutSeconds);

	/**
	 * MySQL 네임드 락 해제
	 */
	@Query(value = "SELECT RELEASE_LOCK(:lockKey)", nativeQuery = true)
	Integer releaseLock(@Param("lockKey") String lockKey);
}
