package com.onmoim.server.common.lock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Transactional
@Component
@RequiredArgsConstructor
public class NamedLockRepository {
	private final EntityManager em;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void getLock(String key) {
		String sql = "select get_lock(:key, 3000)";
		em.createNativeQuery(sql)
			.setParameter("key", key)
			.getSingleResult();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void releaseLock(String key) {
		em.createNativeQuery("select release_lock(:key)")
			.setParameter("key", key)
			.getSingleResult();
	}
}
