package com.onmoim.server.user.entity;

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
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("유저 role id")
	private Long roleId;

	@Comment("소셜 고유 id")
	private String oauthId;

	@Comment("소셜 프로바이더")
	private String provider;

	@Comment("이메일")
	private String email;

	@Comment("이름")
	private String name;

	@Comment("성별")
	private String gender;

	@Comment("생년월일")
	private LocalDateTime birth;

	@Comment("지역 id")
	private Long addressId;

	@Comment("관심사(카테고리)")
	private Long categoryId;

	@Comment("프로필 사진 url")
	private String profileImgUrl;

	@Comment("자기소개")
	private String introduction;

	@Builder
	private User(Long roleId, String oauthId, String provider, String email, String name, String gender,
		LocalDateTime birth, Long addressId, Long categoryId, String profileImgUrl, String introduction) {
		this.roleId = roleId;
		this.provider = provider;
		this.email = email;
		this.oauthId = oauthId;
		this.name = name;
		this.gender = gender;
		this.birth = birth;
		this.addressId = addressId;
		this.categoryId = categoryId;
		this.profileImgUrl = profileImgUrl;
		this.introduction = introduction;
	}

	public static User create(Long roleId, String oauthId, String provider, String email,
		String name, String gender, LocalDateTime birth, Long addressId,
		Long categoryId, String profileImgUrl, String introduction) {
		return User.builder()
			.roleId(roleId)
			.oauthId(oauthId)
			.provider(provider)
			.email(email)
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
