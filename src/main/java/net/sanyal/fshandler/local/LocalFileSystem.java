package net.sanyal.fshandler.local;

import net.sanyal.fshandler.core.FileSystem;
import net.sanyal.fshandler.core.config.LocalFileSystemConfig;
import net.sanyal.fshandler.core.model.FileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class LocalFileSystem implements FileSystem {
    private final Path basePath;

    public LocalFileSystem(LocalFileSystemConfig config) {
        if (config == null || config.getBasePath() == null) {
            throw new IllegalArgumentException("Config and basePath must not be null");
        }
        this.basePath = Paths.get(config.getBasePath()).toAbsolutePath().normalize();
        if (!initializeBasePath()) {
            throw new IllegalStateException("Failed to initialize local file system");
        }
    }

    private boolean initializeBasePath() {
        try {
            Files.createDirectories(basePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to create base directory: {}", basePath, e);
            return false;
        }
    }

    private Path resolveFullPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        // Empty path or "." means base directory
        if (path.isEmpty() || path.equals(".")) {
            return basePath;
        }
        // Prevent path traversal
        if (path.contains("..")) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
        return basePath.resolve(path).normalize();
    }

    @Override
    public FileMetadata createFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when creating a file");
        }
        try {
            Path filePath = resolveFullPath(path);
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);
            return getMetadata(path);
        } catch (IOException e) {
            log.error("Failed to create file at path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to create file: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetadata createDirectory(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when creating a directory");
        }
        try {
            Path dirPath = resolveFullPath(path);
            Files.createDirectories(dirPath);
            return getMetadata(path);
        } catch (IOException e) {
            log.error("Failed to create directory at path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to create directory: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream readFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when reading a file");
        }
        try {
            Path filePath = resolveFullPath(path);
            return new BufferedInputStream(Files.newInputStream(filePath));
        } catch (IOException e) {
            log.error("Failed to read file at path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        }
    }

    @Override
    public OutputStream writeFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when writing to a file");
        }
        try {
            Path filePath = resolveFullPath(path);
            Files.createDirectories(filePath.getParent());
            return new BufferedOutputStream(Files.newOutputStream(filePath));
        } catch (IOException e) {
            log.error("Failed to write to file at path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to write to file: " + e.getMessage(), e);
        }
    }

    @Override
    public OutputStream appendFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when appending to a file");
        }
        try {
            Path filePath = resolveFullPath(path);
            Files.createDirectories(filePath.getParent());
            return new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE));
        } catch (IOException e) {
            log.error("Failed to append to file at path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to append to file: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetadata move(String sourcePath, String destinationPath) {
        if (sourcePath == null || destinationPath == null) {
            throw new IllegalArgumentException("Source and destination paths must not be null when moving a file");
        }
        try {
            Path source = resolveFullPath(sourcePath);
            Path destination = resolveFullPath(destinationPath);
            Files.createDirectories(destination.getParent());
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return getMetadata(destinationPath);
        } catch (IOException e) {
            log.error("Failed to move file from '{}' to '{}': {}", sourcePath, destinationPath, e.getMessage(), e);
            throw new RuntimeException("Failed to move file: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetadata rename(String path, String newName) {
        if (path == null || newName == null) {
            throw new IllegalArgumentException("Path and new name must not be null when renaming a file");
        }
        String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
        return move(path, parentPath + newName);
    }

    @Override
    public void delete(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when deleting a file or directory");
        }
        try {
            Path filePath = resolveFullPath(path);
            if (Files.isDirectory(filePath)) {
                FileSystemUtils.deleteRecursively(filePath);
            } else {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to delete: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileMetadata> list(String path, Predicate<FileMetadata> filter) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when listing directory contents");
        }
        try {
            Path dirPath = resolveFullPath(path);
            if (!Files.exists(dirPath)) {
                log.warn("Directory does not exist at path: {}", path);
                return java.util.Collections.emptyList();
            }
            if (!Files.isDirectory(dirPath)) {
                log.error("Path '{}' exists but is not a directory", path);
                throw new IllegalArgumentException("Path exists but is not a directory: " + path);
            }
            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream
                    .map(p -> {
                        try {
                            // Get the relative path from the base directory
                            String relativePath = path.isEmpty() ? 
                                p.getFileName().toString() : 
                                path + "/" + p.getFileName().toString();
                            return getMetadata(relativePath);
                        } catch (Exception e) {
                            log.warn("Failed to get metadata for path '{}': {}", p, e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .filter(filter != null ? filter : metadata -> true)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list directory at path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to list directory: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileMetadata> listRecursive(String path, Predicate<FileMetadata> filter) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when listing directory contents recursively");
        }
        try {
            Path dirPath = resolveFullPath(path);
            if (!Files.exists(dirPath)) {
                log.warn("Directory does not exist at path: {}", path);
                return java.util.Collections.emptyList();
            }
            if (!Files.isDirectory(dirPath)) {
                log.error("Path '{}' exists but is not a directory", path);
                throw new IllegalArgumentException("Path exists but is not a directory: " + path);
            }
            try (Stream<Path> stream = Files.walk(dirPath)) {
                return stream
                    .filter(p -> !p.equals(dirPath))
                    .map(p -> {
                        try {
                            // Get the relative path from the base directory
                            String relativePath = path + "/" + dirPath.relativize(p).toString();
                            return getMetadata(relativePath);
                        } catch (Exception e) {
                            log.warn("Failed to get metadata for path '{}': {}", p, e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .filter(filter != null ? filter : metadata -> true)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list directory recursively at path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to list directory recursively: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetadata getMetadata(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when getting metadata");
        }
        try {
            Path filePath = resolveFullPath(path);
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            String relativePath = basePath.relativize(filePath).toString();
            return FileMetadata.builder()
                .name(filePath.getFileName().toString())
                .path(relativePath)
                .size(attrs.isDirectory() ? -1 : attrs.size())
                .creationTime(attrs.creationTime().toInstant())
                .lastModifiedTime(attrs.lastModifiedTime().toInstant())
                .isDirectory(attrs.isDirectory())
                .build();
        } catch (IOException e) {
            log.error("Failed to get metadata for path '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to get metadata: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        return Files.isDirectory(basePath) && Files.isWritable(basePath) && Files.isReadable(basePath);
    }
}
