package net.sanyal.fshandler.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import net.sanyal.fshandler.core.model.FileMetadata;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FileSystemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String TEST_PROJECT = "test-project";
    private final String MAIN_JAVA = TEST_PROJECT + "/src/main/java";
    private final String TEST_JAVA = TEST_PROJECT + "/src/test/java";

    @BeforeEach
    void setUp() throws Exception {
        // Create project root
        mockMvc.perform(post("/api/v1/fs/directory")
                .param("path", TEST_PROJECT))
                .andExpect(status().isOk());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test directory
        mockMvc.perform(delete("/api/v1/fs/delete")
                .param("path", TEST_PROJECT))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldCreateAndListProjectStructure() throws Exception {
        // Create directory structure
        createDirectory(MAIN_JAVA);
        createDirectory(TEST_JAVA);

        // Create Main.java
        String mainJavaContent = """
                package com.example;
                
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """;
        createFile(MAIN_JAVA + "/Main.java", mainJavaContent);

        // Create MainTest.java
        String testJavaContent = """
                package com.example;
                
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;
                
                public class MainTest {
                    @Test
                    void testMain() {
                        assertTrue(true);
                    }
                }
                """;
        createFile(TEST_JAVA + "/MainTest.java", testJavaContent);

        // Create build.gradle
        String buildGradleContent = """
                plugins {
                    id 'java'
                    id 'application'
                }
                
                group = 'com.example'
                version = '1.0-SNAPSHOT'
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
                }
                
                application {
                    mainClass = 'com.example.Main'
                }
                
                test {
                    useJUnitPlatform()
                }
                """;
        createFile(TEST_PROJECT + "/build.gradle", buildGradleContent);

        // List all files recursively
        MvcResult result = mockMvc.perform(get("/api/v1/fs/list")
                .param("path", TEST_PROJECT)
                .param("recursive", "true"))
                .andExpect(status().isOk())
                .andReturn();

        List<FileMetadata> files = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});

        // Verify directory structure
        assertNotNull(files);
        assertTrue(files.size() >= 7); // At least 7 items (root, src, main, test, Main.java, MainTest.java, build.gradle)

        // Verify specific files exist
        assertTrue(files.stream().anyMatch(f -> f.getPath().equals(MAIN_JAVA + "/Main.java")));
        assertTrue(files.stream().anyMatch(f -> f.getPath().equals(TEST_JAVA + "/MainTest.java")));
        assertTrue(files.stream().anyMatch(f -> f.getPath().equals(TEST_PROJECT + "/build.gradle")));

        // Verify file contents
        verifyFileContent(MAIN_JAVA + "/Main.java", mainJavaContent);
        verifyFileContent(TEST_JAVA + "/MainTest.java", testJavaContent);
        verifyFileContent(TEST_PROJECT + "/build.gradle", buildGradleContent);

        // Test filtering
        result = mockMvc.perform(get("/api/v1/fs/list")
                .param("path", TEST_PROJECT)
                .param("recursive", "true")
                .param("filter", "*.java"))
                .andExpect(status().isOk())
                .andReturn();

        List<FileMetadata> javaFiles = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});

        assertEquals(2, javaFiles.size()); // Only Main.java and MainTest.java
        assertTrue(javaFiles.stream().allMatch(f -> f.getPath().endsWith(".java")));
    }

    private void createDirectory(String path) throws Exception {
        mockMvc.perform(post("/api/v1/fs/directory")
                .param("path", path))
                .andExpect(status().isOk());
    }

    private void createFile(String path, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "filename.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/fs/file")
                .file(file)
                .param("path", path))
                .andExpect(status().isOk());
    }

    private void verifyFileContent(String path, String expectedContent) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/fs/file")
                .param("path", path))
                .andExpect(status().isOk())
                .andReturn();

        String actualContent = result.getResponse().getContentAsString();
        assertEquals(expectedContent, actualContent);
    }
}
