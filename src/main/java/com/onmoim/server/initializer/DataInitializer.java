package com.onmoim.server.initializer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.datafaker.Faker;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.repository.UserCategoryRepository;
import com.onmoim.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Profile("local")
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final UserCategoryRepository userCategoryRepository;
	private final LocationRepository locationRepository;

	/**
	 * 카테고리 데이터 생성
	 */
	@Bean
	@Order(1)
	public CommandLineRunner initCategories(CategoryRepository categoryRepository) {
		return args -> {
			if (categoryRepository.count() > 0) {
				System.out.println("Category 데이터 존재");
				return;
			}

			List<String> names = List.of(
				"운동/스포츠", "사교/인맥", "인문학/책/글", "아웃도어/여행",
				"음악/악기", "업종/직무", "문화/공연/축제", "외국/언어",
				"게임/오락", "공예/만들기", "댄스/무용", "봉사활동",
				"사진/영상", "자기계발", "스포츠관람", "반려동물",
				"요리/제조", "차/바이크"
			);

			List<Category> categories = names.stream()
				.map(name -> Category.create(name, null))
				.toList();

			categoryRepository.saveAll(categories);
		};
	}

	/**
	 * 유저 데이터 생성
	 */
	@Bean
	@Order(2)
	public CommandLineRunner initDummyUsers() {
		return args -> {
			// User 데이터 존재할 경우 다시 들어가지 않도록 함
			if (userRepository.count() > 0) {
				System.out.println("User 데이터 존재");
				return;
			}

			Faker faker = new Faker(new Locale("ko"));

			// 1. 유저 생성
			for (int i = 0; i < 100; i++) {
				User user = User.builder()
					.name(faker.name().fullName().replaceAll("\\s+", ""))
					.gender(faker.gender().binaryTypes().equalsIgnoreCase("Male") ? "M" : "F")
					.birth(LocalDateTime.now())
					.addressId(faker.number().numberBetween(1L, 300L))
					.build();

				userRepository.save(user);
			}

			// 2. 유저-카테고리 랜덤 생성
			List<User> users = userRepository.findAll();
			List<Category> categories = categoryRepository.findAll();

			for (User user : users) {
				Set<Long> assigned = new HashSet<>();
				int num = faker.number().numberBetween(1, 3); // 1~2개 관심사 랜덤

				for (int i = 0; i < num; i++) {
					Category category;
					do {
						category = categories.get(faker.number().numberBetween(0, categories.size()));
					} while (!assigned.add(category.getId()));

					UserCategory uc = UserCategory.create(user, category);

					userCategoryRepository.save(uc);
				}
			}
		};
	}

	/**
	 * 지역 데이터 생성
	 */
	@Override
	@Order(3)
	public void run(String... args) throws Exception {
		// Location 데이터 존재할 경우 다시 들어가지 않도록 함
		if (locationRepository.count() > 0) {
			System.out.println("Location 데이터 존재");
			return;
		}

		InputStream is = getClass().getClassLoader().getResourceAsStream("location.csv");
		if (is == null) {
			throw new FileNotFoundException("location.csv not found");
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			boolean isFirstLine = true;

			while ((line = reader.readLine()) != null) {
				if (isFirstLine) { // 첫줄 제외
					isFirstLine = false;
					continue;
				}

				String[] tokens = line.split(",", -1);

				if (tokens.length < 5) continue;

				String code = tokens[0].trim();
				String city = tokens[1].trim().isEmpty() ? null : tokens[1].trim();
				String district = tokens[2].trim().isEmpty() ? null : tokens[2].trim();
				String dong = tokens[3].trim().isEmpty() ? null : tokens[3].trim();
				String village = tokens[4].trim().isEmpty() ? null : tokens[4].trim();

				Location location = Location.create(code, city, district, dong, village);
				locationRepository.save(location);

			}
		}
	}
}
