#!/bin/bash

# This script performs a more comprehensive fix for checkstyle issues:
# 1. Adds newline at end of files
# 2. Converts spaces to tabs for indentation 
# 3. Fixes nested indentation
# 4. Fixes import order

# Make sure we're in the right directory
cd "$(dirname "$0")"

echo "Starting deeper checkstyle fixes..."

# Fix files ending without newlines
fix_missing_newlines() {
  echo "Fixing missing newlines at end of files..."
  find src/main/java -name "*.java" | while read -r file; do
    if [ -f "$file" ] && [ "$(tail -c 1 "$file" | wc -l)" -eq 0 ]; then
      echo "" >> "$file"
      echo "Added newline to $file"
    fi
  done
}

# Fix indentation more deeply
fix_indentation() {
  echo "Fixing indentation issues..."
  
  find src/main/java -name "*.java" | while read -r file; do
    echo "Processing $file for indentation..."
    
    # Create a backup
    cp "$file" "${file}.bak"
    
    # Use sed to replace indentation more aggressively
    # This will replace all leading spaces with the correct number of tabs
    
    # Replace 16 spaces with 4 tabs
    sed -i '' 's/^                /\t\t\t\t/g' "$file"
    
    # Replace 12 spaces with 3 tabs
    sed -i '' 's/^            /\t\t\t/g' "$file"
    
    # Replace 8 spaces with 2 tabs
    sed -i '' 's/^        /\t\t/g' "$file"
    
    # Replace 4 spaces with 1 tab
    sed -i '' 's/^    /\t/g' "$file"
    
    # Replace 2 spaces with 1 tab (if that's the indentation style in some places)
    sed -i '' 's/^  /\t/g' "$file"
  done
}

# Fix import order in files
fix_import_order() {
  echo "Fixing import order..."
  
  # Read each Java file
  find src/main/java -name "*.java" | while read -r file; do
    # Create temp file
    temp_file="${file}.tmp"
    
    # Look for the package declaration and the first import
    package_line=$(grep -n "^package" "$file" | head -1 | cut -d ":" -f 1)
    first_import=$(grep -n "^import" "$file" | head -1 | cut -d ":" -f 1)
    last_import=$(grep -n "^import" "$file" | tail -1 | cut -d ":" -f 1)
    
    if [[ -z "$package_line" || -z "$first_import" || -z "$last_import" ]]; then
      # Skip files without package or imports
      continue
    fi
    
    # Extract the parts of the file
    head -n "$package_line" "$file" > "$temp_file"
    echo "" >> "$temp_file"  # Add blank line after package
    
    # Extract all imports
    imports=$(sed -n "${first_import},${last_import}p" "$file")
    
    # Sort imports according to the project's conventions:
    # 1. com.onmoim imports
    # 2. External/third-party imports
    # 3. Jakarta/Java imports
    # 4. lombok imports
    
    # Extract various import types
    project_imports=$(echo "$imports" | grep "^import com\.onmoim")
    jakarta_imports=$(echo "$imports" | grep "^import jakarta\.")
    java_imports=$(echo "$imports" | grep "^import java\.")
    spring_imports=$(echo "$imports" | grep "^import org\.springframework")
    other_external_imports=$(echo "$imports" | grep "^import " | grep -v "^import com\.onmoim" | grep -v "^import jakarta\." | grep -v "^import java\." | grep -v "^import org\.springframework" | grep -v "^import lombok\.")
    lombok_imports=$(echo "$imports" | grep "^import lombok\.")
    
    # Write them in the right order with blank lines between groups
    if [ -n "$project_imports" ]; then
      echo "$project_imports" >> "$temp_file"
      echo "" >> "$temp_file"
    fi
    
    # External/third-party imports (Spring first, then others)
    if [ -n "$spring_imports" ]; then
      echo "$spring_imports" >> "$temp_file"
      if [ -n "$other_external_imports" ]; then
        echo "" >> "$temp_file"
      fi
    fi
    
    if [ -n "$other_external_imports" ]; then
      echo "$other_external_imports" >> "$temp_file"
      echo "" >> "$temp_file"
    fi
    
    # Java and Jakarta imports
    if [ -n "$jakarta_imports" ]; then
      echo "$jakarta_imports" >> "$temp_file"
      if [ -n "$java_imports" ]; then
        echo "" >> "$temp_file"
      fi
    fi
    
    if [ -n "$java_imports" ]; then
      echo "$java_imports" >> "$temp_file"
      echo "" >> "$temp_file"
    fi
    
    # Lombok imports
    if [ -n "$lombok_imports" ]; then
      echo "$lombok_imports" >> "$temp_file"
      echo "" >> "$temp_file"
    fi
    
    # Rest of the file after imports
    sed -n "$((last_import+1)),\$p" "$file" >> "$temp_file"
    
    # Move temp file to original
    mv "$temp_file" "$file"
    echo "Fixed import order in $file"
  done
}

# Execute all fixes
fix_missing_newlines
fix_indentation
fix_import_order

echo "All checkstyle fixes applied." 