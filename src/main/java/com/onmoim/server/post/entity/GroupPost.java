package com.onmoim.server.post.entity;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.image.entity.Image;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 모임 게시글 엔티티
 *
 * TODO: 조회수, 좋아요 수 필드 추가 (향후 구현)
 * TODO: 댓글 관련 연관관계 추가 (향후 구현)
 * TODO: 게시글 카테고리 관계 추가 (향후 구현)
 */
@Entity
@Getter
@Table(
    name = "post", 
    indexes = {
        @Index(name = "idx_post_group_type_isdeleted", columnList = "group_id,type,isDeleted"),
        @Index(name = "idx_post_author_isdeleted", columnList = "author_id,isDeleted")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private GroupPostType type;

    @OneToMany(mappedBy = "post", orphanRemoval = true)
    private List<PostImage> postImages = new ArrayList<>();

    @Builder
    public GroupPost(Group group, User author, String title, String content, GroupPostType type) {
        this.group = group;
        this.author = author;
        this.title = title;
        this.content = content;
        this.type = type;
    }

    /**
     * 게시글 정보 업데이트
     */
    public void update(String title, String content, GroupPostType type) {
        this.title = title;
        this.content = content;
        this.type = type;
    }

    /**
     * 이미지 연결 추가
     */
    public void addImage(PostImage postImage) {
        this.postImages.add(postImage);
    }

    /**
     * 게시글 소프트 삭제 처리
     * BaseEntity의 소프트 삭제 메서드를 호출
     */
    public void softDelete() {
        super.softDelete();
    }

    // TODO: 조회수 증가 메서드 추가 (향후 구현)
    // TODO: 좋아요 추가/취소 메서드 추가 (향후 구현)
}
