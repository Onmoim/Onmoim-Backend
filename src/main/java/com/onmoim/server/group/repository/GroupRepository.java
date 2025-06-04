package com.onmoim.server.group.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.onmoim.server.group.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long>, GroupRepositoryCustom {
	@Query(value = "select get_lock(:key, :timeout)", nativeQuery = true)
	void getLock(String key, int timeout);

	@Query(value = "select release_lock(:key)", nativeQuery = true)
	void releaseLock(String key);

	// group, category, location left fetch join
	@Query("select g from Group g left join fetch g.category left join fetch g.location where g.id=:groupId")
	Optional<Group> findGroupWithDetails(Long groupId);
}
