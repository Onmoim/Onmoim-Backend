package com.onmoim.server.location.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import com.onmoim.server.location.dto.LocationResponseDto;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;

@SpringBootTest
public class LocationServiceTest {

	@Mock
	private LocationRepository locationRepository;

	@InjectMocks
	private LocationQueryService locationQueryService;

	private Location location;

	@Test
	@DisplayName("동 이름으로 지역 조회 성공")
	void findByDongSuccess() {
		// given
		String dong = "신교동";
		Location location1 = Location.create("code", "서울특별시", "종로구", "신교동", null);
		Location location2 = Location.create("code", "경상북도", "경산시", "신교동", null);
		List<Location> locations = List.of(location1, location2);

		when(locationRepository.findByDongContainingAndVillageIsNull(dong)).thenReturn(locations);

		// when
		List<LocationResponseDto> result = locationQueryService.findByDong(dong);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getDong()).isEqualTo("신교동");
		assertThat(result.get(1).getDistrict()).isEqualTo("경산시");

		verify(locationRepository).findByDongContainingAndVillageIsNull(dong);
	}
}
