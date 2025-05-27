package com.onmoim.server.user.mapper;

import org.mapstruct.Mapper;

import com.onmoim.server.user.dto.response.ProfileResponseDto;
import com.onmoim.server.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

	ProfileResponseDto toProfileResponseDto(User user);

}
