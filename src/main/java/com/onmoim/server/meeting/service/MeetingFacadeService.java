package com.onmoim.server.meeting.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.meeting.aop.NamedLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingFacadeService {

	private final MeetingService meetingService;

	/**
	 * 일정 참석 신청 (AOP Named Lock)
	 *
	 * @NamedLock: MySQL Named Lock으로 동시성 제어 (동적 타임아웃)
	 * @Transactional: 비즈니스 로직 트랜잭션 관리
	 */
	@NamedLock(keySpEL = "#meetingId")
	@Transactional
	public void joinMeeting(Long meetingId) {
		log.info("일정 참석 신청 요청 - meetingId: {}", meetingId);

		meetingService.joinMeeting(meetingId);

		log.info("일정 참석 신청 완료 - meetingId: {}", meetingId);
	}

	/**
	 * 일정 참석 취소
	 */
	@NamedLock(keySpEL = "#meetingId")
	@Transactional
	public void leaveMeeting(Long meetingId) {
		log.info("일정 참석 취소 요청 - meetingId: {}", meetingId);

		meetingService.leaveMeeting(meetingId);

		log.info("일정 참석 취소 완료 - meetingId: {}", meetingId);
	}
}
