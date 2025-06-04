package com.onmoim.server.meeting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.entity.UserMeetingId;

@Repository
public interface UserMeetingRepository extends JpaRepository<UserMeeting, UserMeetingId> {

	/**
	 * 특정 사용자가 특정 일정에 참석했는지 확인
	 */
	@Query("SELECT um FROM UserMeeting um WHERE um.meeting.id = :meetingId AND um.user.id = :userId")
	Optional<UserMeeting> findByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

	/**
	 * 특정 일정에 대한 사용자 참석 여부 확인
	 */
	@Query("SELECT CASE WHEN COUNT(um) > 0 THEN true ELSE false END " +
		   "FROM UserMeeting um WHERE um.meeting.id = :meetingId AND um.user.id = :userId")
	boolean existsByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);
} 