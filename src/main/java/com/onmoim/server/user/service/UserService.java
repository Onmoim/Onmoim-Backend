package com.onmoim.server.user.service;

import com.onmoim.server.user.dto.request.CreateUserCategoryRequestDto;
import com.onmoim.server.user.dto.request.SignupRequestDto;
import com.onmoim.server.user.dto.request.UpdateProfileRequestDto;
import com.onmoim.server.user.dto.response.ProfileResponseDto;

public interface UserService {

	public Long signup(SignupRequestDto request);

	public void createUserCategory(CreateUserCategoryRequestDto request);

	public ProfileResponseDto getProfile();

	public void updateUserProfile(Long userId, UpdateProfileRequestDto request);

}
