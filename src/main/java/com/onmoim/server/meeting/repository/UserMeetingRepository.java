package com.onmoim.server.meeting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.entity.UserMeetingId;

@Repository
public interface UserMeetingRepository extends JpaRepository<UserMeeting, UserMeetingId> {

	/**
	 * 특정 일정의 참석자 목록 조회
	 */
	@Query("SELECT um FROM UserMeeting um " +
		   "JOIN FETCH um.user " +
		   "WHERE um.meeting.id = :meetingId " +
		   "ORDER BY um.joinedAt ASC")
	Page<UserMeeting> findByMeetingIdWithUser(@Param("meetingId") Long meetingId, Pageable pageable);

	/**
	 * 특정 일정의 참석자 수 조회
	 */
	@Query("SELECT COUNT(um) FROM UserMeeting um WHERE um.meeting.id = :meetingId")
	long countByMeetingId(@Param("meetingId") Long meetingId);

	/**
	 * 사용자별 참석 일정 조회
	 */
	@Query("SELECT um FROM UserMeeting um " +
		   "JOIN FETCH um.meeting " +
		   "WHERE um.user.id = :userId " +
		   "ORDER BY um.meeting.startAt DESC")
	Page<UserMeeting> findByUserIdWithMeeting(@Param("userId") Long userId, Pageable pageable);

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
	 * 특정 일정의 참석자 ID 목록 조회
	 */
	@Query("SELECT um.user.id FROM UserMeeting um WHERE um.meeting.id = :meetingId")
	List<Long> findUserIdsByMeetingId(@Param("meetingId") Long meetingId);

	/**
	 * 일정 삭제 시 관련 참석 정보 삭제
	 */
	@Modifying
	@Query("DELETE FROM UserMeeting um WHERE um.meeting.id = :meetingId")
	void deleteByMeetingId(@Param("meetingId") Long meetingId);

	/**
	 * 사용자의 모든 참석 정보 삭제 (회원 탈퇴 시)
	 */
	@Modifying
	@Query("DELETE FROM UserMeeting um WHERE um.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);
} 