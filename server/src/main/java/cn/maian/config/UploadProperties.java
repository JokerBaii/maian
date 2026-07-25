package cn.maian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(Path directory, long maxFileSize) {

    public UploadProperties {
        directory = directory == null ? Path.of("./data/uploads") : directory;
        maxFileSize = maxFileSize <= 0 ? 5 * 1024 * 1024 : maxFileSize;
    }
}
