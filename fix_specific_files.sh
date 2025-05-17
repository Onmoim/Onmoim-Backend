#!/bin/bash

# This script focuses on fixing specific files with known checkstyle issues
# It uses a more careful, targeted approach

# Add newline at the end of files if missing
add_newline() {
    file=$1
    if [ -f "$file" ] && [ $(tail -c 1 "$file" | wc -l) -eq 0 ]; then
        echo "" >> "$file"
        echo "Added newline to $file"
    fi
}

# Files that need newlines added
NEWLINE_FILES=(
    "src/main/java/com/onmoim/server/post/dto/response/GroupPostResponseDto.java"
    "src/main/java/com/onmoim/server/post/repository/GroupPostRepository.java"
    "src/main/java/com/onmoim/server/post/repository/GroupPostRepositoryCustom.java"
    "src/main/java/com/onmoim/server/post/repository/GroupPostRepositoryCustomImpl.java"
    "src/main/java/com/onmoim/server/post/service/ImagePostService.java"
)

# Fix newline issues
echo "Fixing files missing newlines..."
for file in "${NEWLINE_FILES[@]}"; do
    add_newline "$file"
done

# Fix specific file: GroupPostController.java - duplicate import
echo "Fixing duplicate import in GroupPostController.java..."
GP_CONTROLLER="src/main/java/com/onmoim/server/post/controller/GroupPostController.java"
# Create a temporary file without the duplicate import
grep -v -A1 "^import lombok.RequiredArgsConstructor;$" "$GP_CONTROLLER" | grep -v "^$" > "$GP_CONTROLLER.tmp"
# Replace the original
mv "$GP_CONTROLLER.tmp" "$GP_CONTROLLER"
add_newline "$GP_CONTROLLER"

# Fix specific file: GroupPostResponseDto.java - import order
echo "Fixing import order in GroupPostResponseDto.java..."
GP_RESPONSE_DTO="src/main/java/com/onmoim/server/post/dto/response/GroupPostResponseDto.java"
# Create a temporary file with proper import order
cat > "$GP_RESPONSE_DTO.tmp" << 'EOF'
package com.onmoim.server.post.dto.response;

import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.PostImage;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모임 게시글 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostResponseDto {

	private Long id;
	private Long groupId;
	private Long authorId;
	private String authorName;
	private String authorProfileImage;
	private String title;
	private String content;
	private GroupPostType type;
	private LocalDateTime createdDate;
	private LocalDateTime modifiedDate;
	private Long viewCount;
	private List<String> imageUrls;

	public static GroupPostResponseDto of(GroupPost post, List<String> imageUrls) {
		return GroupPostResponseDto.builder()
				.id(post.getId())
				.groupId(post.getGroup().getId())
				.authorId(post.getAuthor().getId())
				.authorName(post.getAuthor().getName())
				.authorProfileImage(post.getAuthor().getProfileImageUrl())
				.title(post.getTitle())
				.content(post.getContent())
				.type(post.getType())
				.createdDate(post.getCreatedDate())
				.modifiedDate(post.getModifiedDate())
				.viewCount(post.getViewCount())
				.imageUrls(imageUrls)
				.build();
	}
}
EOF
# Replace the original
mv "$GP_RESPONSE_DTO.tmp" "$GP_RESPONSE_DTO"

# Fix specific file: GroupPostRepository.java
echo "Fixing import order in GroupPostRepository.java..."
GP_REPO="src/main/java/com/onmoim/server/post/repository/GroupPostRepository.java"
cat > "$GP_REPO.tmp" << 'EOF'
package com.onmoim.server.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.post.entity.GroupPost;

public interface GroupPostRepository
		extends JpaRepository<GroupPost, Long>,
		GroupPostRepositoryCustom {
}
EOF
# Replace the original
mv "$GP_REPO.tmp" "$GP_REPO"

# Fix specific file: GroupPostRepositoryCustom.java
echo "Fixing import order in GroupPostRepositoryCustom.java..."
GP_REPO_CUSTOM="src/main/java/com/onmoim/server/post/repository/GroupPostRepositoryCustom.java"
cat > "$GP_REPO_CUSTOM.tmp" << 'EOF'
package com.onmoim.server.post.repository;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;

/**
 * 모임 게시글을 위한 커스텀 레포지토리 인터페이스 (Querydsl 기능 확장용)
 */
public interface GroupPostRepositoryCustom {
	CursorPageResponseDto<GroupPost> findPosts(
			Group group,
			GroupPostType type,
			Long cursorId,
			int size
	);
}
EOF
# Replace the original
mv "$GP_REPO_CUSTOM.tmp" "$GP_REPO_CUSTOM"

echo "Fixes applied successfully." 