package com.onmoim.server.common.image.service;

import com.onmoim.server.common.image.entity.Image;
import com.onmoim.server.common.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageQueryService {
    
    private final ImageRepository imageRepository;
    
    /**
     * 이미지 저장
     */
    @Transactional
    public Image save(Image image) {
        return imageRepository.save(image);
    }
} 