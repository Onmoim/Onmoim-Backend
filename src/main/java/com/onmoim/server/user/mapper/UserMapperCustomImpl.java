package com.onmoim.server.user.mapper;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.util.List;

import org.springframework.stereotype.Component;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.user.dto.response.ProfileResponseDto;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserCategoryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserMapperCustomImpl implements UserMapperCustom {

	private final UserMapper userMapper;
	private final LocationRepository locationRepository;
	private final UserCategoryRepository userCategoryRepository;

	@Override
	public ProfileResponseDto toProfileResponseDto(User user) {
		ProfileResponseDto response = userMapper.toProfileResponseDto(user);

		Long locationId = user.getLocation().getId();
		String locationName = locationRepository.findDongById(user.getLocation().getId());
		List<String> categoryList = userCategoryRepository.findCategoryNamesByUserId(user.getId());

		response.setLocationId(locationId);
		response.setLocationName(locationName);
		response.setCategoryList(categoryList);
		return response;
	}

}
