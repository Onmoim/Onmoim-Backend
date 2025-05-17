#!/bin/bash

# This script fixes common checkstyle issues:
# 1. Adds newlines at end of files
# 2. Converts spaces to tabs for indentation
# 3. Fixes import order

# Make sure we're in the right directory
cd "$(dirname "$0")"

# Files that need to be fixed
JAVA_FILES=$(find src/main/java -name "*.java")

# Fix files ending without newlines
fix_missing_newlines() {
  echo "Fixing missing newlines at end of files..."
  for file in $JAVA_FILES; do
    if [ -f "$file" ] && [ "$(tail -c 1 "$file" | wc -l)" -eq 0 ]; then
      echo "Adding newline to $file"
      echo "" >> "$file"
    fi
  done
}

# Fix indentation (convert spaces to tabs)
fix_indentation() {
  echo "Fixing indentation (converting spaces to tabs)..."
  for file in $JAVA_FILES; do
    if grep -q "^    " "$file"; then
      echo "Converting spaces to tabs in $file"
      # Make a backup
      cp "$file" "${file}.bak"
      # Replace 4 spaces with a tab at the beginning of lines
      sed -i '' 's/^    /\t/g' "$file"
      # Replace 8 spaces with two tabs
      sed -i '' 's/^        /\t\t/g' "$file"
      # Replace 12 spaces with three tabs
      sed -i '' 's/^            /\t\t\t/g' "$file"
    fi
  done
}

# Main execution
echo "Starting checkstyle fixes..."
fix_missing_newlines
fix_indentation
echo "Fixes applied. Please check the results." 