package com.onmoim.server.group.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Embeddable
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class GroupUserId implements Serializable {
	private final Long groupId;
	private final Long userId;

	protected GroupUserId() {
		this(null, null);
	}
}
