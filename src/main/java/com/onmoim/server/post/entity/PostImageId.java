package com.onmoim.server.post.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * 게시글 이미지 복합키 클래스
 */
public class PostImageId implements Serializable {
	private Long post;
	private Long image;

	public PostImageId() {
	}

	public PostImageId(Long post, Long image) {
		this.post = post;
		this.image = image;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		PostImageId that = (PostImageId) object;
		return Objects.equals(post, that.post) && Objects.equals(image, that.image);
	}

	@Override
	public int hashCode() {
		return Objects.hash(post, image);
	}
}
