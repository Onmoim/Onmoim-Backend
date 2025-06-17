package com.onmoim.server.meeting.repository;

import java.sql.Connection;
import java.sql.SQLException;

public interface MeetingLockRepository {
    boolean getLock(Connection conn, String key, int timeout) throws SQLException;
    void releaseLock(Connection conn, String key) throws SQLException;
} 