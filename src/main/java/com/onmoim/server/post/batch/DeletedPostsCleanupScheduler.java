package com.onmoim.server.post.batch;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 삭제된 게시글 및 이미지를 주기적으로 영구 삭제하는 스케줄러
 * TODO: 스케줄링 적용 후 활성화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeletedPostsCleanupScheduler {

}
