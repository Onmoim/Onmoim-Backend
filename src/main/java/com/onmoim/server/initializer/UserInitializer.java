package com.onmoim.server.initializer;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import net.datafaker.Faker;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.entity.UserCategoryId;
import com.onmoim.server.user.repository.UserCategoryRepository;
import com.onmoim.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserInitializer {

	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final UserCategoryRepository userCategoryRepository;

	@Bean
	public CommandLineRunner initDummyUsers() {
		return args -> {
			Faker faker = new Faker(new Locale("ko"));
			Timestamp now = new Timestamp(System.currentTimeMillis());

			// 1. 유저 생성
			for (int i = 0; i < 100; i++) {

				User user = User.builder()
					.name(faker.name().fullName().replaceAll("\\s+", ""))
					.gender(faker.gender().binaryTypes().equalsIgnoreCase("Male") ? "M" : "F")
					.birth(Date.valueOf(faker.timeAndDate().birthday(20, 50)))
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

}
