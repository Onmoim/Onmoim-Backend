package com.onmoim.server.meeting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.entity.UserMeetingId;

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

	/**
	 * 특정 일정의 모든 참석자 삭제 (일정 삭제 시 사용)
	 */
	@Modifying
	@Query("DELETE FROM UserMeeting um WHERE um.meeting.id = :meetingId")
	void deleteByMeetingId(@Param("meetingId") Long meetingId);

	/**
	 * 여러 일정의 모든 참석자 삭제 (테스트용)
	 */
	@Modifying
	@Query("DELETE FROM UserMeeting um WHERE um.meeting.id IN :meetingIds")
	void deleteAllByMeetingIdIn(@Param("meetingIds") List<Long> meetingIds);

	/**
	 * 특정 일정의 참석자 수 조회
	 */
	@Query("SELECT COUNT(um) FROM UserMeeting um WHERE um.meeting.id = :meetingId")
	long countByMeetingId(@Param("meetingId") Long meetingId);

	/**
	 * 특정 사용자가 참여한 모든 일정 ID 조회
	 */
	@Query("SELECT um.meeting.id FROM UserMeeting um WHERE um.user.id = :userId")
	List<Long> findMeetingIdsByUserId(@Param("userId") Long userId);
}
