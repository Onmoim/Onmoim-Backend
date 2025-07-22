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
import lombok.ToString;

@Entity
@Getter
@ToString(of = {"group", "user"})
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

	public void checkOwner() {
		if(!isOwner()) throw new CustomException(GROUP_FORBIDDEN);
	}

	public void checkMember() {
		if(!isMember()) throw new CustomException(GROUP_FORBIDDEN);
	}

	public void checkGroupMember(){
		if(!isJoined()) throw new CustomException(NOT_GROUP_MEMBER);
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

	// 강퇴
	public void ban() {
		this.updateStatus(Status.BAN);
	}

	public void deleteGroupUser() {
		this.status = Status.DELETED;
		this.softDelete();
	}

}
