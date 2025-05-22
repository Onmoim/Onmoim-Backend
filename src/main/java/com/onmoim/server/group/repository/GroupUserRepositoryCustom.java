package com.onmoim.server.group.repository;

import java.util.List;

import com.onmoim.server.group.entity.GroupUser;

public interface GroupUserRepositoryCustom {
	Long countGroupMembers(Long groupId);
	List<GroupUser> findGroupUsers(Long groupId, Long cursorId, int size);
}
