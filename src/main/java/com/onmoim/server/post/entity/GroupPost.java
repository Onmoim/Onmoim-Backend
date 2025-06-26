package com.onmoim.server.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.user.entity.User;

/**
 * 모임 게시글 엔티티
 */
// 현재 type 컬럼은 카더널리티가 낮지만,
// 공지만 보기, 자유게시판만 보기 같은 기능이 많습니다.
// group_id, type, deleted_date를 복합 인덱스로 설정했는데,
// 이렇게 type을 포함시키는 것이 실질적인 성능 향상에 도움이 될까요?
@Entity
@Getter
@Table(name = "post", indexes = {
	@Index(name = "idx_post_group_type_deleted", columnList = "group_id,type,deleted_date"),
	@Index(name = "idx_post_author_deleted", columnList = "author_id,deleted_date")
})
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

    /**
     * 게시글 수정
     */
    public void update(
            String title,
            String content,
            GroupPostType type
    ) {
        this.title = title;
        this.content = content;
        this.type = type;
    }

    /**
     * 게시글 작성자 검증
     */
    public void validateAuthor(Long userId) {
        if (!this.author.getId().equals(userId)) {
            throw new CustomException(ErrorCode.DENIED_UNAUTHORIZED_USER);
        }
    }

    /**
     * 게시글 그룹 소속 검증
     */
    public void validateBelongsToGroup(Long groupId) {
        if (!this.group.getId().equals(groupId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
    }
}
