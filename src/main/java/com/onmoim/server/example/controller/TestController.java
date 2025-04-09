package com.onmoim.server.example.controller;

import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.example.entity.TestEntity;
import com.onmoim.server.example.service.TestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
@Tag(name = "Test", description = "Test 관련 API입니다.")
public class TestController {

    private final TestService testService;

    @PostMapping("/save")
    @Operation(summary = "유저 정보 저장", description = "유저 정보를 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 정보 저장 성공"),
            @ApiResponse(responseCode = "409", description = "유저 정보 저장 실패(유저 중복)") })
    public ResponseEntity<String> testPost(@RequestBody TestEntity userInfo) {
        log.info("유저 정보 저장 컨트롤러 실행");
        try {
            testService.save(userInfo);
            return ResponseEntity.ok("유저 정보 저장 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("유저 정보 저장 실패");
        }
    }

    @GetMapping("/findAll")
    @Operation(summary = "전체 유저 정보 반환", description = "전체 유저 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 전체 정보 반환 성공"),
            @ApiResponse(responseCode = "400", description = "유저 전체 정보 반환 실패") })
    public ResponseEntity<List<TestEntity>> testGet() {
        log.info("유저 정보 반환 컨트롤러 실행");

        try {
            List<TestEntity> testEntity = testService.findAll();
            return ResponseEntity.ok(testEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(null);
        }

    }
} 