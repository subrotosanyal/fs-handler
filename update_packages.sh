#!/bin/bash

# Create new directory structure
mkdir -p src/main/java/net/sanyal/fshandler/{api,core/{config,model},local,s3}
mkdir -p src/main/resources

# Move files to new locations
mv src/main/java/com/fshandler/api/FileSystemController.java src/main/java/net/sanyal/fshandler/api/
mv src/main/java/com/fshandler/core/FileSystem.java src/main/java/net/sanyal/fshandler/core/
mv src/main/java/com/fshandler/core/config/FileSystemConfig.java src/main/java/net/sanyal/fshandler/core/config/
mv src/main/java/com/fshandler/core/model/FileMetadata.java src/main/java/net/sanyal/fshandler/core/model/
mv src/main/java/com/fshandler/local/LocalFileSystem.java src/main/java/net/sanyal/fshandler/local/
mv src/main/java/com/fshandler/s3/S3FileSystem.java src/main/java/net/sanyal/fshandler/s3/
mv src/main/java/com/fshandler/Application.java src/main/java/net/sanyal/fshandler/

# Update package names
find src/main/java/net/sanyal/fshandler -type f -name "*.java" -exec sed -i '' 's/package com.fshandler/package net.sanyal.fshandler/g' {} +
find src/main/java/net/sanyal/fshandler -type f -name "*.java" -exec sed -i '' 's/import com.fshandler/import net.sanyal.fshandler/g' {} +

# Remove old directory structure
rm -rf src/main/java/com
