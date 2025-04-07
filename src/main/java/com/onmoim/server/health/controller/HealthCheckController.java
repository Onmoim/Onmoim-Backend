package com.onmoim.server.health.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
@Tag(name = "Health Check", description = "서버 상태 확인 API입니다.")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "서버 상태 확인", description = "서버가 정상 작동 중인지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "서버 정상 작동 중")
    })
    public ResponseEntity<String> healthCheck() {
        log.info("Health check 요청 수신");
        return ResponseEntity.ok("Server is up and running");
    }
} 