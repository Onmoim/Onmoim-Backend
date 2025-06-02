package com.onmoim.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableAsync
public class OnmoimApplication {
	public static void main(String[] args) {
		SpringApplication.run(OnmoimApplication.class, args);
		System.out.println("Hello World!");
	}

}
