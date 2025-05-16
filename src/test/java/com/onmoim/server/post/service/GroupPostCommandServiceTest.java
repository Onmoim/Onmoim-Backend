package com.onmoim.server.post.service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.image.entity.Image;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.service.GroupQueryService;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupPostCommandServiceTest {

    @Mock
    private GroupQueryService groupQueryService;

    @Mock
    private GroupPostQueryService groupPostQueryService;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private ImagePostService imagePostService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private GroupPostCommandService groupPostCommandService;

    private User testUser;
    private Group testGroup;
    private GroupPost testPost;
    private GroupPostRequestDto requestDto;
    private List<MultipartFile> mockFiles;

    @BeforeEach
    void setUp() {
        // User 객체 생성
        testUser = User.builder()
                .name("testUser")
                .build();
        // User 객체 ID 설정
        setId(testUser, 1L);

        testGroup = Group.groupCreateBuilder()
                .name("testGroup")
                .capacity(10)
                .build();
        // Group 객체 ID 설정
        setId(testGroup, 1L);

        // GroupPost 객체 생성
        testPost = GroupPost.builder()
                .title("Test Title")
                .content("Test Content")
                .type(GroupPostType.FREE)
                .group(testGroup)
                .author(testUser)
                .build();
        // GroupPost 객체 ID 설정
        setId(testPost, 1L);

        // GroupPostRequestDto 객체 생성
        requestDto = GroupPostRequestDto.builder()
                .title("Test Title")
                .content("Test Content")
                .type(GroupPostType.FREE)
                .build();

        // MockMultipartFile 리스트 생성
        mockFiles = Arrays.asList(
                new MockMultipartFile("file", "test1.jpg", "image/jpeg", "test1".getBytes()),
                new MockMultipartFile("file", "test2.jpg", "image/jpeg", "test2".getBytes())
        );
    }

    private void setId(Object entity, Long id) {
    }

    @Test
    @DisplayName("게시글 생성 성공 테스트")
    void createPostSuccess() {
        // given
        Long groupId = 1L;
        Long userId = 1L;

        when(groupQueryService.getById(groupId)).thenReturn(testGroup);
        when(userQueryService.findById(userId)).thenReturn(testUser);
        doNothing().when(groupPostQueryService).validateGroupMembership(groupId, userId);
        when(groupPostQueryService.save(any(GroupPost.class))).thenReturn(testPost);

        FileUploadResponseDto uploadResponse = FileUploadResponseDto.builder()
                .fileName("test.jpg")
                .fileUrl("https://test-url.com/test.jpg")
                .build();
        when(fileStorageService.uploadFile(any(MultipartFile.class), eq("posts"))).thenReturn(uploadResponse);

        Image mockImage = Image.builder().imageUrl("https://test-url.com/test.jpg").build();
        PostImage mockPostImage = PostImage.builder().image(mockImage).post(testPost).build();
        when(imagePostService.saveImageAndPostImage(any(Image.class), any(GroupPost.class))).thenReturn(mockPostImage);

        // when
        GroupPostResponseDto result = groupPostCommandService.createPost(groupId, userId, requestDto, mockFiles);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getContent()).isEqualTo("Test Content");
        verify(groupPostQueryService).validateGroupMembership(groupId, userId);
        verify(groupPostQueryService).save(any(GroupPost.class));
        verify(fileStorageService, times(2)).uploadFile(any(MultipartFile.class), eq("posts"));
    }

    @Test
    @DisplayName("이미지 개수 초과 시 예외 발생 테스트")
    void createPostWithTooManyImages() {
        // given
        Long groupId = 1L;
        Long userId = 1L;

        List<MultipartFile> tooManyFiles = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tooManyFiles.add(new MockMultipartFile("file", "test" + i + ".jpg", "image/jpeg", ("test" + i).getBytes()));
        }

        when(groupQueryService.getById(groupId)).thenReturn(testGroup);
        when(userQueryService.findById(userId)).thenReturn(testUser);
        doNothing().when(groupPostQueryService).validateGroupMembership(groupId, userId);

        // when & then
        assertThrows(CustomException.class, () ->
            groupPostCommandService.createPost(groupId, userId, requestDto, tooManyFiles)
        );
    }

    @Test
    @DisplayName("게시글 수정 성공 테스트")
    void updatePostSuccess() {
        // given
        Long groupId = 1L;
        Long postId = 1L;
        Long userId = 1L;

        when(groupQueryService.getById(groupId)).thenReturn(testGroup);
        doNothing().when(groupPostQueryService).validateGroupMembership(groupId, userId);
        when(groupPostQueryService.findById(postId)).thenReturn(testPost);
        doNothing().when(groupPostQueryService).validatePostBelongsToGroup(testPost, groupId);
        doNothing().when(groupPostQueryService).validatePostAuthor(testPost, userId);

        FileUploadResponseDto uploadResponse = FileUploadResponseDto.builder()
                .fileName("test.jpg")
                .fileUrl("https://test-url.com/test.jpg")
                .build();
        when(fileStorageService.uploadFile(any(MultipartFile.class), eq("posts"))).thenReturn(uploadResponse);

        List<PostImage> existingImages = new ArrayList<>();
        when(imagePostService.findAllByPost(testPost)).thenReturn(existingImages);

        Image mockImage = Image.builder().imageUrl("https://test-url.com/test.jpg").build();
        PostImage mockPostImage = PostImage.builder().image(mockImage).post(testPost).build();
        when(imagePostService.saveImageAndPostImage(any(Image.class), any(GroupPost.class))).thenReturn(mockPostImage);

        // when
        GroupPostResponseDto result = groupPostCommandService.updatePost(groupId, postId, userId, requestDto, mockFiles);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getContent()).isEqualTo("Test Content");
        verify(groupPostQueryService).validateGroupMembership(groupId, userId);
        verify(groupPostQueryService).findById(postId);
        verify(groupPostQueryService).validatePostBelongsToGroup(testPost, groupId);
        verify(groupPostQueryService).validatePostAuthor(testPost, userId);
        verify(fileStorageService, times(2)).uploadFile(any(MultipartFile.class), eq("posts"));
    }

    @Test
    @DisplayName("게시글 삭제 성공 테스트")
    void deletePostSuccess() {
        // given
        Long groupId = 1L;
        Long postId = 1L;
        Long userId = 1L;

        when(groupQueryService.getById(groupId)).thenReturn(testGroup);
        doNothing().when(groupPostQueryService).validateGroupMembership(groupId, userId);
        when(groupPostQueryService.findById(postId)).thenReturn(testPost);
        doNothing().when(groupPostQueryService).validatePostBelongsToGroup(testPost, groupId);
        doNothing().when(groupPostQueryService).validatePostAuthor(testPost, userId);

        List<PostImage> postImages = new ArrayList<>();
        PostImage postImage = PostImage.builder().image(Image.builder().build()).post(testPost).build();
        postImages.add(postImage);
        when(imagePostService.findAllByPost(testPost)).thenReturn(postImages);

        // when
        groupPostCommandService.deletePost(groupId, postId, userId);

        // then
        verify(groupPostQueryService).validateGroupMembership(groupId, userId);
        verify(groupPostQueryService).findById(postId);
        verify(groupPostQueryService).validatePostBelongsToGroup(testPost, groupId);
        verify(groupPostQueryService).validatePostAuthor(testPost, userId);
        verify(imagePostService).findAllByPost(testPost);
    }
}
