package com.onmoim.server.meeting.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;


public interface MeetingRepository extends JpaRepository<Meeting, Long>, QuerydslPredicateExecutor<Meeting> {

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
