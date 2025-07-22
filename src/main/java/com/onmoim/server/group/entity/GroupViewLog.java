package com.onmoim.server.group.entity;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_view_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupViewLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	private Group group;

	@Column(nullable = false)
	private Long viewCount = 0L;

	public static GroupViewLog create(User user, Group group) {
		GroupViewLog groupViewLog = new GroupViewLog();
		groupViewLog.user = user;
		groupViewLog.group = group;
		groupViewLog.viewCount = 1L;
		return groupViewLog;
	}

	public void markViewed() {
		this.viewCount += 1;
	}

}
