package com.onmoim.server.user.mapper;

import com.onmoim.server.user.dto.response.ProfileResponseDto;
import com.onmoim.server.user.entity.User;

public interface UserMapperCustom {

	ProfileResponseDto toProfileResponseDto(User user);

}
