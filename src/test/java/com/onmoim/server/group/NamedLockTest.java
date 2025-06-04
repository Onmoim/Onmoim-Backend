package com.onmoim.server.group;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.onmoim.server.group.repository.GroupRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * 락 획득 성공 -> 1 반환
 * 락 획득 실패 -> 0 반환
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NamedLockTest {
	@Autowired
	private GroupRepository groupRepository;
	@TestConfiguration
	static class TestConfig {
		@Bean
		public JPAQueryFactory jpaQueryFactory(EntityManager em){
			return new JPAQueryFactory(em);
		}
	}
	@Test
	void checkLockException() throws InterruptedException, ExecutionException {
		final var target = "named_lock";
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		Future<Long> future1 = executorService.submit(
			() -> groupRepository.getLock(target, 2));

		Future<Long> future2 = executorService.submit(
			() -> groupRepository.getLock(target, 0));

		assertThat(future1.get()).isEqualTo(1L);
		assertThat(future2.get()).isEqualTo(0L);
		executorService.shutdown();
	}
}
