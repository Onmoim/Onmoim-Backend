package com.onmoim.server.common.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;

public interface EnumModel {

	@JsonValue
	String getKey();

	String getValue();

}
