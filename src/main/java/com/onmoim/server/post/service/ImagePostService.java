package com.onmoim.server.post.service;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImagePostService {

	private final PostImageRepository postImageRepository;

	/**
	 * 게시글에 속한 모든 이미지 조회
	 */
	public List<PostImage> findAllByPost(GroupPost post) {
		return postImageRepository.findAllByPost(post);
	}

	/**
	 * 이미지와 게시글 이미지 함께 저장
	 */
	@Transactional
	public PostImage saveImageAndPostImage(GroupPost post) {

		// 게시글 이미지 생성 및 저장
		PostImage postImage = PostImage.builder()
			.post(post)
			.build();

		postImageRepository.save(postImage);
		return postImage;
	}
}
