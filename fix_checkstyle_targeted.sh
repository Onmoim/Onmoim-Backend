#!/bin/bash

# This script fixes common checkstyle issues:
# - Indentation: Convert spaces to tabs
# - Import ordering: Reorder imports according to project convention
# - Newlines: Ensure files end with a newline

# Ensure we're in the project directory
cd "$(dirname "$0")"

# File paths
TARGET_DIR="src/main/java/com/onmoim/server"

# Function to ensure a file ends with a newline
add_newline() {
    file="$1"
    if [ -f "$file" ] && [ "$(tail -c 1 "$file" | wc -l)" -eq 0 ]; then
        echo "" >> "$file"
        echo "Added newline to $file"
    fi
}

# Function to convert spaces to tabs for indentation
fix_indentation() {
    file="$1"
    # Create backup
    cp "$file" "${file}.bak"
    
    # Replace leading 4 spaces with a tab
    sed -i '' 's/^    /\t/g' "$file"
    # Replace leading 8 spaces with two tabs
    sed -i '' 's/^        /\t\t/g' "$file"
    # Replace leading 12 spaces with three tabs
    sed -i '' 's/^            /\t\t\t/g' "$file"
    
    echo "Fixed indentation in $file"
}

# Function to fix import order
fix_import_order() {
    file="$1"
    # Create backup if not already created
    if [ ! -f "${file}.bak" ]; then
        cp "$file" "${file}.bak"
    fi
    
    # Extract package and import lines
    package_line=$(grep -m 1 "^package " "$file")
    project_imports=$(grep -E "^import com\.onmoim\." "$file" | sort)
    external_imports=$(grep -E "^import [a-zA-Z0-9_]+\.[a-zA-Z0-9_]+\." "$file" | grep -v "^import com\.onmoim\." | grep -v "^import java\." | grep -v "^import jakarta\." | grep -v "^import lombok\." | sort)
    jakarta_imports=$(grep -E "^import jakarta\." "$file" | sort)
    java_imports=$(grep -E "^import java\." "$file" | sort)
    lombok_imports=$(grep -E "^import lombok\." "$file" | sort)
    
    # Find the start and end of imports section
    start_line=$(grep -n "^import " "$file" | head -n 1 | cut -d: -f1)
    end_line=$(grep -n "^import " "$file" | tail -n 1 | cut -d: -f1)
    
    # If no imports found, exit
    if [ -z "$start_line" ] || [ -z "$end_line" ]; then
        echo "No imports found in $file"
        return
    fi
    
    # Delete all import lines
    sed -i '' "${start_line},${end_line}d" "$file"
    
    # Create a temporary file with ordered imports
    tmp_file=$(mktemp)
    echo "$package_line" > "$tmp_file"
    echo "" >> "$tmp_file"
    
    # Add project imports if any
    if [ -n "$project_imports" ]; then
        echo "$project_imports" >> "$tmp_file"
        echo "" >> "$tmp_file"
    fi
    
    # Add external imports if any
    if [ -n "$external_imports" ]; then
        echo "$external_imports" >> "$tmp_file"
        echo "" >> "$tmp_file"
    fi
    
    # Add Jakarta imports if any
    if [ -n "$jakarta_imports" ]; then
        echo "$jakarta_imports" >> "$tmp_file"
        echo "" >> "$tmp_file"
    fi
    
    # Add Java imports if any
    if [ -n "$java_imports" ]; then
        echo "$java_imports" >> "$tmp_file"
        echo "" >> "$tmp_file"
    fi
    
    # Add Lombok imports if any
    if [ -n "$lombok_imports" ]; then
        echo "$lombok_imports" >> "$tmp_file"
        echo "" >> "$tmp_file"
    fi
    
    # Extract everything after imports
    head_line=$((start_line - 1))
    tail_line=$((end_line + 1))
    head -n $head_line "$file" | grep -v "^package " > "${tmp_file}.head"
    tail -n +$tail_line "$file" > "${tmp_file}.tail"
    
    # Combine everything
    cat "${tmp_file}.head" >> "$tmp_file"
    cat "${tmp_file}.tail" >> "$tmp_file"
    
    # Replace the original file
    mv "$tmp_file" "$file"
    rm -f "${tmp_file}.head" "${tmp_file}.tail"
    
    echo "Fixed import order in $file"
}

# Process files
find_and_fix_files() {
    # Find Java files that have checkstyle issues with indentation
    find "$TARGET_DIR" -name "*.java" | while read -r file; do
        # Add a newline at the end of file if missing
        add_newline "$file"
        
        # Fix indentation (spaces to tabs)
        if grep -q "^ \{4\}" "$file"; then
            fix_indentation "$file"
        fi
        
        # Fix import order
        if grep -q "^import " "$file"; then
            fix_import_order "$file"
        fi
    done
}

# Main execution
echo "Starting targeted checkstyle fixes..."
find_and_fix_files
echo "Targeted fixes completed!"

# Extra for specific files with more complex issues
fix_controller_file() {
    file="$TARGET_DIR/post/controller/GroupPostController.java"
    if [ -f "$file" ]; then
        echo "Fixing $file with special handling..."
        # Fix duplicated imports
        sed -i '' '/^import lombok.RequiredArgsConstructor;$/,+1d' "$file"
        fix_indentation "$file"
        fix_import_order "$file"
    fi
}

fix_controller_file 