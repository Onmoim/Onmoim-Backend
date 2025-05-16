package com.onmoim.server.common.image.repository;

import com.onmoim.server.common.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
