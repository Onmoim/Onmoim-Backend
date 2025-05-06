package com.onmoim.server.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.group.entity.GroupUser;

public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {
}
