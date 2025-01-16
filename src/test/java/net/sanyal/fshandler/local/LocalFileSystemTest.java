package net.sanyal.fshandler.local;

import net.sanyal.fshandler.core.AbstractFileSystemTest;
import net.sanyal.fshandler.core.FileSystem;
import net.sanyal.fshandler.core.config.LocalFileSystemConfig;
import net.sanyal.fshandler.core.model.FileMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileSystemTest extends AbstractFileSystemTest {
    private LocalFileSystem fileSystem;
    @TempDir
    Path tempDir;

    @Override
    protected void setupFileSystem() {
        LocalFileSystemConfig config = LocalFileSystemConfig.builder()
                .basePath(tempDir.toString())
                .maxConnections(10)
                .timeoutMillis(5000)
                .build();
        fileSystem = new LocalFileSystem(config);
    }

    @Override
    protected FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    protected void cleanupFileSystem() {
        // No cleanup needed as @TempDir handles it
    }

    @Test
    void appendFile_ShouldAppendContent() throws IOException {
        String testPath = "test.txt";
        String initialContent = "Hello";
        String appendedContent = ", World!";

        // Write initial content
        try (OutputStream os = fileSystem.writeFile(testPath)) {
            os.write(initialContent.getBytes(StandardCharsets.UTF_8));
        }

        // Append content
        try (OutputStream os = fileSystem.appendFile(testPath)) {
            os.write(appendedContent.getBytes(StandardCharsets.UTF_8));
        }

        // Read full content
        try (InputStream is = fileSystem.readFile(testPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            assertEquals(initialContent + appendedContent, reader.readLine());
        }
    }

    @Test
    void rename_ShouldRenameFile() {
        String originalPath = "original.txt";
        String newName = "renamed.txt";

        fileSystem.createFile(originalPath);
        FileMetadata renamedMetadata = fileSystem.rename(originalPath, newName);

        assertFalse(Files.exists(tempDir.resolve(originalPath)));
        assertTrue(Files.exists(tempDir.resolve(newName)));
        assertEquals(newName, renamedMetadata.getPath());
    }

    @Test
    void verifyLocalFileExists_ShouldConfirmFileExistence() {
        String testPath = "test.txt";
        fileSystem.createFile(testPath);
        assertTrue(Files.exists(tempDir.resolve(testPath)), "File should exist in local filesystem");
    }

    @Test
    void verifyLocalDirectoryExists_ShouldConfirmDirectoryExistence() {
        String testPath = "testDir";
        fileSystem.createDirectory(testPath);
        assertTrue(Files.isDirectory(tempDir.resolve(testPath)), "Directory should exist in local filesystem");
    }
}
