package net.sanyal.fshandler.core.config;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class FileSystemConfig {
    private final int maxConnections;
    private final long timeoutMillis;
    
    public abstract String getType();
}
