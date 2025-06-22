package com.onmoim.server.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.GroupUserId;
import com.onmoim.server.group.entity.Status;

public interface GroupUserRepository extends JpaRepository<GroupUser, GroupUserId>, GroupUserRepositoryCustom {
	@Query("select gu from GroupUser gu where gu.id.groupId = :groupId and gu.id.userId = :userId")
	Optional<GroupUser> findGroupUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

	@Query("select count(gu) from GroupUser gu where gu.id.groupId = :groupId and gu.status in (:statuses)")
	Long countByGroupAndStatuses(@Param("groupId") Long groupId, @Param("statuses") List<Status> statuses);

	boolean existsByUserIdAndStatus(Long userId, Status status);

	List<GroupUser> findGroupUserByUserId(Long userId);

}
