package com.onmoim.server.group.entity;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.HQLSelect;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.location.entity.Location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseEntity {
	@Id
	@Column(name = "group_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location;

	@Comment("모임 이름")
	@Column(unique = true)
	private String name;

	@Comment("모임 설명")
	@Column(columnDefinition = "TEXT")
	private String description;

	@Comment("모임 최대 정원")
	private int capacity;

	@Comment("현재 모임 인원")
	private int participantCount;

	@Comment("모임 대표 사진")
	private String imgUrl;

	public static Group create(String name, String description, int capacity, Location location) {
		Group group = new Group();
		group.name = name;
		group.description = description;
		group.capacity = capacity;
		group.location = location;
		return group;
	}
}
