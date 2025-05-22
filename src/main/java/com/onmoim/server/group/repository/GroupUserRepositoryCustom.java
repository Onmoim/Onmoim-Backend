package com.onmoim.server.group.repository;

import java.util.List;

import com.onmoim.server.group.entity.GroupUser;

public interface GroupUserRepositoryCustom {
	List<GroupUser> findGroupUserAndMembers(Long groupId);
}
