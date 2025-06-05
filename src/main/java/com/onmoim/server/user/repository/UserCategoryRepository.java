package com.onmoim.server.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.entity.UserCategoryId;

public interface UserCategoryRepository extends JpaRepository<UserCategory, UserCategoryId> {

	@Query("select uc.category.name from UserCategory uc where uc.user.id = :userId")
	List<String> findCategoryNamesByUserId(@Param("userId") Long userId);

}
