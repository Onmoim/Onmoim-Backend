package com.onmoim.server.user.entity;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCategory extends BaseEntity {

	@EmbeddedId
	private UserCategoryId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("categoryId")
	@JoinColumn(name = "category_id")
	private Category category;

	@Builder
	private UserCategory(User user, Category category) {
		this.user = user;
		this.category = category;
		this.id = new UserCategoryId(user.getId(), category.getId());
	}

	public static UserCategory create(User user, Category category) {
		return UserCategory.builder()
			.user(user)
			.category(category)
			.build();
	}
}
