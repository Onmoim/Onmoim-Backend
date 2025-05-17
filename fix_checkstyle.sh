#!/bin/bash

# This script fixes common checkstyle issues:
# 1. Fixes import ordering
# 2. Converts spaces to tabs for indentation
# 3. Adds newlines at end of files where missing

# Make sure we're in the right directory
cd "$(dirname "$0")"

# Function to fix import order based on common patterns
fix_import_order() {
  local file="$1"
  
  # Reorder imports based on the project's conventions:
  # 1. Project imports (com.onmoim.*)
  # 2. External package imports
  # 3. Jakarta imports
  # 4. Java imports 
  # 5. Lombok imports
  
  # First, create a backup
  cp "$file" "${file}.bak"
  
  # Extract the package declaration
  local package_line=$(grep "^package" "$file")
  
  # Extract all imports by type
  local project_imports=$(grep "^import com.onmoim" "$file" | sort)
  local external_imports=$(grep "^import " "$file" | grep -v "^import com.onmoim" | grep -v "^import jakarta" | grep -v "^import java" | grep -v "^import lombok" | sort)
  local jakarta_imports=$(grep "^import jakarta" "$file" | sort)
  local java_imports=$(grep "^import java" "$file" | sort)
  local lombok_imports=$(grep "^import lombok" "$file" | sort)
  
  # Remove any lines with extra spaces between import sections
  local content=$(cat "$file" | grep -v "^import " | grep -v "^$")
  
  # Reconstruct the file with proper import order
  echo "$package_line" > "$file"
  echo "" >> "$file"
  
  # Add project imports if any
  if [ ! -z "$project_imports" ]; then
    echo "$project_imports" >> "$file"
    echo "" >> "$file"
  fi
  
  # Add external imports if any
  if [ ! -z "$external_imports" ]; then
    echo "$external_imports" >> "$file"
    echo "" >> "$file"
  fi
  
  # Add jakarta imports if any
  if [ ! -z "$jakarta_imports" ]; then
    echo "$jakarta_imports" >> "$file"
    echo "" >> "$file"
  fi
  
  # Add java imports if any
  if [ ! -z "$java_imports" ]; then
    echo "$java_imports" >> "$file"
    echo "" >> "$file"
  fi
  
  # Add lombok imports if any
  if [ ! -z "$lombok_imports" ]; then
    echo "$lombok_imports" >> "$file"
    echo "" >> "$file"
  fi
  
  # Add remaining content
  echo "$content" >> "$file"
  
  # Replace star imports with explicit imports
  if grep -q "import.*\.\*;" "$file"; then
    echo "Warning: $file contains wildcard imports that should be replaced with explicit imports"
  fi
}

# Function to convert spaces to tabs for indentation
fix_indentation() {
  local file="$1"
  
  # Create a backup
  cp "$file" "${file}.bak"
  
  # Use sed to replace indentation spaces with tabs
  # This assumes standard 4-space indentation
  sed -i '' 's/^    /\t/g' "$file"
  sed -i '' 's/^        /\t\t/g' "$file"
  sed -i '' 's/^            /\t\t\t/g' "$file"
  sed -i '' 's/^                /\t\t\t\t/g' "$file"
}

# Function to ensure newline at end of file
add_newline_eof() {
  local file="$1"
  
  if [ ! -z "$file" ] && [ -f "$file" ]; then
    # Check if file ends with newline
    if [ "$(tail -c 1 "$file" | xxd -p)" != "0a" ]; then
      echo "" >> "$file"
      echo "Added newline to $file"
    fi
  fi
}

# Find all Java files
JAVA_FILES=$(find src/main/java -name "*.java")

# Process each file
for file in $JAVA_FILES; do
  echo "Processing $file..."
  
  # Fix import order
  fix_import_order "$file"
  
  # Fix indentation
  fix_indentation "$file"
  
  # Ensure newline at end of file
  add_newline_eof "$file"
done

echo "Done! Run './gradlew checkstyleMain' to verify fixes." 