package com.onmoim.server.user.entity;

import java.sql.Timestamp;

import com.onmoim.server.category.entity.Category;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCategory {

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

	@Column(columnDefinition = "TIMESTAMP")
	private Timestamp createdAt;

	@Column(columnDefinition = "TIMESTAMP")
	private Timestamp updatedAt;

	@Column(columnDefinition = "TIMESTAMP")
	private Timestamp deletedAt;

}
