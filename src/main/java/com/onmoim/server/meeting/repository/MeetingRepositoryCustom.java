package com.onmoim.server.meeting.repository;

import com.onmoim.server.meeting.entity.Meeting;

import java.util.List;

public interface MeetingRepositoryCustom {

	List<Meeting> findEmptyMeetingsByCreator(Long userId);

}
