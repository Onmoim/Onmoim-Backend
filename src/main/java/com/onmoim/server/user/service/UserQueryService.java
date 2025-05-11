package com.onmoim.server.user.service;

import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserQueryService {
	private final UserRepository userRepository;

	public User findById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_USER));
	}
}
