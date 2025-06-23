package com.onmoim.server.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.user.entity.User;

@Entity
@Getter
@Table(
    name = "comment",
    indexes = {
        @Index(
            name = "idx_comment_post_parent_deleted",
            columnList = "post_id,parent_id,deleted_date"
        ),
        @Index(
            name = "idx_comment_author_deleted",
            columnList = "author_id,deleted_date"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private GroupPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    public Comment(GroupPost post, User author, Comment parent, String content) {
        this.post = post;
        this.author = author;
        this.parent = parent;
        this.content = content;
    }

    /**
     * 부모 댓글인지 확인
     */
    public boolean isParentComment() {
        return this.parent == null;
    }

    /**
     * 답글인지 확인
     */
    public boolean isReply() {
        return this.parent != null;
    }

    /**
     * 댓글 수정
     */
    public void updateContent(String content) {
        this.content = content;
    }
}
