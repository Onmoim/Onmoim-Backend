#!/bin/bash

# Let's fix the 3 most problematic files manually
# 1. GroupPostController.java
# 2. GroupPost.java  
# 3. PostImage.java

# Fix GroupPostController.java
echo "Fixing GroupPostController.java..."
controller_file="src/main/java/com/onmoim/server/post/controller/GroupPostController.java"

# Make a backup
cp "$controller_file" "${controller_file}.bak"

# Fix imports in GroupPostController.java - sort them correctly
sed -i '' '1,34d' "$controller_file"
cat > "$controller_file.tmp" << 'EOF'
package com.onmoim.server.post.controller;

import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.service.GroupPostService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.util.List;

import lombok.RequiredArgsConstructor;
EOF

# Append the rest of the file
sed -n '35,$p' "${controller_file}.bak" >> "$controller_file.tmp"
mv "$controller_file.tmp" "$controller_file"

# Fix all indentation to use tabs in the entire file
sed -i '' 's/^    /\t/g' "$controller_file"
sed -i '' 's/^        /\t\t/g' "$controller_file"
sed -i '' 's/^            /\t\t\t/g' "$controller_file"
sed -i '' 's/^                /\t\t\t\t/g' "$controller_file"

# Fix GroupPost.java
echo "Fixing GroupPost.java..."
grouppost_file="src/main/java/com/onmoim/server/post/entity/GroupPost.java"

# Make a backup
cp "$grouppost_file" "${grouppost_file}.bak"

# Fix imports in GroupPost.java
sed -i '' '1,26d' "$grouppost_file"
cat > "$grouppost_file.tmp" << 'EOF'
package com.onmoim.server.post.entity;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
EOF

# Append the rest of the file
sed -n '27,$p' "${grouppost_file}.bak" >> "$grouppost_file.tmp"
mv "$grouppost_file.tmp" "$grouppost_file"

# Fix all indentation to use tabs in the entire file
sed -i '' 's/^    /\t/g' "$grouppost_file"
sed -i '' 's/^        /\t\t/g' "$grouppost_file"
sed -i '' 's/^            /\t\t\t/g' "$grouppost_file"
sed -i '' 's/^                /\t\t\t\t/g' "$grouppost_file"

# Fix PostImage.java
echo "Fixing PostImage.java..."
postimage_file="src/main/java/com/onmoim/server/post/entity/PostImage.java"

# Make a backup
cp "$postimage_file" "${postimage_file}.bak"

# Fix imports in PostImage.java
sed -i '' '1,17d' "$postimage_file"
cat > "$postimage_file.tmp" << 'EOF'
package com.onmoim.server.post.entity;

import com.onmoim.server.common.BaseEntity;
import com.onmoim.server.common.image.entity.Image;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
EOF

# Append the rest of the file
sed -n '18,$p' "${postimage_file}.bak" >> "$postimage_file.tmp"
mv "$postimage_file.tmp" "$postimage_file"

# Fix all indentation to use tabs in the entire file
sed -i '' 's/^    /\t/g' "$postimage_file"
sed -i '' 's/^        /\t\t/g' "$postimage_file"
sed -i '' 's/^            /\t\t\t/g' "$postimage_file"
sed -i '' 's/^                /\t\t\t\t/g' "$postimage_file"

echo "Fixes applied to specific files." 