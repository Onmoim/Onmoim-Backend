package com.onmoim.server.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onmoim.server.example.entity.TestEntity;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {
}
