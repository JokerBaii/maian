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
import java.nio.file.StandardOpenOption;
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

        try {
            byte[] content = file.getBytes();
            if (!hasExpectedSignature(content, imageType.extension())) {
                throw new IllegalArgumentException("图片内容与文件类型不匹配");
            }
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
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

    private boolean hasExpectedSignature(byte[] content, String extension) {
        return switch (extension) {
            case "jpg" -> content.length >= 3
                && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF;
            case "png" -> content.length >= 8
                && (content[0] & 0xFF) == 0x89
                && content[1] == 0x50
                && content[2] == 0x4E
                && content[3] == 0x47
                && content[4] == 0x0D
                && content[5] == 0x0A
                && content[6] == 0x1A
                && content[7] == 0x0A;
            case "webp" -> content.length >= 12
                && content[0] == 'R'
                && content[1] == 'I'
                && content[2] == 'F'
                && content[3] == 'F'
                && content[8] == 'W'
                && content[9] == 'E'
                && content[10] == 'B'
                && content[11] == 'P';
            default -> false;
        };
    }

    private record StoredImageType(String extension, MediaType mediaType) {
    }

    public record StoredImage(Resource resource, MediaType mediaType) {
    }
}
