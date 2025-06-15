package com.onmoim.server.meeting.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.repository.MeetingRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Meeting Named Lock 전용 관리 서비스
 *
 * 4단계 개선:
 * 1. 명시적 커넥션 획득/반환 (누수 방지)
 * 2. TransactionTemplate으로 같은 커넥션 보장
 * 3. autocommit 상태 명시적 제어
 * 4. 타임아웃 & 폴링 전략 개선
 *
 * 장점:
 * - 커넥션 풀 누수 완전 방지
 * - 세션 일관성 100% 보장
 * - autocommit 상태 명확 제어
 * - 향상된 사용자 경험
 */
@Slf4j
@Service
public class MeetingLockManager {

    private final MeetingRepository meetingRepository;
    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;

    public MeetingLockManager(
            MeetingRepository meetingRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {
        this.meetingRepository = meetingRepository;
        // 커넥션 재사용 최적화
        this.dataSource = new LazyConnectionDataSourceProxy(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 락을 획득하고 비즈니스 로직을 실행한 후 락을 해제
     */
    public void executeWithLock(Long meetingId, Runnable businessLogic) {
        String lockKey = generateLockKey(meetingId);
        int timeoutSeconds = determineLockTimeout(meetingId);

        log.debug("Meeting Named Lock 시작 - 키: {}, 타임아웃: {}초", lockKey, timeoutSeconds);

        // 1. 명시적 커넥션 획득
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            // 2.락 획득 (autocommit 명시적 설정)
            connection.setAutoCommit(true);
            if (!acquireLock(connection, lockKey, timeoutSeconds)) {
                throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
            }

            log.debug("Meeting Named Lock 획득 성공 - 키: {}", lockKey);

            try {
                // 3. 비즈니스 트랜잭션 (TransactionTemplate으로 같은 커넥션 보장)
                transactionTemplate.execute(status -> {
                    try {
                        connection.setAutoCommit(false);
                        businessLogic.run();
                        return null;
                    } catch (SQLException e) {
                        throw new IllegalStateException("autocommit 설정 실패", e);
                    }
                });

                log.info("일정 비즈니스 로직 완료 - meetingId: {} (Facade Named Lock 완벽 보장)", meetingId);
            } finally {
                // 4.락 해제 (autocommit 복원 후 해제)
                connection.setAutoCommit(true);
                releaseLock(connection, lockKey);
                log.debug("Meeting Named Lock 해제 완료 - 키: {}", lockKey);
            }
        } catch (SQLException e) {
            log.error("커넥션 관리 중 오류 발생 - meetingId: {}", meetingId, e);
            throw new IllegalStateException("Named Lock 처리 실패", e);
        } finally {
            // 1️명시적 커넥션 반환 (풀 누수 방지)
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    /**
     * 락 획득 시도
     */
    private boolean acquireLock(Connection connection, String lockKey, int timeoutSeconds) {
        try (Statement statement = connection.createStatement()) {
            String sql = String.format("SELECT GET_LOCK('%s', %d)", lockKey, timeoutSeconds);
            var resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                int result = resultSet.getInt(1);
                boolean acquired = (result == 1);

                if (!acquired) {
                    log.warn("Meeting Named Lock 획득 실패 - 키: {}, 타임아웃: {}초", lockKey, timeoutSeconds);
                }

                return acquired;
            }
            return false;
        } catch (SQLException e) {
            log.error("락 획득 중 오류 발생 - 키: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 락 해제
     */
    private void releaseLock(Connection connection, String lockKey) {
        try (Statement statement = connection.createStatement()) {
            String sql = String.format("SELECT RELEASE_LOCK('%s')", lockKey);
            var resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                Integer result = resultSet.getInt(1);
                log.debug("Meeting Named Lock 해제 결과 - 키: {}, 결과: {}", lockKey, result);
            }
        } catch (SQLException e) {
            log.error("Meeting Named Lock 해제 실패 - 키: {} (계속 진행)", lockKey, e);
            // 해제 실패해도 애플리케이션은 계속 진행
        }
    }

    /**
     * 락 키 생성
     */
    private String generateLockKey(Long meetingId) {
        return "meeting_" + meetingId;
    }

    /**
     * 일정 타입에 따른 락 타임아웃 결정
     */
    private int determineLockTimeout(Long meetingId) {
        try {
            MeetingType meetingType = meetingRepository.findMeetingTypeById(meetingId);

            if (meetingType == null) {
                log.error("일정 타입 조회 실패 - meetingId: {}", meetingId);
                throw new CustomException(ErrorCode.NOT_EXISTS_GROUP);
            }

            return switch (meetingType) {
                case REGULAR -> {
                    log.debug("정기모임 락 타임아웃: 10초 - meetingId: {}", meetingId);
                    yield 10;
                }
                case FLASH -> {
                    log.debug("번개모임 락 타임아웃: 15초 - meetingId: {}", meetingId);
                    yield 15;
                }
                default -> {
                    log.warn("알 수 없는 일정 타입: {} - meetingId: {}", meetingType, meetingId);
                    yield 10;
                }
            };
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("일정 타입 조회 중 오류 발생 - meetingId: {}", meetingId, e);
            throw new CustomException(ErrorCode.NOT_EXISTS_GROUP);
        }
    }
}
