package net.sanyal.fshandler.s3;

import net.sanyal.fshandler.core.FileSystem;
import net.sanyal.fshandler.core.config.S3FileSystemConfig;
import net.sanyal.fshandler.core.model.FileMetadata;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;

@Slf4j
public class S3FileSystem implements FileSystem, AutoCloseable {
    private final S3Client s3Client;
    private final String bucketName;

    public S3FileSystem(S3FileSystemConfig config) {
        if (config == null || config.getBucketName() == null || config.getRegion() == null
            || config.getAccessKey() == null || config.getSecretKey() == null) {
            throw new IllegalArgumentException("All S3 configuration parameters must be provided");
        }
        this.bucketName = config.getBucketName();
        this.s3Client = initializeS3Client(config);
        ensureBucketExists();
    }

    private S3Client initializeS3Client(S3FileSystemConfig config) {
        var builder = S3Client.builder()
            .region(Region.of(config.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())));

        String endpointUrl = System.getProperty("aws.endpoint-url");
        if (endpointUrl != null && !endpointUrl.isEmpty()) {
            try {
                builder.endpointOverride(URI.create(endpointUrl));
            } catch (IllegalArgumentException e) {
                log.error("Invalid endpoint URL: {}", endpointUrl, e);
            }
        }

        return builder.build();
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    @Override
    public FileMetadata createFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when creating a file");
        }
        try {
            s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build(),
                RequestBody.empty());
            return getMetadata(path);
        } catch (S3Exception e) {
            log.error("Failed to create file at path '{}': {} ({})", path, e.getMessage(), e.awsErrorDetails().errorCode(), e);
            throw new RuntimeException("Failed to create file: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetadata createDirectory(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when creating a directory");
        }
        String dirPath = path.endsWith("/") ? path : path + "/";
        try {
            s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(dirPath)
                .build(),
                RequestBody.empty());
            
            // Return metadata without trailing slash to match test expectations
            FileMetadata metadata = getMetadata(dirPath);
            return FileMetadata.builder()
                .name(path.substring(path.lastIndexOf('/') + 1))
                .path(path)
                .size(metadata.getSize())
                .lastModifiedTime(metadata.getLastModifiedTime())
                .creationTime(metadata.getCreationTime())
                .isDirectory(true)
                .build();
        } catch (S3Exception e) {
            log.error("Failed to create directory at path '{}': {} ({})", dirPath, e.getMessage(), e.awsErrorDetails().errorCode(), e);
            throw new RuntimeException("Failed to create directory: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream readFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when reading a file");
        }
        try {
            return new BufferedInputStream(s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build()));
        } catch (S3Exception e) {
            log.error("Failed to read file at path '{}': {} ({})", path, e.getMessage(), e.awsErrorDetails().errorCode(), e);
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        }
    }

    @Override
    public OutputStream writeFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when writing to a file");
        }
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                try {
                    s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(path)
                        .build(),
                        RequestBody.fromBytes(toByteArray()));
                } catch (S3Exception e) {
                    log.error("Failed to write to file at path '{}': {} ({})", path, e.getMessage(), e.awsErrorDetails().errorCode(), e);
                    throw new IOException("Failed to write to file: " + e.getMessage(), e);
                }
                super.close();
            }
        };
    }

    @Override
    public OutputStream appendFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when appending to a file");
        }
        throw new UnsupportedOperationException("S3 does not support direct append operations");
    }

    @Override
    public FileMetadata move(String sourcePath, String destinationPath) {
        if (sourcePath == null || destinationPath == null) {
            throw new IllegalArgumentException("Source and destination paths must not be null when moving a file");
        }
        try {
            // Copy the object to the new location
            s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourcePath)
                .destinationBucket(bucketName)
                .destinationKey(destinationPath)
                .build());

            // Delete the original object
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(sourcePath)
                .build());

            return getMetadata(destinationPath);
        } catch (S3Exception e) {
            log.error("Failed to move file from '{}' to '{}': {} ({})", sourcePath, destinationPath, e.getMessage(), e.awsErrorDetails().errorCode(), e);
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
            if (path.endsWith("/")) {
                // Delete all objects under this prefix for directories
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(path);

                ListObjectsV2Response response;
                do {
                    response = s3Client.listObjectsV2(requestBuilder.build());
                    for (S3Object object : response.contents()) {
                        s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object.key())
                            .build());
                    }
                    requestBuilder.continuationToken(response.nextContinuationToken());
                } while (response.isTruncated());
            } else {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build());
            }
        } catch (S3Exception e) {
            log.error("Failed to delete path '{}': {} ({})", path, e.getMessage(), e.awsErrorDetails().errorCode(), e);
            throw new RuntimeException("Failed to delete: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileMetadata> list(String path, Predicate<FileMetadata> filter) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when listing directory contents");
        }
        try {
            // For root directory, don't use any prefix
            String prefix = path.isEmpty() ? null : (path.endsWith("/") ? path : path + "/");
            System.out.println("S3 list - Using prefix: '" + prefix + "'");
            
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucketName);
                
            if (prefix != null) {
                requestBuilder.prefix(prefix);
            }
            
            // Only use delimiter for non-empty paths
            if (!path.isEmpty()) {
                requestBuilder.delimiter("/");
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            System.out.println("S3 list - Raw objects found:");
            response.contents().forEach(obj -> System.out.println(" - " + obj.key()));

            List<FileMetadata> result = response.contents().stream()
                .filter(obj -> {
                    // For root directory, only include files without '/'
                    if (path.isEmpty()) {
                        return !obj.key().contains("/");
                    }
                    // For other directories, filter out the directory itself and other directories
                    return !obj.key().equals(prefix) && !obj.key().endsWith("/");
                })
                .map(this::createFileMetadata)
                .filter(filter != null ? filter : metadata -> true)
                .collect(Collectors.toList());

            System.out.println("S3 list - After filtering:");
            result.forEach(meta -> System.out.println(" - " + meta.getPath() + " (name: " + meta.getName() + ")"));

            return result;
        } catch (S3Exception e) {
            log.error("Failed to list directory at path '{}': {} ({})", path, e.getMessage(), e.awsErrorDetails().errorCode(), e);
            throw new RuntimeException("Failed to list directory: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileMetadata> listRecursive(String path, Predicate<FileMetadata> filter) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when listing directory contents recursively");
        }
        try {
            String prefix = path.isEmpty() ? "" : path.endsWith("/") ? path : path + "/";
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build());

            return response.contents().stream()
                .filter(obj -> !obj.key().equals(prefix)) // Filter out the directory itself
                .map(this::createFileMetadata)
                .filter(filter != null ? filter : metadata -> true)
                .collect(Collectors.toList());
        } catch (S3Exception e) {
            log.error("Failed to list directory recursively at path '{}': {} ({})", path, e.getMessage(), e.awsErrorDetails().errorCode(), e);
            throw new RuntimeException("Failed to list directory recursively: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetadata getMetadata(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null when getting metadata");
        }
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build());

            return FileMetadata.builder()
                .name(path.substring(path.lastIndexOf('/') + 1))
                .path(path)
                .size(response.contentLength())
                .lastModifiedTime(response.lastModified())
                .creationTime(response.lastModified()) // S3 doesn't store creation time
                .isDirectory(path.endsWith("/"))
                .build();
        } catch (S3Exception e) {
            log.error("Failed to get metadata for path '{}': {} ({})", path, e.getMessage(), e.awsErrorDetails().errorCode(), e);
            throw new RuntimeException("Failed to get metadata: " + e.getMessage(), e);
        }
    }

    private FileMetadata createFileMetadata(S3Object s3Object) {
        String key = s3Object.key();
        boolean isDirectory = key.endsWith("/");
        
        // Remove trailing slash for directories to match test expectations
        String path = isDirectory ? key.substring(0, key.length() - 1) : key;
        
        // Handle root path specially
        String name = path.contains("/") ? 
            path.substring(path.lastIndexOf('/') + 1) : 
            path;
        
        return FileMetadata.builder()
            .name(name)
            .path(path)
            .size(s3Object.size())
            .lastModifiedTime(s3Object.lastModified())
            .creationTime(s3Object.lastModified()) // S3 doesn't store creation time
            .isDirectory(isDirectory)
            .build();
    }

    @Override
    public boolean isHealthy() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
