package com.onmoim.server;

import java.time.LocalDateTime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableAsync
public class OnmoimApplication {
	public static void main(String[] args) {
		SpringApplication.run(OnmoimApplication.class, args);
		System.out.println("Hello World!");
		// Production Server Start Time check
		System.out.println("Current Time: " + LocalDateTime.now());
	}

}
