package com.onmoim.server.oauth.enumeration;

import com.onmoim.server.common.enumeration.EnumModel;

/**
 * 회원가입 상태
 */
public enum SignupStatus implements EnumModel {

	EXISTS("가입"),
	NOT_EXISTS("미가입"),
	NO_CATEGORY("관심사 설정 필요");

	private final String value;

	SignupStatus(String value) {
		this.value = value;
	}

	@Override
	public String getKey() {
		return name();
	}

	@Override
	public String getValue() {
		return this.value;
	}

}
