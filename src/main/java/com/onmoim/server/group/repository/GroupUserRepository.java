package com.onmoim.server.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.GroupUserId;

public interface GroupUserRepository extends JpaRepository<GroupUser, GroupUserId> {
}
