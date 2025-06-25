package com.onmoim.server.post.constant;

/**
 * Post 도메인 공통 상수
 */
public final class PostConstants {

    private PostConstants() {
    }

    // 페이징 관련
    /**
     * 커서 기반 페이징에서 한 번에 조회할 데이터 개수
     * (무한 스크롤에서 한 번에 로드되는 아이템 수)
     */
    public static final int CURSOR_PAGE_SIZE = 20;

    // 이미지 관련

    /**
     * 게시글당 최대 이미지 업로드 개수
     */
    public static final int MAX_IMAGES_PER_POST = 5;

    /**
     * 이미지 업로드 S3 경로
     */
    public static final String IMAGE_UPLOAD_PATH = "posts";

    /**
     * 댓글 최대 깊이 (부모댓글 + 답글 2단계만 허용)
     */
    public static final int MAX_COMMENT_DEPTH = 2;
}
