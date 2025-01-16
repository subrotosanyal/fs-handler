package net.sanyal.fshandler.core.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class FileMetadata {
    String name;
    String path;
    long size;
    Instant creationTime;
    Instant lastModifiedTime;
    boolean isDirectory;
}
