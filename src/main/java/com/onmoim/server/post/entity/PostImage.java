package com.onmoim.server.post.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.image.entity.Image;
import com.onmoim.server.post.entity.GroupPost;

/**
 * 모임 게시글 이미지 연결 엔티티
 */
@Entity
@Getter
@IdClass(PostImageId.class)
@Table(name = "post_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private GroupPost post;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @Builder
    public PostImage(
            GroupPost post,
            Image image
    ) {
        this.post = post;
        this.image = image;
    }
}
