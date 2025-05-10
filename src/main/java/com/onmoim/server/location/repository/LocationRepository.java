package com.onmoim.server.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.location.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
