package com.onmoim.server.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.entity.UserCategoryId;

public interface UserCategoryRepository extends JpaRepository<UserCategory, UserCategoryId> {

	@Query("SELECT uc FROM UserCategory uc JOIN FETCH uc.category WHERE uc.user = :user")
	List<UserCategory> findUserCategoriesByUser(User user);

}
