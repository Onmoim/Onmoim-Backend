package com.onmoim.server.meeting.repository;

import java.util.List;

public interface UserMeetingRepositoryCustom {

	List<Long> findRemainingMeetingIdsByUserId(Long userId);

}
