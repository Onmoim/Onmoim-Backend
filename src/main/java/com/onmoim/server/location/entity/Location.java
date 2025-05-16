package com.onmoim.server.location.entity;

import org.hibernate.annotations.Comment;

import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "location",
	indexes = {
		@Index(name = "idx_location_dong", columnList = "dong")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {
	@Id
	@Column(name = "location_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("코드")
	private String code;

	@Comment("시")
	private String city;

	@Comment("구")
	private String district;

	@Comment("동")
	private String dong;

	@Comment("동리")
	private String village;

	public static Location create(String code, String city, String district, String dong, String village) {
		Location location = new Location();
		location.code = code;
		location.city = city;
		location.district = district;
		location.dong = dong;
		location.village = village;
		return location;
	}
}
