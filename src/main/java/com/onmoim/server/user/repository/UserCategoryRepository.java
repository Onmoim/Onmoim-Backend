package com.onmoim.server.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onmoim.server.user.entity.UserCategory;
import com.onmoim.server.user.entity.UserCategoryId;

public interface UserCategoryRepository extends JpaRepository<UserCategory, UserCategoryId> {
}
