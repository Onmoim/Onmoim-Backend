package com.onmoim.server.group.repository;

import com.onmoim.server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.group.entity.GroupLike;
import com.onmoim.server.group.entity.GroupUserId;

import java.util.List;

public interface GroupLikeRepository extends JpaRepository<GroupLike, GroupUserId>, GroupLikeRepositoryCustom {

    List<GroupLike> user(User user);

}
