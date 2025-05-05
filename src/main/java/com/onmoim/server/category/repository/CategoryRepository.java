package com.onmoim.server.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onmoim.server.category.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
