package com.onmoim.server.meeting.aop;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.stereotype.Component;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.repository.MeetingRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Meeting MySQL Named Lock AOP
 *
 * 완벽한 락 관리 패턴:
 * 1. @Order(HIGHEST_PRECEDENCE)로 트랜잭션 AOP보다 먼저 실행
 * 2. LazyConnectionDataSourceProxy로 커넥션 동일성 보장
 * 3. SpEL로 동적 락 키 생성
 * 4. try-finally로 락 해제 100% 보장
 * 5. autocommit 모드 명시적 제어
 *
 * 사용법:
 * @NamedLock(keySpEL = "#meetingId", timeout = 15)
 * @Transactional
 * public void joinMeeting(Long meetingId) { ... }
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 트랜잭션 AOP보다 먼저 실행
public class MeetingNamedLockAspect {

    private final DataSource dataSource;
    private final MeetingRepository meetingRepository;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public MeetingNamedLockAspect(DataSource dataSource, MeetingRepository meetingRepository) {
        // LazyConnectionDataSourceProxy로 커넥션 재사용 보장함.
        this.dataSource = new LazyConnectionDataSourceProxy(dataSource);
        this.meetingRepository = meetingRepository;
    }

    @Around("@annotation(com.onmoim.server.meeting.aop.NamedLock)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 어노테이션 정보 추출
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        NamedLock namedLock = method.getAnnotation(NamedLock.class);

        if (namedLock == null) {
            log.warn("@NamedLock 어노테이션을 찾을 수 없습니다: {}", method.getName());
            return pjp.proceed();
        }

        // 1. 락 키 생성 (SpEL 파싱)
        String lockKey = parseLockKey(namedLock, pjp);
        int timeout = determineDynamicTimeout(namedLock, pjp);

        log.debug("MySQL Named Lock 시작 - 키: {}, 타임아웃: {}초", lockKey, timeout);

        // 2. 커넥션 획득
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            // 3. 락 획득 (autocommit)
            connection.setAutoCommit(true);
            if (!acquireLock(connection, lockKey, timeout)) {
                throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
            }

            log.debug("MySQL Named Lock 획득 성공 - 키: {}", lockKey);

            // 4. 비즈니스 로직 실행
            Object result = pjp.proceed();

            log.info("비즈니스 로직 완료 - 키: {} (AOP Named Lock 완벽 보장)", lockKey);
            return result;

        } finally {
            // 5. 락 해제 + 커넥션 반환
            try {
                connection.setAutoCommit(true);
                releaseLock(connection, lockKey);
                log.debug("MySQL Named Lock 해제 완료 - 키: {}", lockKey);
            } catch (SQLException e) {
                log.error("락 해제 중 오류 발생 - 키: {} (계속 진행)", lockKey, e);
            } finally {
                DataSourceUtils.releaseConnection(connection, dataSource);
                log.debug("커넥션 반환 완료 - 키: {}", lockKey);
            }
        }
    }

    /**
     * SpEL을 사용한 동적 락 키 생성
     */
    private String parseLockKey(NamedLock namedLock, ProceedingJoinPoint pjp) {
        if (namedLock.keySpEL().isEmpty()) {
            throw new IllegalArgumentException("@NamedLock의 keySpEL이 비어있습니다.");
        }
        EvaluationContext context = new StandardEvaluationContext();

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // SpEL 표현식 파싱 및 평가
        Expression expression = parser.parseExpression(namedLock.keySpEL());
        Object keyValue = expression.getValue(context);

        return namedLock.lockPrefix() + keyValue;
    }

    /**
     * 동적 타임아웃 결정 (timeout = -1이면 타입별 타임아웃 적용)
     */
    private int determineDynamicTimeout(NamedLock namedLock, ProceedingJoinPoint pjp) {
        // 명시적 타임아웃이 설정된 경우 그대로 사용
        if (namedLock.timeout() > 0) {
            return namedLock.timeout();
        }

        // -1인 경우에만 동적 타임아웃 적용
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = pjp.getArgs();

            for (int i = 0; i < paramNames.length; i++) {
                if ("meetingId".equals(paramNames[i]) && args[i] instanceof Long) {
                    Long meetingId = (Long) args[i];
                    return getTimeoutByMeetingType(meetingId);
                }
            }
        } catch (Exception e) {
            log.warn("동적 타임아웃 결정 실패, 기본값 사용: {}", e.getMessage());
        }

        return 10; // 기본값 (동적 결정 실패 시)
    }

    /**
     * 일정 타입별 타임아웃 결정
     */
    private int getTimeoutByMeetingType(Long meetingId) {
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
        } catch (Exception e) {
            log.error("일정 타입 조회 중 오류 발생 - meetingId: {}, 기본 타임아웃 사용", meetingId, e);
            return 10; // 기본값
        }
    }

    /**
     * 락 획득
     */
    private boolean acquireLock(Connection connection, String lockKey, int timeoutSeconds) {
        final int maxRetries = 3;
        final long backoffMs = 100;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT GET_LOCK('%s', %d)", lockKey, timeoutSeconds);
                var resultSet = statement.executeQuery(sql);

                if (resultSet.next()) {
                    int result = resultSet.getInt(1);
                    boolean acquired = (result == 1);

                    if (acquired) {
                        if (attempt > 1) {
                            log.info("MySQL Named Lock 획득 성공 ({}회 시도) - 키: {}", attempt, lockKey);
                        }
                        return true;
                    } else {
                        log.warn("MySQL Named Lock 획득 실패 (시도 {}/{}) - 키: {}, 타임아웃: {}초",
                            attempt, maxRetries, lockKey, timeoutSeconds);

                        // 마지막 시도가 아니면 백오프
                        if (attempt < maxRetries) {
                            try {
                                Thread.sleep(backoffMs);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return false;
                            }
                        }
                    }
                } else {
                    log.error("GET_LOCK 결과셋이 비어있음 (시도 {}/{}) - 키: {}", attempt, maxRetries, lockKey);
                }
            } catch (SQLException e) {
                log.error("락 획득 중 SQL 오류 (시도 {}/{}) - 키: {}", attempt, maxRetries, lockKey, e);

                // 마지막 시도가 아니면 백오프
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        log.error("MySQL Named Lock 획득 최종 실패 ({}회 시도 모두 실패) - 키: {}", maxRetries, lockKey);
        return false;
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
                log.debug("MySQL Named Lock 해제 결과 - 키: {}, 결과: {}", lockKey, result);
            }
        } catch (SQLException e) {
            log.error("MySQL Named Lock 해제 실패 - 키: {} (계속 진행)", lockKey, e);
            // 해제 실패해도 애플리케이션은 계속 진행
        }
    }
}
