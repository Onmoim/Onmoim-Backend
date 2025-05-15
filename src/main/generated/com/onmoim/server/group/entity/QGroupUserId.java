package com.onmoim.server.group.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGroupUserId is a Querydsl query type for GroupUserId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QGroupUserId extends BeanPath<GroupUserId> {

    private static final long serialVersionUID = 1207438296L;

    public static final QGroupUserId groupUserId = new QGroupUserId("groupUserId");

    public final NumberPath<Long> groupId = createNumber("groupId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QGroupUserId(String variable) {
        super(GroupUserId.class, forVariable(variable));
    }

    public QGroupUserId(Path<? extends GroupUserId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGroupUserId(PathMetadata metadata) {
        super(GroupUserId.class, metadata);
    }

}

