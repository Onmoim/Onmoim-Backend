#!/bin/bash

# This script performs simple checkstyle fixes:
# 1. Adds newline at end of files
# 2. Converts spaces to tabs for indentation

# Function to ensure newline at end of file
add_newline_eof() {
  local file="$1"
  
  if [ -f "$file" ]; then
    # Check if file ends with newline
    if [ "$(tail -c 1 "$file" | xxd -p)" != "0a" ]; then
      echo "Adding newline to $file"
      echo "" >> "$file"
    fi
  fi
}

# Function to convert spaces to tabs for indentation
fix_indentation() {
  local file="$1"
  
  # Check if the file contains spaces for indentation
  if grep -q "^    " "$file"; then
    echo "Fixing indentation in $file"
    sed -i '.bak' 's/^    /\t/g' "$file"
    sed -i '.bak' 's/^        /\t\t/g' "$file"
    sed -i '.bak' 's/^            /\t\t\t/g' "$file"
    sed -i '.bak' 's/^                /\t\t\t\t/g' "$file"
  fi
}

# Files to process
FILES_TO_FIX=(
  "src/main/java/com/onmoim/server/post/dto/response/GroupPostResponseDto.java"
  "src/main/java/com/onmoim/server/post/entity/GroupPost.java"
  "src/main/java/com/onmoim/server/post/entity/PostImage.java"
  "src/main/java/com/onmoim/server/post/entity/PostImageId.java"
  "src/main/java/com/onmoim/server/post/repository/GroupPostRepository.java"
  "src/main/java/com/onmoim/server/post/repository/GroupPostRepositoryCustom.java"
  "src/main/java/com/onmoim/server/post/repository/GroupPostRepositoryCustomImpl.java"
  "src/main/java/com/onmoim/server/post/controller/GroupPostController.java"
)

# Process each file
for file in "${FILES_TO_FIX[@]}"; do
  echo "Processing $file..."
  add_newline_eof "$file"
  fix_indentation "$file"
done

echo "Done! Run './gradlew checkstyleMain' to verify fixes." 