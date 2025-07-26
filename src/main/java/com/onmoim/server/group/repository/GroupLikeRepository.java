package com.onmoim.server.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.group.entity.GroupLike;
import com.onmoim.server.group.entity.GroupUserId;

public interface GroupLikeRepository extends JpaRepository<GroupLike, GroupUserId>, GroupLikeRepositoryCustom {

}
