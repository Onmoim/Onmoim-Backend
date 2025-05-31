package com.onmoim.server.common.kakaomap;


/**
 * {@link GeoPointUpdateHandler}
 * @param groupId // group(모임) pk
 * @param locationId // location(위치) pk
 * @param address // city(시) + district(구) + dong(동) + village(동리)
 */
public record GeoPointUpdateEvent (Long groupId, Long locationId, String address) {
}
