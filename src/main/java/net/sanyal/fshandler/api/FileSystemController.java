package net.sanyal.fshandler.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.sanyal.fshandler.core.FileSystem;
import net.sanyal.fshandler.core.model.FileMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fs")
@RequiredArgsConstructor
@Tag(name = "File System", description = "File System operations API")
public class FileSystemController {

    private final FileSystem fileSystem;

    private void validatePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path must not be null or empty");
        }
        // Prevent absolute paths and path traversal
        if (path.startsWith("/") || 
            path.startsWith("\\") || 
            path.contains("..") ||
            path.matches("^[A-Za-z]:\\\\.*")) {  // Windows drive letter pattern
            throw new IllegalArgumentException("Invalid path: absolute paths and path traversal not allowed");
        }
    }

    private void validateNewName(String newName) {
        if (newName == null || newName.isEmpty() || newName.contains("..") || newName.contains("/")) {
            throw new IllegalArgumentException("Invalid new name");
        }
    }

    @Operation(summary = "Create a new file")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file path"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/file")
    public FileMetadata createFile(
            @Parameter(description = "Path where the file should be created") 
            @RequestParam String path) {
        validatePath(path);
        return fileSystem.createFile(path);
    }

    @Operation(summary = "Create a new directory")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Directory created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid directory path"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/directory")
    public FileMetadata createDirectory(
            @Parameter(description = "Path where the directory should be created") 
            @RequestParam String path) {
        validatePath(path);
        return fileSystem.createDirectory(path);
    }

    @Operation(summary = "Read file contents")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File content retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/file")
    public ResponseEntity<byte[]> readFile(
            @Parameter(description = "Path to the file to read") 
            @RequestParam String path) throws IOException {
        validatePath(path);
        try (InputStream is = fileSystem.readFile(path)) {
            byte[] content = StreamUtils.copyToByteArray(is);
            return ResponseEntity.ok(content);
        } catch (java.io.FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Write content to a file")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File written successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file path"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/file", consumes = "multipart/form-data")
    public FileMetadata writeFile(
            @Parameter(description = "Path where the file should be written") 
            @RequestParam String path,
            @Parameter(description = "File content to write") 
            @RequestParam MultipartFile file) throws IOException {
        validatePath(path);
        try (OutputStream os = fileSystem.writeFile(path)) {
            file.getInputStream().transferTo(os);
        }
        return fileSystem.getMetadata(path);
    }

    @Operation(summary = "List directory contents")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Directory listing retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Directory not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/list")
    public List<FileMetadata> list(
            @Parameter(description = "Path to the directory to list") 
            @RequestParam String path,
            @Parameter(description = "Whether to list recursively") 
            @RequestParam(required = false) boolean recursive,
            @Parameter(description = "Filter pattern (e.g., *.txt, *.java)") 
            @RequestParam(required = false) String filter) {
        validatePath(path);
        java.util.function.Predicate<FileMetadata> filterPredicate = filter != null ?
            metadata -> java.nio.file.FileSystems.getDefault()
                .getPathMatcher("glob:" + filter)
                .matches(java.nio.file.Paths.get(metadata.getPath()).getFileName()) :
            null;
        if (recursive) {
            return fileSystem.listRecursive(path, filterPredicate);
        }
        return fileSystem.list(path, filterPredicate);
    }

    @Operation(summary = "Delete a file or directory")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "File or directory deleted successfully"),
        @ApiResponse(responseCode = "404", description = "File or directory not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Path to the file or directory to delete") 
            @RequestParam String path) {
        validatePath(path);
        fileSystem.delete(path);
    }

    @Operation(summary = "Move a file or directory")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File or directory moved successfully"),
        @ApiResponse(responseCode = "404", description = "Source path not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/move")
    public FileMetadata move(
            @Parameter(description = "Source path of the file or directory") 
            @RequestParam String sourcePath,
            @Parameter(description = "Destination path for the file or directory") 
            @RequestParam String destinationPath) {
        validatePath(sourcePath);
        validatePath(destinationPath);
        return fileSystem.move(sourcePath, destinationPath);
    }

    @Operation(summary = "Rename a file or directory")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File or directory renamed successfully"),
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/rename")
    public FileMetadata rename(
            @Parameter(description = "Path to the file or directory to rename") 
            @RequestParam String path,
            @Parameter(description = "New name for the file or directory") 
            @RequestParam String newName) {
        validatePath(path);
        validateNewName(newName);
        return fileSystem.rename(path, newName);
    }

    @Operation(summary = "Check system health")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "System is healthy"),
        @ApiResponse(responseCode = "503", description = "System is unhealthy")
    })
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean isHealthy = fileSystem.isHealthy();
        return ResponseEntity
                .status(isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(isHealthy ? "Healthy" : "Unhealthy");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Error: " + e.getMessage());
    }

    @ExceptionHandler({Exception.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<String> handleException(Exception e) {
        if (e instanceof org.springframework.web.bind.MissingServletRequestParameterException) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Error: Missing required parameter: " + ((org.springframework.web.bind.MissingServletRequestParameterException) e).getParameterName());
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
    }
}
