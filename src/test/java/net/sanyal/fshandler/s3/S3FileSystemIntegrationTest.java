package net.sanyal.fshandler.s3;

import net.sanyal.fshandler.core.AbstractFileSystemTest;
import net.sanyal.fshandler.core.FileSystem;
import net.sanyal.fshandler.core.config.S3FileSystemConfig;
import net.sanyal.fshandler.core.model.FileMetadata;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class S3FileSystemIntegrationTest extends AbstractFileSystemTest {
    private static final String BUCKET_NAME = "test-bucket";
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");

    @Container
    private static final LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.S3);

    private S3FileSystem fileSystem;
    private S3FileSystemConfig config;

    @Override
    protected void setupFileSystem() {
        assertTrue(localstack.isRunning(), "LocalStack container must be running");
        
        config = S3FileSystemConfig.builder()
            .bucketName(BUCKET_NAME)
            .region(localstack.getRegion())
            .accessKey(localstack.getAccessKey())
            .secretKey(localstack.getSecretKey())
            .maxConnections(10)
            .timeoutMillis(5000)
            .build();

        System.setProperty("aws.endpoint-url", localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        fileSystem = new S3FileSystem(config);
        
        // Clean up any existing files
        List<FileMetadata> existingFiles = fileSystem.list("", null);
        for (FileMetadata file : existingFiles) {
            fileSystem.delete(file.getPath());
        }
    }

    @Override
    protected FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    protected void cleanupFileSystem() {
        System.clearProperty("aws.endpoint-url");
        if (fileSystem != null) {
            // Clean up any remaining files
            List<FileMetadata> existingFiles = fileSystem.list("", null);
            for (FileMetadata file : existingFiles) {
                try {
                    fileSystem.delete(file.getPath());
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
            
            try {
                fileSystem.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    void verifyS3Configuration() {
        assertTrue(localstack.isRunning(), "LocalStack container should be running");
        
        // Verify the endpoint configuration
        assertEquals(
            localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            System.getProperty("aws.endpoint-url"),
            "S3 endpoint should be configured correctly"
        );

        // Verify we can perform operations
        String testFile = "test.txt";
        FileMetadata metadata = fileSystem.createFile(testFile);
        assertNotNull(metadata, "Should be able to create files in S3");
        assertEquals(testFile, metadata.getPath());
    }

    @Test
    void verifyBucketConfiguration() {
        // Verify bucket exists by creating and listing a file
        String testFile = "bucket-test.txt";
        fileSystem.createFile(testFile);
        
        List<FileMetadata> files = fileSystem.list("", null);
        assertTrue(files.stream().anyMatch(f -> f.getPath().equals(testFile)),
            "File should be created in the correct bucket");
    }
}
