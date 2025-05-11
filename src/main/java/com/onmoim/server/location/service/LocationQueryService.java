package com.onmoim.server.location.service;

import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationQueryService {
	private final LocationRepository locationRepository;

	public Location findById(Long id) {
		return locationRepository.findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOCATION));
	}
}
