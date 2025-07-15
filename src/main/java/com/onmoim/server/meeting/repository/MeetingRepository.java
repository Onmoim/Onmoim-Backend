package com.onmoim.server.meeting.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;


public interface MeetingRepository extends JpaRepository<Meeting, Long>, QuerydslPredicateExecutor<Meeting>, MeetingRepositoryCustom {

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

}
