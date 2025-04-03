package com.onmoim.server.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Profile("local")
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/redis")
public class RedisController {

    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping
    public ResponseEntity<String> create(@RequestParam(required = false) String value) {
        if(!StringUtils.hasText(value)) {
            return ResponseEntity.badRequest().body("value is empty");
        }
        redisTemplate.opsForValue().set("key", value);
        return ResponseEntity.ok().body("ok");
    }

    @GetMapping
    public ResponseEntity<String> get() {
        String find = redisTemplate.opsForValue().get("key");
        if(find == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(find);
    }
}
