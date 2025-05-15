package com.onmoim.server.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserCategoryId is a Querydsl query type for UserCategoryId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QUserCategoryId extends BeanPath<UserCategoryId> {

    private static final long serialVersionUID = 920252635L;

    public static final QUserCategoryId userCategoryId = new QUserCategoryId("userCategoryId");

    public final NumberPath<Long> categoryId = createNumber("categoryId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserCategoryId(String variable) {
        super(UserCategoryId.class, forVariable(variable));
    }

    public QUserCategoryId(Path<? extends UserCategoryId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserCategoryId(PathMetadata metadata) {
        super(UserCategoryId.class, metadata);
    }

}

