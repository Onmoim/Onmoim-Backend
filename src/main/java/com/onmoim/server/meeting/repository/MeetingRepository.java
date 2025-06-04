package com.onmoim.server.meeting.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;

import jakarta.persistence.LockModeType;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	/**
	 * 그룹별 예정된 일정 목록 조회 (커서 기반)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.groupId = :groupId AND m.startAt > :now AND (:cursorId IS NULL OR m.id > :cursorId) AND m.deletedDate IS NULL ORDER BY m.startAt ASC, m.id ASC")
	Slice<Meeting> findUpcomingMeetingsByGroupIdAfterCursor(@Param("groupId") Long groupId, @Param("now") LocalDateTime now, @Param("cursorId") Long cursorId, Pageable pageable);

	/**
	 * 그룹별 예정된 일정 목록 조회 (타입 필터링, 커서 기반)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.groupId = :groupId AND m.startAt > :now AND (:type IS NULL OR m.type = :type) AND (:cursorId IS NULL OR m.id > :cursorId) AND m.deletedDate IS NULL ORDER BY m.startAt ASC, m.id ASC")
	Slice<Meeting> findUpcomingMeetingsByGroupIdAndTypeAfterCursor(@Param("groupId") Long groupId, @Param("now") LocalDateTime now, @Param("type") MeetingType type, @Param("cursorId") Long cursorId, Pageable pageable);

	/**
	 * 사용자의 예정된 일정 조회 (커서 기반)
	 */
	@Query("SELECT m FROM Meeting m JOIN UserMeeting um ON m.id = um.meeting.id WHERE um.user.id = :userId AND m.startAt > :now AND (:cursorId IS NULL OR m.id > :cursorId) AND m.deletedDate IS NULL ORDER BY m.startAt ASC, m.id ASC")
	Slice<Meeting> findUpcomingMeetingsByUserIdAfterCursor(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("cursorId") Long cursorId, Pageable pageable);

	/**
	 * 일정 단건 조회 (삭제되지 않은 것만)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.id = :id AND m.deletedDate IS NULL")
	Optional<Meeting> findByIdAndNotDeleted(@Param("id") Long id);

	/**
	 * 일정 단건 조회 (Lock 적용)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT m FROM Meeting m WHERE m.id = :id AND m.deletedDate IS NULL")
	Optional<Meeting> findByIdAndNotDeletedWithLock(@Param("id") Long id);

	// ===== 네임드 락 관련 =====

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
