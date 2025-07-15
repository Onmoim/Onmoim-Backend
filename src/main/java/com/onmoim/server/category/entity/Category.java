package com.onmoim.server.category.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

	@Id
	@Column(name = "category_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("이름")
	@Column(nullable = false)
	private String name;

	@Comment("아이콘 url")
	private String iconUrl;

	@Builder
	private Category(String name, String iconUrl) {
		this.name = name;
		this.iconUrl = iconUrl;
	}

	public static Category create(String name, String iconUrl) {
		return Category.builder()
			.name(name)
			.iconUrl(iconUrl)
			.build();
	}

}
