package com.onmoim.server.group.entity;

import org.hibernate.annotations.Comment;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_table")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(builderClassName = "GroupCreateBuilder", builderMethodName = "groupCreateBuilder")
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
	@Builder.Default
	private int participantCount = 1;

	@Comment("모임 대표 사진")
	private String imgUrl;

	public void join() {
		if (capacity < participantCount + 1) {
			throw new CustomException(ErrorCode.GROUP_CAPACITY_EXCEEDED);
		}
		// 변경 감지
		participantCount++;
	}
}
