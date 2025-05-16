package com.onmoim.server.group.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class GroupUserId implements Serializable {
	@Column(name = "group_id")
	private final Long groupId;
	@Column(name = "user_id")
	private final Long userId;

	protected GroupUserId() {
		this.groupId = null;
		this.userId = null;
	}
}
