package com.onmoim.server.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.user.entity.User;

/**
 * 모임 게시글 엔티티
 */
@Entity
@Getter
@Table(
        name = "post",
        indexes = {
                @Index(
                        name = "idx_post_group_type_deleted",
                        columnList = "group_id,type,deleted_date"
                ),
                @Index(
                        name = "idx_post_author_deleted",
                        columnList = "author_id,deleted_date"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private GroupPostType type;

    @Builder
    public GroupPost(
            Group group,
            User author,
            String title,
            String content,
            GroupPostType type
    ) {
        this.group = group;
        this.author = author;
        this.title = title;
        this.content = content;
        this.type = type;
    }

    public void update(
            String title,
            String content,
            GroupPostType type
    ) {
        this.title = title;
        this.content = content;
        this.type = type;
    }

    public void softDelete() {
        super.softDelete();
    }
}
