package com.onmoim.server.group.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * 락 획득 성공 -> 1 반환
 * 락 획득 실패 -> 0 반환
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NamedLockTest {
	private static String NAME = "target";
	@Autowired
	private TransactionFacade transactionFacade;

	@TestConfiguration
	static class TestConfig {
		@Bean
		public JPAQueryFactory jpaQueryFactory(EntityManager em){
			return new JPAQueryFactory(em);
		}
		@Bean
		public TransactionFacade transactionFacade(GroupRepository groupRepository) {
			return new TransactionFacade(groupRepository);
		}
	}

	static class TransactionFacade {

		private final GroupRepository groupRepository;

		public TransactionFacade(GroupRepository groupRepository) {
			this.groupRepository = groupRepository;
		}

		@Transactional
		public Long start() throws InterruptedException {
			Long result = groupRepository.getLock(NAME, 0);
			Thread.sleep(500L);
			groupRepository.releaseLock(NAME);
			return result;
		}
	}

	@Test
	@Disabled
	void checkLockException() throws InterruptedException, ExecutionException {
		final var target = "named_lock";
		CompletableFuture<Long> result1 = CompletableFuture.supplyAsync(() -> {
				try {
					return transactionFacade.start();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).thenApply(num -> num);

		CompletableFuture<Long> result2 = CompletableFuture.supplyAsync(() -> {
			try {
				return transactionFacade.start();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).thenApply(num -> num);

		assertThat(result1.get()).isEqualTo(1L);
		assertThat(result2.get()).isEqualTo(0L);
	}
}
