package com.onmoim.server.user.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.user.dto.SignupRequest;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;
import com.onmoim.server.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;

	@Override
	public void signup(SignupRequest signupRequest) {

		Optional<User> existingUser = userRepository.findByOauthIdAndProvider(
			signupRequest.getOauthId(), signupRequest.getProvider()
		);

		if (existingUser.isPresent()) {
			throw new IllegalStateException("이미 가입된 사용자입니다.");
		}

		User user = User.builder()
			.oauthId(signupRequest.getOauthId())
			.provider(signupRequest.getProvider())
			.email(signupRequest.getEmail())
			.name(signupRequest.getName())
			.gender(signupRequest.getGender())
			.birth(signupRequest.getBirth())
			.addressId(signupRequest.getAddressId())
			.categoryId(signupRequest.getCategoryId())
			.build();

		userRepository.save(user);

	}

}
