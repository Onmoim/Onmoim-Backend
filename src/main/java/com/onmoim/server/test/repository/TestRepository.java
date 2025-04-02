package com.onmoim.server.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onmoim.server.test.entity.TestEntity;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {
} 