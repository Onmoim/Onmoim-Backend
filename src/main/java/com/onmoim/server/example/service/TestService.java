package com.onmoim.server.example.service;

import java.util.List;

import com.onmoim.server.example.entity.TestEntity;

public interface TestService {
	void save(TestEntity userInfo);

	List<TestEntity> findAll();
}
