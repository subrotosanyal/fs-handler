package net.sanyal.fshandler.core.config;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class S3FileSystemConfig extends FileSystemConfig {
    private final String bucketName;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    @Override
    public String getType() {
        return "s3";
    }
}
