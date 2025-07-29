package com.onmoim.server.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.location.entity.Location;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location;

	@Comment("프로필 사진 url")
	private String profileImgUrl;

	@Comment("자기소개")
	private String introduction;

	@Builder
	private User(Long roleId, String oauthId, String provider, String email, String name, String gender,
		LocalDateTime birth, Location location, String profileImgUrl, String introduction) {
		this.roleId = roleId;
		this.provider = provider;
		this.email = email;
		this.oauthId = oauthId;
		this.name = name;
		this.gender = gender;
		this.birth = birth;
		this.location = location;
		this.profileImgUrl = profileImgUrl;
		this.introduction = introduction;
	}

	public static User create(Long roleId, String oauthId, String provider, String email, String name,
		String gender, LocalDateTime birth, Location location, String profileImgUrl, String introduction) {
		return User.builder()
			.roleId(roleId)
			.oauthId(oauthId)
			.provider(provider)
			.email(email)
			.name(name)
			.gender(gender)
			.birth(birth)
			.location(location)
			.profileImgUrl(profileImgUrl)
			.introduction(introduction)
			.build();
	}

	public void updateProfile(String name, String gender, LocalDateTime birth, Location location, String profileImgUrl, String introduction) {
		this.name = name;
		this.gender = gender;
		this.birth = birth;
		this.location = location;
		this.profileImgUrl = profileImgUrl;
		this.introduction = introduction;
	}

	public void leaveUser() {
		this.oauthId = null;
		this.provider = null;
		this.email = null;
		this.name = "deletedUser";
		this.gender = null;
		this.birth = null;
		this.location = null;
		this.profileImgUrl = null;
		this.introduction = null;
		this.softDelete();
	}
}
