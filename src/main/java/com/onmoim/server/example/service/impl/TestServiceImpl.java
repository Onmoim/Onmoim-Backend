package com.onmoim.server.example.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.example.entity.TestEntity;
import com.onmoim.server.example.repository.TestRepository;
import com.onmoim.server.example.service.TestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {
    
    private final TestRepository testRepository;
    
    @Override
    @Transactional
    public void save(TestEntity userInfo) {
        testRepository.save(userInfo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TestEntity> findAll() {
        return testRepository.findAll();
    }
} 