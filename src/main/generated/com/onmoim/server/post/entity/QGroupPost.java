package com.onmoim.server.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGroupPost is a Querydsl query type for GroupPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupPost extends EntityPathBase<GroupPost> {

    private static final long serialVersionUID = -1282074317L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGroupPost groupPost = new QGroupPost("groupPost");

    public final com.onmoim.server.common.QBaseEntity _super = new com.onmoim.server.common.QBaseEntity(this);

    public final com.onmoim.server.user.entity.QUser author;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final com.onmoim.server.group.entity.QGroup group;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath title = createString("title");

    public final EnumPath<GroupPostType> type = createEnum("type", GroupPostType.class);

    public QGroupPost(String variable) {
        this(GroupPost.class, forVariable(variable), INITS);
    }

    public QGroupPost(Path<? extends GroupPost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGroupPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGroupPost(PathMetadata metadata, PathInits inits) {
        this(GroupPost.class, metadata, inits);
    }

    public QGroupPost(Class<? extends GroupPost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.onmoim.server.user.entity.QUser(forProperty("author")) : null;
        this.group = inits.isInitialized("group") ? new com.onmoim.server.group.entity.QGroup(forProperty("group"), inits.get("group")) : null;
    }

}

