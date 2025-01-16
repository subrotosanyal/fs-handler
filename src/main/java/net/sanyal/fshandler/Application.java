package net.sanyal.fshandler;

import net.sanyal.fshandler.core.FileSystem;
import net.sanyal.fshandler.core.config.LocalFileSystemConfig;
import net.sanyal.fshandler.core.config.S3FileSystemConfig;
import net.sanyal.fshandler.local.LocalFileSystem;
import net.sanyal.fshandler.s3.S3FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {
    @Value("${filesystem.type:local}")
    private String fsType;

    @Value("${filesystem.basePath}")
    private String basePath;

    @Value("${filesystem.s3.region:#{null}}")
    private String region;

    @Value("${filesystem.s3.accessKey:#{null}}")
    private String accessKey;

    @Value("${filesystem.s3.secretKey:#{null}}")
    private String secretKey;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public FileSystem fileSystem() {
        if ("s3".equals(fsType)) {
            S3FileSystemConfig config = S3FileSystemConfig.builder()
                .bucketName(basePath)
                .region(region)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .maxConnections(50)
                .timeoutMillis(5000)
                .build();
            return new S3FileSystem(config);
        } else {
            LocalFileSystemConfig config = LocalFileSystemConfig.builder()
                .basePath(basePath)
                .maxConnections(50)
                .timeoutMillis(5000)
                .build();
            return new LocalFileSystem(config);
        }
    }
}
