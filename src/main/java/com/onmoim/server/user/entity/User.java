package com.onmoim.server.user.entity;

import java.sql.Date;

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
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("유저 role id")
	private Long roleId;

	@Comment("SNS 타입")
	private String snsType;

	@Comment("소셜 key")
	private String oauthId;

	@Comment("이름")
	@Column(nullable = false)
	private String name;

	@Comment("성별")
	@Column(nullable = false)
	private String gender;

	@Comment("생년월일")
	@Column(nullable = false)
	private Date birth;

	@Comment("지역 id")
	@Column(nullable = false)
	private Long addressId;

	@Comment("관심사(카테고리)")
	private Long categoryId;

	@Comment("프로필 사진 url")
	private String profileImgUrl;

	@Comment("자기소개")
	private String introduction;

	@Builder
	private User(Long roleId, String snsType, String oauthId, String name, String gender, Date birth,
		Long addressId, Long categoryId, String profileImgUrl, String introduction) {
		this.roleId = roleId;
		this.snsType = snsType;
		this.oauthId = oauthId;
		this.name = name;
		this.gender = gender;
		this.birth = birth;
		this.addressId = addressId;
		this.categoryId = categoryId;
		this.profileImgUrl = profileImgUrl;
		this.introduction = introduction;
	}

	public static User create(Long roleId, String snsType, String oauthId, String name, String gender, Date birth,
		Long addressId, Long categoryId, String profileImgUrl, String introduction) {
		return User.builder()
			.roleId(roleId)
			.snsType(snsType)
			.oauthId(oauthId)
			.name(name)
			.gender(gender)
			.birth(birth)
			.addressId(addressId)
			.categoryId(categoryId)
			.profileImgUrl(profileImgUrl)
			.introduction(introduction)
			.build();
	}

}
