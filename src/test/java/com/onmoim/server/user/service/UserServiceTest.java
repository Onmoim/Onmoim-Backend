package com.onmoim.server.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.dto.request.UpdateProfileRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.oauth.enumeration.SignupStatus;
import com.onmoim.server.security.JwtHolder;
import com.onmoim.server.security.JwtProvider;
import com.onmoim.server.user.dto.request.CreateUserCategoryRequestDto;
import com.onmoim.server.user.dto.request.SignupRequestDto;
import com.onmoim.server.user.dto.response.ProfileResponseDto;
import com.onmoim.server.user.dto.response.SignupResponseDto;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.repository.UserCategoryRepository;
import com.onmoim.server.user.repository.UserRepository;
import com.onmoim.server.user.service.impl.UserServiceImpl;

@SpringBootTest
@Transactional
public class UserServiceTest {

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private UserServiceImpl userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserCategoryRepository userCategoryRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private GroupQueryService groupQueryService;

	@Autowired
	private GroupUserRepository groupUserRepository;

	@MockBean
	private FileStorageService fileStorageService;

	private Location location;
	private Category category1;
	private Category category2;
	private User user;

	@BeforeEach
	void setUp() {
		location = locationRepository.save(Location.create("100000", "서울특별시", "강남구", "역삼동", null));
		category1 = categoryRepository.save(Category.create("운동/스포츠", null));
		category2 = categoryRepository.save(Category.create("음악/악기", null));

		user = userRepository.save(User.builder()
			.oauthId("1234567890")
			.provider("google")
			.email("test@test.com")
			.name("홍길동")
			.gender("F")
			.birth(LocalDateTime.now())
			.location(location)
			.profileImgUrl("https://cdn.example.com/profile/old.jpg")
			.build()
		);

		// 인증 정보 설정
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		CustomUserDetails userDetails = new CustomUserDetails(user.getId());
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("회원가입 성공")
	void signupSuccess() {
		// given
		SignupRequestDto request = new SignupRequestDto("홍길동", "M", LocalDate.now(), location.getId());

		String signupToken = jwtProvider.createSignupToken("kakao", "11111111111", "signup@test.com"); // signupToken 생성
		JwtHolder.set(signupToken);

		try {
			// when
			SignupResponseDto response = userService.signup(request);

			// then
			assertNotNull(response.getUserId());
			assertNotNull(response.getAccessToken());
			assertNotNull(response.getRefreshToken());
			assertEquals(SignupStatus.NO_CATEGORY, response.getStatus());

			// then
			User savedUser = userRepository.findById(response.getUserId()).orElseThrow();
			assertEquals("홍길동", savedUser.getName());
			assertEquals("signup@test.com", savedUser.getEmail());
		} finally {
			JwtHolder.clear();
		}

	}

	@Test
	@DisplayName("관심사 저장 성공")
	void createUserCategorySuccess() {
		// given
		CreateUserCategoryRequestDto request = new CreateUserCategoryRequestDto(
			user.getId(),
			List.of(category1.getId(), category2.getId())
		);

		// when
		userService.createUserCategory(request);

		// then
		List<UserCategory> all = userCategoryRepository.findAll();
		List<UserCategory> userCategories = all.stream()
			.filter(uc -> uc.getUser().getId().equals(user.getId()))
			.toList();

		assertEquals(2, userCategories.size());
	}

	@Test
	@DisplayName("유저 프로필 조회 성공")
	void getProfileSuccess() {
		// when
		ProfileResponseDto profile = userService.getProfile();

		// then
		assertEquals("홍길동", profile.getName());
		assertEquals("역삼동", profile.getLocationName());
	}

	@Test
	@DisplayName("유저 프로필 편집 성공")
	void updateUserProfileSuccess() {
		// given
		String testFileName = "test-profile.jpg";
		String directory = "images/profile";
		String keyName = directory + "/" + testFileName;
		String testDomain = "https://cdn.example.com";
		String expectedUrl = testDomain + "/" + keyName.replaceFirst("^images/", "");

		MockMultipartFile mockImage = new MockMultipartFile(
			"profileImgFile", testFileName, "image/jpeg", "image-content".getBytes()
		);

		FileUploadResponseDto mockUploadResponse = FileUploadResponseDto.builder()
			.fileUrl(expectedUrl)
			.fileName(testFileName)
			.fileSize(mockImage.getSize())
			.fileType(mockImage.getContentType())
			.build();

		when(fileStorageService.uploadFile(any(), eq(directory))).thenReturn(mockUploadResponse);
		doNothing().when(fileStorageService).deleteFile(any());

		UpdateProfileRequestDto request = new UpdateProfileRequestDto(
			"홍길동",
			"F",
			LocalDate.of(1990, 1, 1),
			location.getId(),
			"테스트",
			List.of(category1.getId()),
			null
		);

		// when
		userService.updateUserProfile(user.getId(), request, mockImage);

		// then
		User updatedUser = userRepository.findById(user.getId()).orElseThrow();
		assertEquals("홍길동", updatedUser.getName());
		assertEquals("F", updatedUser.getGender());
		assertEquals("테스트", updatedUser.getIntroduction());
		assertEquals(location.getId(), updatedUser.getLocation().getId());
		assertEquals(expectedUrl, updatedUser.getProfileImgUrl());

		List<UserCategory> userCategories = userCategoryRepository.findUserCategoriesByUser(updatedUser);
		assertEquals(1, userCategories.size());
		assertEquals(category1.getId(), userCategories.get(0).getCategory().getId());

		// 업로드/삭제 확인
		verify(fileStorageService).uploadFile(any(), eq(directory));
		verify(fileStorageService).deleteFile(any());
	}

	@Test
	@DisplayName("회원 탈퇴 성공")
	void leaveUserSuccess() {
		// when
		userService.leaveUser(user.getId());

		// then
		User deletedUser = userRepository.findById(user.getId()).orElseThrow();
		assertTrue(deletedUser.isDeleted());
	}

	@Test
	@DisplayName("모임장인 유저가 탈퇴 시도 - 예외 발생")
	void leaveUserIsGroupOwner() {
		// given
		// group, group_user 생성
		Group group = groupQueryService.saveGroup(
			category1,
			location,
			"테스트 모임",
			"테스트 모임 설명",
			100
		);

		groupUserRepository.save(GroupUser.create(group, user, Status.OWNER));

		// when, then
		CustomException exception = assertThrows(CustomException.class, () -> userService.leaveUser(user.getId()));
		assertEquals("IS_GROUP_OWNER", exception.getErrorCode().name());
	}

}
