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
import jakarta.persistence.Version;
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

	@Version
	private Long version;

	public static GroupUser create(Group group, User user, Status status) {
		GroupUser groupUser = new GroupUser();
		groupUser.group = group;
		groupUser.user = user;
		groupUser.status = status;
		groupUser.id = new GroupUserId(group.getId(), user.getId());
		return groupUser;
	}

	public void joinValidate() {
		switch (status) {
			case OWNER:
			case MEMBER:
				throw new CustomException(GROUP_ALREADY_JOINED);
			case BAN:
				throw new CustomException(GROUP_BANNED_MEMBER);
		}
	}

	public void updateStatus(Status status) {
		this.status = status;
	}

	/**
	 * 북마크(찜) 상태 변경
	 * 모임장, 회원, 벤 -> 예외
	 * 북마크 -> 임시
	 * 임시 -> 북마크
	 */
	public void like() {
		switch (status) {
			case OWNER:
			case MEMBER:
				throw new CustomException(GROUP_ALREADY_JOINED);
			case BAN:
				throw new CustomException(GROUP_BANNED_MEMBER);
			case BOOKMARK:
				this.status = Status.PENDING;
				break;
			case PENDING:
				this.status = Status.BOOKMARK;
		}
	}

	public boolean isOwner() {
		return status == Status.OWNER;
	}

	public boolean isMember() {
		return status == Status.MEMBER;
	}

	public boolean isJoined() {
		return isOwner() || isMember();
	}
}
