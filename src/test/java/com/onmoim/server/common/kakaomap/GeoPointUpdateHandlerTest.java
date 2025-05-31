package com.onmoim.server.common.kakaomap;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;

/**
 * 이벤트 처리 및 카카오 맵 API 호출 통합 테스트
 * TestTransaction for 테스트 트랜잭션 커밋, Awaitility for 비동기 처리 wait
 */
@SpringBootTest
@Transactional
class GeoPointUpdateHandlerTest {

	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@Autowired
	private GroupRepository groupRepository;
	@Autowired
	private LocationRepository locationRepository;

	@Test
	@DisplayName("트랜잭션 활성화 테스트")
	void transactionCheck() {
		assertThat(TestTransaction.isActive()).isTrue();
	}

	@Test
	@DisplayName("GeoPointUpdateHandler 이벤트 성공 처리 테스트")
	void testSuccessHandle() {
		// given
		Group group = Group.groupCreateBuilder()
			.name("테스트 그룹")
			.description("테스트 그룹 설명").build();
		groupRepository.save(group);

		Location location = Location.create("1234", "서울특별시", "종로구", "청운동", null);
		locationRepository.save(location);

		Long groupId = group.getId();
		Long locationId = location.getId();
		String fullAddress = location.getFullAddress();

		// when
		eventPublisher.publishEvent(new GeoPointUpdateEvent(groupId, locationId, fullAddress));
		TestTransaction.flagForCommit(); // 테스트 트랜잭션 커밋
		TestTransaction.end();

		// 트랜잭션 종료 확인
		assertThat(TestTransaction.isActive()).isFalse();

		// then
		Awaitility.await().atMost(2, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				Optional<Group> optionalGroup = groupRepository.findById(groupId);
				assertThat(optionalGroup.isPresent()).isTrue();
				Group findGroup = optionalGroup.get();
				assertThat(findGroup.getGeoPoint()).isNotNull();
				System.out.println("geoPoint = " + findGroup.getGeoPoint());
			});
	}
}
