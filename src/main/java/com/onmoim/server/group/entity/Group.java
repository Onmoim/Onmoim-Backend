package com.onmoim.server.group.entity;

import static com.onmoim.server.common.exception.ErrorCode.*;

import org.hibernate.annotations.Comment;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.GeoPoint;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.location.entity.Location;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_table")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Group extends BaseEntity {
	@Id
	@Column(name = "group_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location;

	@Comment("모임 이름")
	@Column(unique = true)
	private String name;

	@Comment("모임 설명")
	@Column(columnDefinition = "TEXT")
	private String description;

	@Comment("모임 정원")
	private int capacity;

	@Comment("모임 대표 사진")
	private String imgUrl;

	@Embedded
	GeoPoint geoPoint;

	public void join(Long current) {
		if (capacity < current + 1) {
			throw new CustomException(GROUP_CAPACITY_EXCEEDED);
		}
	}

	public void update(String description, int capacity, Long currentMember) {
		if(currentMember > capacity){
			throw new CustomException(CAPACITY_MUST_BE_GREATER_THAN_CURRENT);
		}
		this.description = description;
		this.capacity = capacity;
	}

	public void updateImage(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	public void updateLocation(final GeoPoint geoPoint) {
		this.geoPoint = geoPoint;
	}
}
