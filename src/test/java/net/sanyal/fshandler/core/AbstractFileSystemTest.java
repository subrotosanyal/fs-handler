package net.sanyal.fshandler.core;

import net.sanyal.fshandler.core.model.FileMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractFileSystemTest {
    protected abstract FileSystem getFileSystem();
    protected abstract void setupFileSystem();
    protected abstract void cleanupFileSystem();

    @BeforeEach
    void setUp() {
        setupFileSystem();
    }

    @AfterEach
    void tearDown() {
        cleanupFileSystem();
    }

    @Test
    void createFile_ShouldCreateEmptyFile() {
        String testPath = "test.txt";
        FileMetadata metadata = getFileSystem().createFile(testPath);

        assertEquals(testPath, metadata.getPath());
        assertEquals("test.txt", metadata.getName());
        assertEquals(0, metadata.getSize());
        assertFalse(metadata.isDirectory());
    }

    @Test
    void createDirectory_ShouldCreateDirectory() {
        String testPath = "testDir";
        FileMetadata metadata = getFileSystem().createDirectory(testPath);

        assertEquals(testPath, metadata.getPath());
        assertEquals("testDir", metadata.getName());
        assertTrue(metadata.isDirectory());
    }

    @Test
    void writeAndReadFile_ShouldWorkCorrectly() throws IOException {
        String testPath = "test.txt";
        String content = "Hello, World!";

        // Write content
        try (OutputStream os = getFileSystem().writeFile(testPath)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }

        // Read content
        try (InputStream is = getFileSystem().readFile(testPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            assertEquals(content, reader.readLine());
        }
    }

    @Test
    void move_ShouldMoveFile() {
        String sourcePath = "source.txt";
        String destPath = "dest.txt";

        getFileSystem().createFile(sourcePath);
        FileMetadata movedMetadata = getFileSystem().move(sourcePath, destPath);

        assertEquals(destPath, movedMetadata.getPath());
        assertThrows(RuntimeException.class, () -> getFileSystem().getMetadata(sourcePath));
    }

    @Test
    void delete_ShouldDeleteFileOrDirectory() {
        String filePath = "test.txt";
        String dirPath = "testDir";

        getFileSystem().createFile(filePath);
        getFileSystem().createDirectory(dirPath);

        getFileSystem().delete(filePath);
        getFileSystem().delete(dirPath);

        assertThrows(RuntimeException.class, () -> getFileSystem().getMetadata(filePath));
        assertThrows(RuntimeException.class, () -> getFileSystem().getMetadata(dirPath));
    }

    @Test
    void list_ShouldListDirectoryContents() throws IOException {
        String dirPath = "testDir";
        String file1 = dirPath + "/file1.txt";
        String file2 = dirPath + "/file2.txt";

        getFileSystem().createDirectory(dirPath);
        getFileSystem().createFile(file1);
        getFileSystem().createFile(file2);

        List<FileMetadata> contents = getFileSystem().list(dirPath, null);
        assertEquals(2, contents.size());
        assertTrue(contents.stream().anyMatch(m -> m.getName().equals("file1.txt")));
        assertTrue(contents.stream().anyMatch(m -> m.getName().equals("file2.txt")));
    }

    @Test
    void listRecursive_ShouldListRecursively() throws IOException {
        String dirPath = "testDir";
        String subDirPath = dirPath + "/subDir";
        String file1 = dirPath + "/file1.txt";
        String file2 = subDirPath + "/file2.txt";

        getFileSystem().createDirectory(dirPath);
        getFileSystem().createDirectory(subDirPath);

        try (OutputStream os1 = getFileSystem().writeFile(file1)) {
            os1.write("test1".getBytes());
        }
        try (OutputStream os2 = getFileSystem().writeFile(file2)) {
            os2.write("test2".getBytes());
        }

        List<FileMetadata> contents = getFileSystem().listRecursive(dirPath, null);
        assertEquals(3, contents.size()); // subDir, file1.txt, file2.txt
        
        assertTrue(contents.stream().anyMatch(m -> m.getName().equals("subDir") && m.isDirectory()));
        assertTrue(contents.stream().anyMatch(m -> m.getName().equals("file1.txt") && !m.isDirectory()));
        assertTrue(contents.stream().anyMatch(m -> m.getName().equals("file2.txt") && !m.isDirectory()));
    }

    @Test
    void list_ShouldFilterCorrectly() throws IOException {
        String txtFile1 = "test1.txt";
        String txtFile2 = "test2.txt";
        String javaFile = "Test.java";
        String content = "test content";

        System.out.println("\nCreating test files...");
        FileMetadata meta1 = getFileSystem().createFile(txtFile1);
        System.out.println("Created " + txtFile1 + ": " + meta1.getPath() + " (name: " + meta1.getName() + ")");
        FileMetadata meta2 = getFileSystem().createFile(txtFile2);
        System.out.println("Created " + txtFile2 + ": " + meta2.getPath() + " (name: " + meta2.getName() + ")");
        FileMetadata meta3 = getFileSystem().createFile(javaFile);
        System.out.println("Created " + javaFile + ": " + meta3.getPath() + " (name: " + meta3.getName() + ")");

        try (OutputStream os = getFileSystem().writeFile(txtFile1)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
        try (OutputStream os = getFileSystem().writeFile(txtFile2)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
        try (OutputStream os = getFileSystem().writeFile(javaFile)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("\nListing all files before filtering:");
        List<FileMetadata> allFiles = getFileSystem().list("", meta -> true);
        allFiles.forEach(meta -> System.out.println(" - " + meta.getPath() + " (name: " + meta.getName() + ")"));

        System.out.println("\nListing .txt files:");
        List<FileMetadata> txtFiles = getFileSystem().list("", meta -> meta.getName().endsWith(".txt"));
        txtFiles.forEach(meta -> System.out.println(" - " + meta.getPath() + " (name: " + meta.getName() + ")"));
        assertEquals(2, txtFiles.size(), "Expected exactly 2 .txt files but found " + txtFiles.size() + 
            ". Files found: " + txtFiles.stream().map(FileMetadata::getName).collect(Collectors.joining(", ")));
        assertTrue(txtFiles.stream().allMatch(meta -> meta.getName().endsWith(".txt")));

        System.out.println("\nListing .java files:");
        List<FileMetadata> javaFiles = getFileSystem().list("", meta -> meta.getName().endsWith(".java"));
        javaFiles.forEach(meta -> System.out.println(" - " + meta.getPath() + " (name: " + meta.getName() + ")"));
        assertEquals(1, javaFiles.size());
        assertEquals("Test.java", javaFiles.get(0).getName());
    }
}
