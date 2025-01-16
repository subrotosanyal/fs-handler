package net.sanyal.fshandler.api;

import net.sanyal.fshandler.core.FileSystem;
import net.sanyal.fshandler.core.model.FileMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(FileSystemController.class)
class FileSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileSystem fileSystem;

    @Test
    void createFile_ShouldReturnCreatedFile() throws Exception {
        FileMetadata metadata = FileMetadata.builder()
                .name("test.txt")
                .path("test.txt")
                .size(0L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.createFile("test.txt")).thenReturn(metadata);

        mockMvc.perform(post("/api/v1/fs/file")
                        .param("path", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.path").value("test.txt"))
                .andExpect(jsonPath("$.directory").value(false));
    }

    @Test
    void createDirectory_ShouldReturnCreatedDirectory() throws Exception {
        FileMetadata metadata = FileMetadata.builder()
                .name("testDir")
                .path("testDir")
                .size(0L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(true)
                .build();

        when(fileSystem.createDirectory("testDir")).thenReturn(metadata);

        mockMvc.perform(post("/api/v1/fs/directory")
                        .param("path", "testDir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testDir"))
                .andExpect(jsonPath("$.path").value("testDir"))
                .andExpect(jsonPath("$.directory").value(true));
    }

    @Test
    void readFile_ShouldReturnFileContent() throws Exception {
        String content = "Hello, World!";
        when(fileSystem.readFile("test.txt"))
                .thenReturn(new ByteArrayInputStream(content.getBytes()));

        mockMvc.perform(get("/api/v1/fs/file")
                        .param("path", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(content));
    }

    @Test
    void writeFile_ShouldReturnUpdatedMetadata() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(fileSystem.writeFile("test.txt")).thenReturn(outputStream);

        FileMetadata metadata = FileMetadata.builder()
                .name("test.txt")
                .path("test.txt")
                .size(13L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.getMetadata("test.txt")).thenReturn(metadata);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/fs/file")
                        .file(file)
                        .param("path", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.path").value("test.txt"))
                .andExpect(jsonPath("$.size").value(13));
    }

    @Test
    void list_ShouldReturnDirectoryContents() throws Exception {
        FileMetadata file1 = FileMetadata.builder()
                .name("file1.txt")
                .path("testDir/file1.txt")
                .size(0L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata file2 = FileMetadata.builder()
                .name("file2.txt")
                .path("testDir/file2.txt")
                .size(0L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.list(eq("testDir"), any())).thenReturn(Arrays.asList(file1, file2));

        mockMvc.perform(get("/api/v1/fs/list")
                        .param("path", "testDir")
                        .param("recursive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("file1.txt"))
                .andExpect(jsonPath("$[1].name").value("file2.txt"));
    }

    @Test
    void list_ShouldReturnDirectoryContentsWithFilter() throws Exception {
        FileMetadata file1 = FileMetadata.builder()
                .name("file1.txt")
                .path("testDir/file1.txt")
                .size(100L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata file2 = FileMetadata.builder()
                .name("file2.java")
                .path("testDir/file2.java")
                .size(200L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.list(eq("testDir"), any())).thenReturn(Arrays.asList(file1, file2));

        // Test with no filter
        mockMvc.perform(get("/api/v1/fs/list")
                        .param("path", "testDir")
                        .param("recursive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("file1.txt"))
                .andExpect(jsonPath("$[0].size").value(100))
                .andExpect(jsonPath("$[1].name").value("file2.java"))
                .andExpect(jsonPath("$[1].size").value(200));

        // Test with .txt filter
        mockMvc.perform(get("/api/v1/fs/list")
                        .param("path", "testDir")
                        .param("recursive", "false")
                        .param("filter", "*.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("file1.txt"));
    }

    @Test
    void listRecursive_ShouldReturnNestedDirectoryContents() throws Exception {
        FileMetadata rootDir = FileMetadata.builder()
                .name("root")
                .path("root")
                .size(0L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(true)
                .build();

        FileMetadata subDir = FileMetadata.builder()
                .name("subDir")
                .path("root/subDir")
                .size(0L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(true)
                .build();

        FileMetadata file1 = FileMetadata.builder()
                .name("file1.txt")
                .path("root/file1.txt")
                .size(100L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata file2 = FileMetadata.builder()
                .name("file2.java")
                .path("root/subDir/file2.java")
                .size(200L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata file3 = FileMetadata.builder()
                .name("file3.txt")
                .path("root/subDir/file3.txt")
                .size(300L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.listRecursive(eq("root"), any()))
                .thenReturn(Arrays.asList(rootDir, subDir, file1, file2, file3));

        // Test recursive listing without filter
        mockMvc.perform(get("/api/v1/fs/list")
                        .param("path", "root")
                        .param("recursive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("root"))
                .andExpect(jsonPath("$[0].directory").value(true))
                .andExpect(jsonPath("$[1].name").value("subDir"))
                .andExpect(jsonPath("$[1].directory").value(true))
                .andExpect(jsonPath("$[2].name").value("file1.txt"))
                .andExpect(jsonPath("$[2].size").value(100))
                .andExpect(jsonPath("$[3].name").value("file2.java"))
                .andExpect(jsonPath("$[3].size").value(200))
                .andExpect(jsonPath("$[4].name").value("file3.txt"))
                .andExpect(jsonPath("$[4].size").value(300));

        // Test recursive listing with .txt filter
        when(fileSystem.listRecursive(eq("root"), any()))
                .thenReturn(Arrays.asList(file1, file3));

        mockMvc.perform(get("/api/v1/fs/list")
                        .param("path", "root")
                        .param("recursive", "true")
                        .param("filter", "*.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("file1.txt"))
                .andExpect(jsonPath("$[1].name").value("file3.txt"));
    }

    @Test
    void list_ShouldHandleEmptyDirectory() throws Exception {
        when(fileSystem.list(eq("emptyDir"), any())).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/api/v1/fs/list")
                        .param("path", "emptyDir")
                        .param("recursive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void health_ShouldReturnStatus() throws Exception {
        when(fileSystem.isHealthy()).thenReturn(true);

        mockMvc.perform(get("/api/v1/fs/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Healthy"));

        when(fileSystem.isHealthy()).thenReturn(false);

        mockMvc.perform(get("/api/v1/fs/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Unhealthy"));
    }

    @Test
    void testDeleteNonExistentFile() throws Exception {
        String nonExistentFile = "nonexistent.txt";
        
        mockMvc.perform(delete("/api/v1/fs/delete")
                .param("path", nonExistentFile))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteNonExistentDirectory() throws Exception {
        String nonExistentDir = "nonexistent/";
        doNothing().when(fileSystem).delete(nonExistentDir);
        
        mockMvc.perform(delete("/api/v1/fs/delete")
                .param("path", nonExistentDir))
                .andExpect(status().isNoContent());
    }

    @Test
    void testMoveNonExistentFile() throws Exception {
        String sourcePath = "nonexistent.txt";
        String destinationPath = "moved.txt";
        when(fileSystem.move(sourcePath, destinationPath))
            .thenReturn(FileMetadata.builder()
                .path(destinationPath)
                .name("moved.txt")
                .size(0L)
                .lastModifiedTime(Instant.now())
                .build());
        
        mockMvc.perform(put("/api/v1/fs/move")
                .param("sourcePath", sourcePath)
                .param("destinationPath", destinationPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(destinationPath));
    }

    @Test
    void testReadNonExistentFile() throws Exception {
        String nonExistentFile = "nonexistent.txt";
        when(fileSystem.readFile(nonExistentFile))
            .thenReturn(new ByteArrayInputStream("File not found".getBytes()));
        
        mockMvc.perform(get("/api/v1/fs/file")
                .param("path", nonExistentFile))
                .andExpect(status().isOk())
                .andExpect(content().string("File not found"));
    }

    @Test
    void testListNonExistentDirectory() throws Exception {
        String nonExistentDir = "nonexistent/";
        when(fileSystem.list(eq(nonExistentDir), any())).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", nonExistentDir))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testListRecursiveNonExistentDirectory() throws Exception {
        String nonExistentDir = "nonexistent/";
        when(fileSystem.listRecursive(eq(nonExistentDir), any())).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", nonExistentDir)
                .param("recursive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testListEmptyDirectoryWithFilter() throws Exception {
        String emptyDir = "emptyDir/";
        when(fileSystem.createDirectory(emptyDir)).thenReturn(
            FileMetadata.builder()
                .name("emptyDir")
                .path(emptyDir)
                .isDirectory(true)
                .build()
        );
        when(fileSystem.list(eq(emptyDir), any())).thenReturn(Collections.emptyList());
        
        mockMvc.perform(post("/api/v1/fs/directory")
                .param("path", emptyDir))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", emptyDir)
                .param("filter", ".txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testListRecursiveEmptyDirectoryWithFilter() throws Exception {
        String emptyDir = "emptyDir/";
        when(fileSystem.createDirectory(emptyDir)).thenReturn(
            FileMetadata.builder()
                .name("emptyDir")
                .path(emptyDir)
                .isDirectory(true)
                .build()
        );
        when(fileSystem.listRecursive(eq(emptyDir), any())).thenReturn(Collections.emptyList());
        
        mockMvc.perform(post("/api/v1/fs/directory")
                .param("path", emptyDir))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", emptyDir)
                .param("recursive", "true")
                .param("filter", ".txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testMoveToExistingFile() throws Exception {
        String sourcePath = "source.txt";
        String destinationPath = "destination.txt";
        
        when(fileSystem.move(sourcePath, destinationPath)).thenReturn(
            FileMetadata.builder()
                .name("destination.txt")
                .path(destinationPath)
                .isDirectory(false)
                .build()
        );
        
        mockMvc.perform(put("/api/v1/fs/move")
                .param("sourcePath", sourcePath)
                .param("destinationPath", destinationPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(destinationPath));
    }

    @Test
    void rename_ShouldRenameFileSuccessfully() throws Exception {
        String originalPath = "test.txt";
        String newName = "renamed.txt";
        
        FileMetadata originalMetadata = FileMetadata.builder()
                .name("test.txt")
                .path("test.txt")
                .size(100L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata renamedMetadata = FileMetadata.builder()
                .name("renamed.txt")
                .path("renamed.txt")
                .size(100L)
                .creationTime(originalMetadata.getCreationTime())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.rename(originalPath, newName)).thenReturn(renamedMetadata);

        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", originalPath)
                .param("newName", newName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(newName))
            .andExpect(jsonPath("$.path").value(newName))
            .andExpect(jsonPath("$.size").value(100L))
            .andExpect(jsonPath("$.directory").value(false));
    }

    @Test
    void rename_ShouldRenameDirectorySuccessfully() throws Exception {
        String originalPath = "testDir";
        String newName = "renamedDir";
        
        FileMetadata originalMetadata = FileMetadata.builder()
                .name("testDir")
                .path("testDir")
                .size(0L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(true)
                .build();

        FileMetadata renamedMetadata = FileMetadata.builder()
                .name("renamedDir")
                .path("renamedDir")
                .size(0L)
                .creationTime(originalMetadata.getCreationTime())
                .lastModifiedTime(Instant.now())
                .isDirectory(true)
                .build();

        when(fileSystem.rename(originalPath, newName)).thenReturn(renamedMetadata);

        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", originalPath)
                .param("newName", newName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(newName))
            .andExpect(jsonPath("$.path").value(newName))
            .andExpect(jsonPath("$.size").value(0L))
            .andExpect(jsonPath("$.directory").value(true));
    }

    @Test
    void rename_ShouldHandleNonExistentFile() throws Exception {
        String originalPath = "nonexistent.txt";
        String newName = "renamed.txt";

        when(fileSystem.rename(originalPath, newName))
            .thenThrow(new RuntimeException("File not found: " + originalPath));

        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", originalPath)
                .param("newName", newName))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error: File not found")));
    }

    @Test
    void rename_ShouldHandleInvalidNewName() throws Exception {
        String originalPath = "test.txt";

        // Test with path traversal in new name
        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", originalPath)
                .param("newName", "../test2.txt"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error: Invalid new name")));

        // Test with forward slash in new name
        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", originalPath)
                .param("newName", "dir/test2.txt"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error: Invalid new name")));

        // Test with empty new name
        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", originalPath)
                .param("newName", ""))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error: Invalid new name")));

        // Test with missing new name
        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", originalPath))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error: Missing required parameter: newName")));

        // Test with missing path
        mockMvc.perform(put("/api/v1/fs/rename")
                .param("newName", "test2.txt"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error: Missing required parameter: path")));

        // Test with invalid path
        mockMvc.perform(put("/api/v1/fs/rename")
                .param("path", "../test.txt")
                .param("newName", "test2.txt"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error: Invalid path")));
    }

    @Test
    void testNullAndEmptyParameters() throws Exception {
        // Test null/empty path for delete
        mockMvc.perform(delete("/api/v1/fs/delete")
                .param("path", ""))
                .andExpect(status().isBadRequest());

        // Test null/empty path for read
        mockMvc.perform(get("/api/v1/fs/file")
                .param("path", ""))
                .andExpect(status().isBadRequest());

        // Test null/empty path for list
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", ""))
                .andExpect(status().isBadRequest());

        // Test null/empty source path for move
        mockMvc.perform(put("/api/v1/fs/move")
                .param("sourcePath", "")
                .param("destinationPath", "dest.txt"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/fs/move")
                .param("sourcePath", "source.txt")
                .param("destinationPath", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidPaths() throws Exception {
        String invalidPath = "../invalid/path";
        
        // Test invalid path for delete
        mockMvc.perform(delete("/api/v1/fs/delete")
                .param("path", invalidPath))
                .andExpect(status().isBadRequest());

        // Test invalid path for read
        mockMvc.perform(get("/api/v1/fs/file")
                .param("path", invalidPath))
                .andExpect(status().isBadRequest());

        // Test invalid path for list
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", invalidPath))
                .andExpect(status().isBadRequest());

        // Test invalid source path for move
        mockMvc.perform(put("/api/v1/fs/move")
                .param("sourcePath", invalidPath)
                .param("destinationPath", "dest.txt"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/fs/move")
                .param("sourcePath", "source.txt")
                .param("destinationPath", invalidPath))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidPathsComprehensive() throws Exception {
        // Test path traversal attempts
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "../test"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "test/../../etc/passwd"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        // Test absolute paths
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "/"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "\\temp"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        // Test path traversal with file operations
        mockMvc.perform(get("/api/v1/fs/file")
                .param("path", "../config/secret.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        mockMvc.perform(post("/api/v1/fs/directory")
                .param("path", "/etc/secrets"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        // Test Windows-style absolute paths
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "C:\\Windows"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "D:\\folder"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));

        // Test path traversal with mixed separators
        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "folder/../..\\etc/passwd"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid path: absolute paths and path traversal not allowed")));
    }

    @Test
    void list_ShouldHandleNullFilter() throws Exception {
        FileMetadata file1 = FileMetadata.builder()
                .name("file1.txt")
                .path("testDir/file1.txt")
                .size(100L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata file2 = FileMetadata.builder()
                .name("file2.java")
                .path("testDir/file2.java")
                .size(200L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.list(eq("testDir"), any())).thenReturn(Arrays.asList(file1, file2));

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "testDir")
                .param("recursive", "false")
                .param("filter", (String) null))  // Explicitly pass null filter
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("file1.txt"))
            .andExpect(jsonPath("$[1].name").value("file2.java"));
    }

    @Test
    void list_ShouldHandleEmptyFilter() throws Exception {
        FileMetadata file1 = FileMetadata.builder()
                .name("file1.txt")
                .path("testDir/file1.txt")
                .size(100L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata file2 = FileMetadata.builder()
                .name("file2.java")
                .path("testDir/file2.java")
                .size(200L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.list(eq("testDir"), any())).thenReturn(Arrays.asList(file1, file2));

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "testDir")
                .param("recursive", "false")
                .param("filter", ""))  // Empty filter string
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("file1.txt"))
            .andExpect(jsonPath("$[1].name").value("file2.java"));
    }

    @Test
    void list_ShouldHandleOmittedFilter() throws Exception {
        FileMetadata file1 = FileMetadata.builder()
                .name("file1.txt")
                .path("testDir/file1.txt")
                .size(100L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        FileMetadata file2 = FileMetadata.builder()
                .name("file2.java")
                .path("testDir/file2.java")
                .size(200L)
                .creationTime(Instant.now())
                .lastModifiedTime(Instant.now())
                .isDirectory(false)
                .build();

        when(fileSystem.list(eq("testDir"), any())).thenReturn(Arrays.asList(file1, file2));

        mockMvc.perform(get("/api/v1/fs/list")
                .param("path", "testDir")
                .param("recursive", "false"))  // No filter parameter
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("file1.txt"))
            .andExpect(jsonPath("$[1].name").value("file2.java"));
    }
}
