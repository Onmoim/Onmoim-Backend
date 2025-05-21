package com.onmoim.server.post.repository;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageQueryService {
    
    private final PostImageRepository postImageRepository;
    
    /**
     * 게시글에 속한 모든 이미지 조회
     */
    public List<PostImage> findAllByPost(GroupPost post) {
        return postImageRepository.findAllByPost(post);
    }
    
    /**
     * 게시글 이미지 저장
     */
    @Transactional
    public PostImage save(PostImage postImage) {
        return postImageRepository.save(postImage);
    }
} 