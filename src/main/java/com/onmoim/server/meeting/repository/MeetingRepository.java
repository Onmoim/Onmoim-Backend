package com.onmoim.server.meeting.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingStatus;
import com.onmoim.server.meeting.entity.MeetingType;

import jakarta.persistence.LockModeType;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	/**
	 * 그룹별 일정 목록 조회 (타입별 필터링)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.groupId = :groupId AND m.deletedDate IS NULL ORDER BY m.startAt ASC")
	Page<Meeting> findByGroupIdAndNotDeleted(@Param("groupId") Long groupId, Pageable pageable);

	@Query("SELECT m FROM Meeting m WHERE m.groupId = :groupId AND m.type = :type AND m.deletedDate IS NULL ORDER BY m.startAt ASC")
	Page<Meeting> findByGroupIdAndTypeAndNotDeleted(@Param("groupId") Long groupId, @Param("type") MeetingType type, Pageable pageable);

	/**
	 * 일정 단건 조회 (삭제되지 않은 것만, 읽기 전용)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.id = :id AND m.deletedDate IS NULL")
	Optional<Meeting> findByIdAndNotDeleted(@Param("id") Long id);

	/**
	 * 일정 단건 조회 (Lock 적용)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT m FROM Meeting m WHERE m.id = :id AND m.deletedDate IS NULL")
	Optional<Meeting> findByIdAndNotDeletedWithLock(@Param("id") Long id);

	/**
	 * 상태별 일정 조회
	 */
	@Query("SELECT m FROM Meeting m WHERE m.status = :status AND m.deletedDate IS NULL")
	List<Meeting> findByStatusAndNotDeleted(@Param("status") MeetingStatus status);

	/**
	 * 시작 시간이 지난 OPEN 상태 일정 조회 (상태 변경용)
	 */
	@Query("SELECT m FROM Meeting m WHERE m.status IN (:statuses) AND m.startAt < :now AND m.deletedDate IS NULL")
	List<Meeting> findByStatusInAndStartAtBeforeAndNotDeleted(@Param("statuses") List<MeetingStatus> statuses, @Param("now") LocalDateTime now);

	/**
	 * 생성자별 일정 조회
	 */
	@Query("SELECT m FROM Meeting m WHERE m.creatorId = :creatorId AND m.deletedDate IS NULL ORDER BY m.startAt DESC")
	Page<Meeting> findByCreatorIdAndNotDeleted(@Param("creatorId") Long creatorId, Pageable pageable);
}
