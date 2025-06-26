package com.onmoim.server.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.user.entity.User;

/**
 * 게시글 좋아요 엔티티
 */
@Entity
@Getter
@Table(
	name = "post_likes",
	indexes = {
		@Index(name = "idx_post_likes_batch", columnList = "post_id"),
		@Index(name = "idx_post_likes_user", columnList = "user_id,post_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private GroupPost post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Builder
	public PostLike(GroupPost post, User user) {
		this.post = post;
		this.user = user;
	}

	/**
	 * 좋아요 활성화
	 */
	public void active() {
		super.restore();
	}

	/**
	 * 좋아요 취소
	 */
	public void cancel() {
		super.softDelete();
	}

	/**
	 * 좋아요 활성 상태인지 확인
	 */
	public boolean isActive() {
		return getDeletedDate() == null;
	}
}
