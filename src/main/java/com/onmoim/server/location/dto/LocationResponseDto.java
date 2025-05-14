package com.onmoim.server.location.dto;

import com.onmoim.server.location.entity.Location;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class LocationResponseDto {

	@Schema(description = "location_id")
	private Long locationId;

	@Schema(description = "코드")
	private String code;

	@Schema(description = "시")
	private String city;

	@Schema(description = "구")
	private String district;

	@Schema(description = "동")
	private String dong;


	public LocationResponseDto(Long locationId, String code, String city, String district, String dong) {
		this.locationId = locationId;
		this.code = code;
		this.city = city;
		this.district = district;
		this.dong = dong;
	}

	public static LocationResponseDto from(Location location) {
		return new LocationResponseDto(
			location.getId(),
			location.getCode(),
			location.getCity(),
			location.getDistrict(),
			location.getDong()
		);
	}

}
