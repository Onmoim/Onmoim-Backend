package com.onmoim.server.location.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.onmoim.server.location.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

	List<Location> findByDongStartingWithAndVillageIsNull(String dong);

	@Query("SELECT l.dong FROM Location l WHERE l.id = :id")
	String findDongById(Long id);

}
