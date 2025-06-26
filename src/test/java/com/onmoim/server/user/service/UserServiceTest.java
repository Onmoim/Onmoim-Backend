package com.onmoim.server.user.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

	@Test
	@DisplayName("회원가입 성공")
	void signupSuccess() {
		// given
		Location location = locationRepository.save(Location.create("100000", "서울특별시", "강남구", "역삼동", null));
		SignupRequestDto request = new SignupRequestDto("홍길동", "M", LocalDate.now(), 1L);

		String signupToken = jwtProvider.createSignupToken("google", "1234567890", "test@test.com"); // signupToken 생성
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
			assertEquals("test@test.com", savedUser.getEmail());
		} finally {
			JwtHolder.clear();
		}

	}

	@Test
	@DisplayName("관심사 저장 성공")
	void createUserCategorySuccess() {
		// given
		Location location = locationRepository.save(Location.create("100000", "서울특별시", "강남구", "역삼동", null));

		User user = userRepository.save(User.builder()
			.oauthId("1234567890")
			.provider("google")
			.email("test@test.com")
			.name("홍길동")
			.gender("F")
			.birth(LocalDateTime.now())
			.location(location)
			.build()
		);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(String.valueOf(user.getId()), null, List.of());
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		Category category1 = Category.create("운동/스포츠", null);
		Category category2 = Category.create("음악/악기", null);
		categoryRepository.save(category1);
		categoryRepository.save(category2);

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
		// given
		Location location = locationRepository.save(Location.create("100000", "서울특별시", "강남구", "역삼동", null));

		User user = userRepository.save(User.builder()
			.oauthId("1234567890")
			.provider("google")
			.email("test@test.com")
			.name("홍길동")
			.gender("F")
			.birth(LocalDateTime.now())
			.location(location)
			.build()
		);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(String.valueOf(user.getId()), null, List.of());
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		// when
		ProfileResponseDto profile = userService.getProfile();

		// then
		assertEquals("홍길동", profile.getName());
		assertEquals("역삼동", profile.getLocationName());
	}

}
