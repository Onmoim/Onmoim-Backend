package com.onmoim.server.group.dto;

import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.Status;

import jakarta.annotation.Nullable;


public record GroupDetail(
	// 모임 ID
	Long groupId,
	// 모임 이름
	String title,
	// 모임 설명
	String description,
	// 모임 주소
	String address,
	// 모임 카테고리
	String category,
	// 모임 이미지 URL
	String imgUrl,
	// 카테고리 아이콘 URL
	String iconUrl,
	// 모임 최대 인원
	int capacity,
	// 현재 사용자와 모임 관계
	@Nullable
	Status status,
	// 현재 사용자와의 모임 좋아요 관계
	@Nullable
	GroupLikeStatus likeStatus
)
{

}
