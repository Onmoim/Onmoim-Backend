package com.onmoim.server.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.group.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
