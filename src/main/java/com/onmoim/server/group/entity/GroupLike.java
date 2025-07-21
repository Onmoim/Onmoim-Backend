package com.onmoim.server.group.entity;

import static com.onmoim.server.group.entity.GroupLikeStatus.*;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.user.entity.User;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupLike extends BaseEntity {
	@EmbeddedId
	private GroupUserId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("groupId")
	@JoinColumn(name = "group_id")
	private Group group;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	private GroupLikeStatus status;

	public static GroupLike create(Group group, User user, GroupLikeStatus status) {
		GroupLike groupLike = new GroupLike();
		groupLike.group = group;
		groupLike.user = user;
		groupLike.id = new GroupUserId(group.getId(), user.getId());
		groupLike.status = status;
		return groupLike;
	}

	// 상태 업데이트 메서드
	public GroupLikeStatus updateStatus() {
		switch (this.status) {
			case NEW, PENDING -> this.status = LIKE;
			case LIKE -> this.status = PENDING;
		}
		return this.status;
	}
}
