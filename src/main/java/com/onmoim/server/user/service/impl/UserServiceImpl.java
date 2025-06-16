package com.onmoim.server.user.service.impl;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.oauth.service.RefreshTokenService;
import com.onmoim.server.user.dto.request.CreateUserCategoryRequestDto;
import com.onmoim.server.user.dto.request.SignupRequestDto;
import com.onmoim.server.user.dto.request.UpdateProfileRequestDto;
import com.onmoim.server.user.dto.response.ProfileResponseDto;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.mapper.UserMapperCustom;
import com.onmoim.server.user.repository.UserCategoryRepository;
import com.onmoim.server.user.repository.UserRepository;
import com.onmoim.server.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final UserCategoryRepository userCategoryRepository;
	private final UserMapperCustom userMapperCustom;
	private final GroupUserRepository groupUserRepository;
	private final RefreshTokenService refreshTokenService;
	private final FileStorageService fileStorageService;

	public Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated()) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}

		log.info("userId = {}", Long.parseLong(auth.getName()));

		return Long.parseLong(auth.getName());
	}

	@Override
	public Long signup(SignupRequestDto request) {

		Optional<User> existingUser = userRepository.findByOauthIdAndProvider(
			request.getOauthId(), request.getProvider()
		);

		if (existingUser.isPresent()) {
			throw new CustomException(ALREADY_EXISTS_USER);
		}

		User user = User.builder()
			.oauthId(request.getOauthId())
			.provider(request.getProvider())
			.email(request.getEmail())
			.name(request.getName())
			.gender(request.getGender())
			.birth(request.getBirth())
			.addressId(request.getAddressId())
			.build();

		userRepository.save(user);
		Long userId = user.getId();

		return userId;
	}

	@Transactional
	@Override
	public void createUserCategory(CreateUserCategoryRequestDto request) {

		List<Long> categoryIdList = request.getCategoryIdList();
		User user = userRepository.findById(request.getUserId())
			.orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		for (Long categoryId : categoryIdList) {
			Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new CustomException(INVALID_CATEGORY));

			UserCategory userCategory = UserCategory.builder()
					.user(user)
					.category(category)
					.build();

			userCategoryRepository.save(userCategory);
		}
	}

	@Override
	public ProfileResponseDto getProfile() {
		ProfileResponseDto response;
		User user = userRepository.findById(getCurrentUserId())
			.orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		response = userMapperCustom.toProfileResponseDto(user);

		return response;
	}

	@Override
	@Transactional
	public void updateUserProfile(Long userId, UpdateProfileRequestDto request, MultipartFile profileImgFile) {
		User user = userRepository.findById(getCurrentUserId())
			.orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		// 0. 프로필 사진 첨부파일 있을 경우 먼저 S3에 업로드
		FileUploadResponseDto fileUploadResponse = null;
		if (profileImgFile != null) {
			String directory = "images/profile";
			fileUploadResponse = fileStorageService.uploadFile(profileImgFile, directory);
			log.info("fileUrl = {}", fileUploadResponse.getFileUrl());

			// 1. user 테이블 update(사진 등록/교체하는 경우)
			user.updateProfile(
				request.getName(),
				request.getGender(),
				request.getBirth(),
				request.getAddressId(),
				request.getIntroduction(),
				fileUploadResponse.getFileUrl()
			);
		} else {
			// 1. user 테이블 update(기존 사진 교체 없는 경우)
			user.updateProfile(
				request.getName(),
				request.getGender(),
				request.getBirth(),
				request.getAddressId(),
				request.getIntroduction(),
				request.getProfileImgUrl()
			);
		}

		// 2. user_category 테이블 delete
		List<UserCategory> userCategoryList = userCategoryRepository.findUserCategoriesByUser(user);
		if (userCategoryList != null) {
			userCategoryRepository.deleteAll(userCategoryList);
		}

		// 3. user_category 테이블 insert
		List<Long> categoryIdList = request.getCategoryIdList();

		for (Long categoryId : categoryIdList) {
			Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new CustomException(INVALID_CATEGORY));

			UserCategory userCategory = UserCategory.builder()
				.user(user)
				.category(category)
				.build();

			userCategoryRepository.save(userCategory);
		}

	}

	@Override
	@Transactional
	public void leaveUser(Long id) {
		Long userId = getCurrentUserId();

		if (!userId.equals(id)) {
			throw new CustomException(FORBIDDEN_USER_ACCESS);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		// 탈퇴하려는 유저가 모임장으로 있는 모임이 하나 이상 있는 경우 exception
		boolean isGroupOwner = groupUserRepository.existsByUserIdAndStatus(userId, Status.OWNER);
		if (isGroupOwner) {
			throw new CustomException(ErrorCode.IS_GROUP_OWNER);
		}

		// TODO: 1. meeting_user 삭제
		// TODO: 2. meeting 삭제(참석자가 본인 포함 1명일 경우)

		// 3. group_user 테이블 soft delete
		List<GroupUser> groupUserList = groupUserRepository.findGroupUserByUserId(userId);
		if (!groupUserList.isEmpty()) {
			groupUserList.forEach(GroupUser::deleteGroupUser);
		}

		// 4. user 테이블 soft delete
		user.leaveUser();

		// 5. redis에서 refresh token 삭제
		refreshTokenService.deleteRefreshToken(id);
	}

}
