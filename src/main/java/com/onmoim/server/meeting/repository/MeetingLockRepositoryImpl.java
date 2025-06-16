package com.onmoim.server.meeting.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.stereotype.Repository;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

@Repository
public class MeetingLockRepositoryImpl implements MeetingLockRepository {

	private static final String GET_LOCK_SQL      = "SELECT GET_LOCK(?, ?)";
	private static final String RELEASE_LOCK_SQL  = "SELECT RELEASE_LOCK(?)";

	@Override
	public boolean getLock(Connection conn, String key, int timeout) {
		try (PreparedStatement ps = conn.prepareStatement(GET_LOCK_SQL)) {
			ps.setString(1, key);
			ps.setInt(2, timeout);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) == 1;
			}
		} catch (SQLException e) {
			throw new CustomException(ErrorCode.LOCK_SYSTEM_ERROR);
		}
	}

	@Override
	public void releaseLock(Connection conn, String key) {
		try (PreparedStatement ps = conn.prepareStatement(RELEASE_LOCK_SQL)) {
			ps.setString(1, key);
			ps.executeQuery();
		} catch (SQLException e) {
			throw new CustomException(ErrorCode.LOCK_SYSTEM_ERROR);
		}
	}
}
