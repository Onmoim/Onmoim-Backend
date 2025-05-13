package com.onmoim.server.group.entity;

import static com.onmoim.server.common.exception.ErrorCode.*;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.exception.CustomException;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupUser extends BaseEntity {
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
	private Status status;

	public static GroupUser create(Group group, User user, Status status) {
		GroupUser groupUser = new GroupUser();
		groupUser.group = group;
		groupUser.user = user;
		groupUser.status = status;
		return groupUser;
	}

	//북마크(찜)한 경우 정상 가입, 차단 상태거나 가입된 경우 예외 처리
	public void joinGroup() {
		switch (status) {
			case BOOKMARK:
				this.status = Status.MEMBER;
				break;
			case BAN:
				throw new CustomException(GROUP_BANNED_MEMBER);
			default:
				throw new CustomException(GROUP_ALREADY_JOINED);
		}
	}
}
