package com.onmoim.server.group.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.GroupUserId;

public interface GroupUserRepository extends JpaRepository<GroupUser, GroupUserId> {
	@Query("select gu from GroupUser gu where gu.id.groupId = :groupId and gu.id.userId = :userId")
	Optional<GroupUser> findGroupUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
