package com.onmoim.server.common.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.common.image.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
}