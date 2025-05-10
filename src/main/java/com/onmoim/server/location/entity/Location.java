package com.onmoim.server.location.entity;

import org.hibernate.annotations.Comment;

import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {
	@Id
	@Column(name = "location_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("시")
	private String city;

	@Comment("구")
	private String district;

	@Comment("동")
	private String dong;
}
