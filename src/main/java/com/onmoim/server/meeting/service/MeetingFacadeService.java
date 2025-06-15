package com.onmoim.server.meeting.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingFacadeService {

	private final MeetingService meetingService;
	private final MeetingLockManager lockManager;

	/**
	 * 일정 참석 신청
	 */
	public void joinMeeting(Long meetingId) {
		log.info("일정 참석 신청 요청 - meetingId: {}", meetingId);

		lockManager.executeWithLock(meetingId, () -> {
			meetingService.joinMeeting(meetingId);
		});

		log.info("일정 참석 신청 완료 - meetingId: {}", meetingId);
	}

	/**
	 * 일정 참석 취소
	 */
	public void leaveMeeting(Long meetingId) {
		log.info("일정 참석 취소 요청 - meetingId: {}", meetingId);

		lockManager.executeWithLock(meetingId, () -> {
			meetingService.leaveMeeting(meetingId);
		});

		log.info("일정 참석 취소 완료 - meetingId: {}", meetingId);
	}
}
