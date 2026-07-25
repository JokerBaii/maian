package cn.maian.file.service;

import cn.maian.config.UploadProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Map<String, StoredImageType> SUPPORTED_TYPES = Map.of(
        MediaType.IMAGE_JPEG_VALUE, new StoredImageType("jpg", MediaType.IMAGE_JPEG),
        MediaType.IMAGE_PNG_VALUE, new StoredImageType("png", MediaType.IMAGE_PNG),
        "image/webp", new StoredImageType("webp", MediaType.parseMediaType("image/webp"))
    );

    private final Path storageDirectory;
    private final long maxFileSize;

    public ImageStorageService(UploadProperties properties) {
        this.storageDirectory = properties.directory().toAbsolutePath().normalize();
        this.maxFileSize = properties.maxFileSize();
    }

    @PostConstruct
    void initializeStorage() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建图片存储目录", exception);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要上传的图片");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("图片大小不能超过 5MB");
        }

        var imageType = SUPPORTED_TYPES.get(file.getContentType());
        if (imageType == null) {
            throw new IllegalArgumentException("仅支持 JPG、PNG 或 WebP 图片");
        }

        var filename = UUID.randomUUID() + "." + imageType.extension();
        var target = storageDirectory.resolve(filename).normalize();
        ensureInsideStorage(target);

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException exception) {
            throw new IllegalStateException("图片保存失败", exception);
        }
    }

    public StoredImage load(String filename) {
        if (!filename.matches("^[0-9a-f-]{36}\\.(jpg|png|webp)$")) {
            throw new IllegalArgumentException("图片地址无效");
        }

        var path = storageDirectory.resolve(filename).normalize();
        ensureInsideStorage(path);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("图片不存在");
        }

        try {
            var extension = filename.substring(filename.lastIndexOf('.') + 1);
            var mediaType = switch (extension) {
                case "jpg" -> MediaType.IMAGE_JPEG;
                case "png" -> MediaType.IMAGE_PNG;
                case "webp" -> MediaType.parseMediaType("image/webp");
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            return new StoredImage(new UrlResource(path.toUri()), mediaType);
        } catch (IOException exception) {
            throw new IllegalStateException("图片读取失败", exception);
        }
    }

    private void ensureInsideStorage(Path path) {
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("图片地址无效");
        }
    }

    private record StoredImageType(String extension, MediaType mediaType) {
    }

    public record StoredImage(Resource resource, MediaType mediaType) {
    }
}
