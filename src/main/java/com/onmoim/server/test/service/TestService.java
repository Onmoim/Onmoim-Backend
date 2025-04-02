package com.onmoim.server.test.service;

import java.util.List;

import com.onmoim.server.test.entity.TestEntity;

public interface TestService {
    void save(TestEntity userInfo);
    List<TestEntity> findAll();
} 