package com.onmoim.server.group.repository;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupViewLog;
import com.onmoim.server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupViewLogRepository extends JpaRepository<GroupViewLog, Long>, GroupViewLogRepositoryCustom {

	Optional<GroupViewLog> findByUserAndGroup(User user, Group group);

}
