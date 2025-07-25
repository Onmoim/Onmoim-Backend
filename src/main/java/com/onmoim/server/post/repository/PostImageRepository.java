package com.onmoim.server.post.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.entity.PostImageId;

public interface PostImageRepository extends JpaRepository<PostImage, PostImageId> {
    List<PostImage> findAllByPost(GroupPost post);

    @Modifying
    @Query("UPDATE PostImage pi SET pi.deletedDate = CURRENT_TIMESTAMP WHERE pi.post.id = :postId")
    void softDeleteAllByPostId(@Param("postId") Long postId);

    /**
     * 여러 게시글 ID에 해당하는 이미지들을 일괄 조회
     */
    @Query("SELECT pi FROM PostImage pi WHERE pi.post.id IN :postIds")
    List<PostImage> findByPostIdIn(@Param("postIds") List<Long> postIds);

}
