package com.onmoim.server.location.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.location.dto.LocationResponseDto;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationQueryService {
	private final LocationRepository locationRepository;

	public Location getById(Long id) {
		return locationRepository.findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOCATION));
	}

	public List<LocationResponseDto> findByDong(String dong) {
		return locationRepository.findByDongStartingWithAndVillageIsNull(dong).stream()
			.map(LocationResponseDto::from)
			.collect(Collectors.toList());
	}
}
