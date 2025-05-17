package com.onmoim.server.post.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.common.image.entity.Image;
import com.onmoim.server.common.image.repository.ImageRepository;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.repository.PostImageRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImagePostService {

    private final PostImageRepository postImageRepository;
    private final ImageRepository imageRepository;

    public List<PostImage> findAllByPost(GroupPost post) {
        return postImageRepository.findAllByPost(post);
    }

    @Transactional
    public void softDeleteAllByPostId(Long postId) {
        postImageRepository.softDeleteAllByPostId(postId);
    }

    @Transactional
    public List<PostImage> saveImages(
            List<Image> images,
            GroupPost post
    ) {
        if (images.isEmpty()) {
            return new ArrayList<>();
        }

        imageRepository.saveAll(images);

        List<PostImage> postImages = new ArrayList<>();
        for (Image image : images) {
            PostImage postImage = PostImage.builder()
                    .post(post)
                    .image(image)
                    .build();

            postImages.add(postImage);
            post.addImage(postImage);
        }

        postImageRepository.saveAll(postImages);

        return postImages;
    }
}