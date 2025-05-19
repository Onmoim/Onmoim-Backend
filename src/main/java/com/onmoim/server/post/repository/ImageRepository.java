package com.onmoim.server.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.onmoim.server.post.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
}