package com.onmoim.server.group.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.service.CategoryQueryService;
import com.onmoim.server.group.dto.request.CreateGroupRequestDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.service.LocationQueryService;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {
	private final GroupQueryService groupQueryService;
	private final GroupUserQueryService groupUserQueryService;
	private final UserQueryService userQueryService;
	private final LocationQueryService locationQueryService;
	private final CategoryQueryService categoryQueryService;

	@Transactional
	public Long createGroup(CreateGroupRequestDto request) {
		User user = userQueryService.findById(getCurrentUserId());
		Location location = locationQueryService.findById(request.getLocationId());
		Category category = categoryQueryService.findById(request.getCategoryId());

		Group group = Group.groupCreateBuilder()
			.name(request.getName())
			.description(request.getDescription())
			.capacity(request.getCapacity())
			.location(location)
			.category(category)
			.build();
		groupQueryService.saveGroup(group);

		GroupUser groupUser = GroupUser.create(group, user, Status.OWNER);
		groupUserQueryService.save(groupUser);
		return group.getId();
	}

	private Long getCurrentUserId()	{
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}
