package com.onmoim.server.group.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.group.dto.request.CreateGroupRequestDTO;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.security.CustomOAuth2User;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {
	private final GroupRepository groupRepository;
	private final GroupUserRepository groupUserRepository;
	private final UserRepository userRepository;
	private final LocationRepository locationRepository;

	/**
	 * 모임 이름 중복 처리 어떻게? try-catch-throw?
	 * @return 생성된 그룹 ID
	 */
	@Transactional
	public Long createGroup(CreateGroupRequestDTO request) {
		User user = userRepository.findById(getCurrentUserId())
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_USER));

		Location location = locationRepository.findById(request.getLocationId())
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOCATION));

		Group group = Group.create(
				request.getName(), request.getDescription(), request.getCapacity(), location);
		groupRepository.save(group);

		GroupUser groupUser = GroupUser.create(group, user, Status.OWNER);

		groupUserRepository.save(groupUser);

		return group.getId();
	}

	private Long getCurrentUserId()	{
		CustomOAuth2User principal = (CustomOAuth2User)SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getId();
	}
}
