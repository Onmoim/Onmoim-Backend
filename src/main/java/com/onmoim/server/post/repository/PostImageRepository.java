package com.onmoim.server.post.repository;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.entity.PostImageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, PostImageId> {
    List<PostImage> findAllByPost(GroupPost post);

    @Modifying
    @Query("UPDATE PostImage pi SET pi.isDeleted = true WHERE pi.post.id = :postId")
    void softDeleteAllByPostId(@Param("postId") Long postId);
    
    /**
     * 여러 게시글 ID에 해당하는 이미지들을 일괄 조회 (소프트 삭제되지 않은 것만)
     */
    @Query("SELECT pi FROM PostImage pi WHERE pi.post.id IN :postIds AND pi.isDeleted = false")
    List<PostImage> findByPostIdInAndIsDeletedFalse(@Param("postIds") List<Long> postIds);
    
    /**
     * 단일 게시글 ID에 해당하는 이미지들 조회 (소프트 삭제되지 않은 것만)
     */
    @Query("SELECT pi FROM PostImage pi WHERE pi.post.id = :postId AND pi.isDeleted = false")
    List<PostImage> findByPostIdAndIsDeletedFalse(@Param("postId") Long postId);
}
