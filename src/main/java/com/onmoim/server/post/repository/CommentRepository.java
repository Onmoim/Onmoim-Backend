package com.onmoim.server.post.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.user.entity.User;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	/**
	 * 특정 게시글의 부모댓글 조회
	 */
	@Query("""
		SELECT c FROM Comment c
		JOIN FETCH c.author
		WHERE c.post = :post
		AND c.parent IS NULL
		AND c.deletedDate IS NULL
		AND (:cursor IS NULL OR c.id < :cursor)
		ORDER BY c.id DESC
		""")
	List<Comment> findParentCommentsByPost(
		@Param("post") GroupPost post,
		@Param("cursor") Long cursor
	);

	/**
	 * 특정 부모댓글의 답글 조회
	 */
	@Query("""
		SELECT c FROM Comment c
		JOIN FETCH c.author
		WHERE c.parent = :parent
		AND c.deletedDate IS NULL
		AND (:cursor IS NULL OR c.id < :cursor)
		ORDER BY c.id DESC
		""")
	List<Comment> findRepliesByParent(
		@Param("parent") Comment parent,
		@Param("cursor") Long cursor
	);

	/**
	 * 특정 댓글 조회 (작성자 정보 포함)
	 */
	@Query("""
		SELECT c FROM Comment c
		JOIN FETCH c.author
		WHERE c.id = :commentId
		""")
	Optional<Comment> findByIdWithAuthor(@Param("commentId") Long commentId);

	/**
	 * 여러 부모댓글의 답글 개수를 한 번에 조회 (타입 안전한 Projection 사용)
	 */
	@Query("""
		SELECT c.parent.id as parentId, COUNT(c) as replyCount
		FROM Comment c
		WHERE c.parent.id IN :parentIds
		AND c.deletedDate IS NULL
		GROUP BY c.parent.id
		""")
	List<CommentReplyCountProjection> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

	/**
	 * 댓글 답글 수 집계를 위한 인터페이스
	 */
	interface CommentReplyCountProjection {
		Long getParentId();
		Long getReplyCount();
	}


	/**
	 * 작성자와 댓글 ID로 댓글 조회 (권한 확인용)
	 */
	@Query("""
		SELECT c FROM Comment c
		WHERE c.id = :commentId
		AND c.author = :author
		""")
	Optional<Comment> findByIdAndAuthor(
		@Param("commentId") Long commentId,
		@Param("author") User author
	);

}
