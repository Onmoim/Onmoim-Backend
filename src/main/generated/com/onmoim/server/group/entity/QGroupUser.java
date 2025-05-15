package com.onmoim.server.group.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGroupUser is a Querydsl query type for GroupUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupUser extends EntityPathBase<GroupUser> {

    private static final long serialVersionUID = -664664611L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGroupUser groupUser = new QGroupUser("groupUser");

    public final com.onmoim.server.common.QBaseEntity _super = new com.onmoim.server.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final QGroup group;

    public final QGroupUserId id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final EnumPath<Status> status = createEnum("status", Status.class);

    public final com.onmoim.server.user.entity.QUser user;

    public QGroupUser(String variable) {
        this(GroupUser.class, forVariable(variable), INITS);
    }

    public QGroupUser(Path<? extends GroupUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGroupUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGroupUser(PathMetadata metadata, PathInits inits) {
        this(GroupUser.class, metadata, inits);
    }

    public QGroupUser(Class<? extends GroupUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.group = inits.isInitialized("group") ? new QGroup(forProperty("group"), inits.get("group")) : null;
        this.id = inits.isInitialized("id") ? new QGroupUserId(forProperty("id")) : null;
        this.user = inits.isInitialized("user") ? new com.onmoim.server.user.entity.QUser(forProperty("user")) : null;
    }

}

