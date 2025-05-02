package com.onmoim.server.category.entity;

import java.sql.Timestamp;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("이름")
	@Column(nullable = false)
	private String name;

	@Comment("아이콘 url")
	private String iconUrl;

	@Column(columnDefinition = "TIMESTAMP")
	@Comment("생성일")
	private Timestamp createdAt;

	@Column(columnDefinition = "TIMESTAMP")
	@Comment("수정일")
	private Timestamp updatedAt;

	@Column(columnDefinition = "TIMESTAMP")
	@Comment("삭제일")
	private Timestamp deletedAt;

}
