package com.onmoim.server.user.mapper;

import java.util.List;

import com.onmoim.server.group.repository.GroupLikeRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.group.repository.GroupViewLogRepository;
import com.onmoim.server.user.dto.response.UserCategoryResponseDto;
import com.onmoim.server.user.entity.UserCategory;
import org.springframework.stereotype.Component;

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
	private final GroupLikeRepository groupLikeRepository;
	private final GroupViewLogRepository groupViewLogRepository;
	private final GroupUserRepository groupUserRepository;

	@Override
	public ProfileResponseDto toProfileResponseDto(User user) {

		Long userId = user.getId();

		ProfileResponseDto response = userMapper.toProfileResponseDto(user);

		Long locationId = user.getLocation().getId();
		String locationName = locationRepository.findDongById(user.getLocation().getId());
		List<UserCategory> categories = userCategoryRepository.findUserCategoriesByUser(user);
		List<UserCategoryResponseDto> categoryList = categories.stream()
			.map(UserCategoryResponseDto::new)
			.toList();

		Long likedGroupsCount = groupLikeRepository.countLikedGroups(userId);
		Long recentViewedGroupsCount = groupViewLogRepository.countRecentViewedGroups(userId);
		Long joinedGroupsCount = groupUserRepository.countJoinedGroups(userId);

		response.setLocationId(locationId);
		response.setLocationName(locationName);
		response.setCategoryList(categoryList);
		response.setLikedGroupsCount(likedGroupsCount);
		response.setRecentViewedGroupsCount(recentViewedGroupsCount);
		response.setJoinedGroupsCount(joinedGroupsCount);

		return response;
	}

}
