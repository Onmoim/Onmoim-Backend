package com.onmoim.server.user.service.impl;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static com.onmoim.server.oauth.enumeration.SignupStatus.*;
import static org.springframework.data.jpa.domain.AbstractPersistable_.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.oauth.dto.OAuthUserDto;
import com.onmoim.server.oauth.service.OAuthService;
import com.onmoim.server.oauth.service.RefreshTokenService;
import com.onmoim.server.security.JwtHolder;
import com.onmoim.server.security.JwtProvider;
import com.onmoim.server.user.dto.request.CreateUserCategoryRequestDto;
import com.onmoim.server.user.dto.request.SignupRequestDto;
import com.onmoim.server.user.dto.request.UpdateProfileRequestDto;
import com.onmoim.server.user.dto.response.ProfileResponseDto;
import com.onmoim.server.user.dto.response.SignupResponseDto;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.mapper.UserMapperCustom;
import com.onmoim.server.user.repository.UserCategoryRepository;
import com.onmoim.server.user.repository.UserRepository;
import com.onmoim.server.user.service.UserService;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final LocationRepository locationRepository;
	private final OAuthService oAuthService;
	private final CategoryRepository categoryRepository;
	private final UserCategoryRepository userCategoryRepository;
	private final UserMapperCustom userMapperCustom;
	private final GroupUserRepository groupUserRepository;
	private final UserMeetingRepository userMeetingRepository;
	private final RefreshTokenService refreshTokenService;
	private final FileStorageService fileStorageService;
	private final JwtProvider jwtProvider;
	private final MeetingRepository meetingRepository;

	@Override
	public Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}

		log.info("userId = {}", Long.parseLong(auth.getName()));

		return Long.parseLong(auth.getName());
	}

	public OAuthUserDto extractSignupClaims() {
		String token = JwtHolder.get();
		if (token == null) {
			 throw new CustomException(ErrorCode.SIGNUP_TOKEN_REQUIRED);
		}

		Claims claims = jwtProvider.parseSignupToken(token);

		String provider = claims.get("provider", String.class);
		String oauthId = claims.get("oauthId", String.class);
		String email = claims.get("email", String.class);

		return new OAuthUserDto(provider, oauthId, email);
	}

	@Override
	public SignupResponseDto signup(SignupRequestDto request) {
		OAuthUserDto claims = extractSignupClaims();

		String provider = claims.getProvider();
		String oauthId = claims.getOauthId();
		String email = claims.getEmail();

		Optional<User> existingUser = userRepository.findByOauthIdAndProvider(
			oauthId, provider
		);

		if (existingUser.isPresent()) {
			throw new CustomException(ALREADY_EXISTS_USER);
		}

		Location location = locationRepository.findById(request.getLocationId())
			.orElseThrow(() -> new CustomException(LOCATION_NOT_FOUND));

		User user = User.builder()
			.oauthId(oauthId)
			.provider(provider)
			.email(email)
			.name(request.getName())
			.gender(request.getGender())
			.birth(request.getBirth().atStartOfDay())
			.location(location)
			.build();

		userRepository.save(user);
		Long userId = user.getId();

		Authentication authentication = oAuthService.createAuthentication(user);
		oAuthService.setAuthenticationToContext(authentication);

		String accessToken = jwtProvider.createAccessToken(authentication);
		String refreshToken = jwtProvider.createRefreshToken(authentication);
		refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

		return new SignupResponseDto(userId, accessToken, refreshToken, NO_CATEGORY);
	}

	@Transactional
	@Override
	public void createUserCategory(CreateUserCategoryRequestDto request) {

		Long userId = getCurrentUserId();

		if (!userId.equals(request.getUserId())) {
			throw new CustomException(FORBIDDEN_USER_ACCESS);
		}

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
	public void updateUserProfile(Long id, UpdateProfileRequestDto request, MultipartFile profileImgFile) {

		Long userId = getCurrentUserId();

		if (!userId.equals(id)) {
			throw new CustomException(FORBIDDEN_USER_ACCESS);
		}

		User user = userRepository.findById(getCurrentUserId())
			.orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		Location location = locationRepository.findById(request.getLocationId())
			.orElseThrow(() -> new CustomException(LOCATION_NOT_FOUND));

		// 0. 프로필 사진 첨부파일 있을 경우 먼저 S3에 업로드
		FileUploadResponseDto fileUploadResponse = null;
		if (profileImgFile != null) {
			String directory = "images/profile";
			fileUploadResponse = fileStorageService.uploadFile(profileImgFile, directory);
			log.info("fileUrl = {}", fileUploadResponse.getFileUrl());

			// 기존 이미지가 있을 경우 삭제
			if (StringUtils.hasText(user.getProfileImgUrl())) {
				fileStorageService.deleteFile(user.getProfileImgUrl());
			}

			// 1. user 테이블 update(사진 등록/교체하는 경우)
			user.updateProfile(
				request.getName(),
				request.getGender(),
				request.getBirth().atStartOfDay(),
				location,
				fileUploadResponse.getFileUrl(),
				request.getIntroduction()
			);
		} else {
			// 1. user 테이블 update(기존 사진 교체 없는 경우)
			user.updateProfile(
				request.getName(),
				request.getGender(),
				request.getBirth().atStartOfDay(),
				location,
				request.getProfileImgUrl(),
				request.getIntroduction()
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

		// 1. meeting 및 user_meeting 삭제(참석자가 본인 포함 1명일 경우)
		List<Meeting> emptyMeetings = meetingRepository.findEmptyMeetingsByCreator(userId);
		log.info("emptyMeetings.size = {}", emptyMeetings.size());
		if (!emptyMeetings.isEmpty()) {
			List<Long> emptyMeetingIds = new ArrayList<>();

			for (Meeting emptyMeeting : emptyMeetings) {
				emptyMeetingIds.add(emptyMeeting.getId());
				emptyMeeting.softDelete(); // meeting 삭제
			}
			userMeetingRepository.deleteAllByMeetingIdIn(emptyMeetingIds); // user_meeting 삭제
		}

		// 2. user_meeting 삭제(아직 시작하지 않은 일정의 user_meeting만 삭제)
		List<Long> remainingUserMeetings = userMeetingRepository.findRemainingMeetingIdsByUserId(userId);
		log.info("remainingUserMeetings.size = {}", remainingUserMeetings.size());
		if (!remainingUserMeetings.isEmpty()) {
			userMeetingRepository.deleteAllByUserIdAndMeetingIdIn(userId, remainingUserMeetings);
		}

		// 3. group_user 테이블 soft delete
		List<GroupUser> groupUserList = groupUserRepository.findGroupUserByUserId(userId);
		log.info("groupUserList.size = {}", groupUserList.size());
		if (!groupUserList.isEmpty()) {
			groupUserList.forEach(GroupUser::deleteGroupUser);
		}

		// 4. user 테이블 soft delete
		user.leaveUser();

		// 5. redis에서 refresh token 삭제
		refreshTokenService.deleteRefreshToken(id);
	}

}
