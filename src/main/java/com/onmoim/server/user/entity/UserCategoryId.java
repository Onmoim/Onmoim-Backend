package com.onmoim.server.user.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Embeddable
@Getter
@EqualsAndHashCode
public class UserCategoryId implements Serializable {

	private final Long userId;
	private final Long categoryId;

	protected UserCategoryId() {
		this.userId = null;
		this.categoryId = null;
	}

	public UserCategoryId(Long userId, Long categoryId) {
		this.userId = userId;
		this.categoryId = categoryId;
	}

}
