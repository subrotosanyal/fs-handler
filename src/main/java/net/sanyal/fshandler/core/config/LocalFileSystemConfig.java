package net.sanyal.fshandler.core.config;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class LocalFileSystemConfig extends FileSystemConfig {
    private final String basePath;

    @Override
    public String getType() {
        return "local";
    }
}
