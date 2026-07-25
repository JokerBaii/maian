package cn.maian.file.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.file.dto.FileUploadResponse;
import cn.maian.file.service.ImageStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/files/images")
public class ImageController {

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
        @RequestPart("file") MultipartFile file
    ) {
        var filename = imageStorageService.store(file);
        var response = new FileUploadResponse("/api/v1/files/images/" + filename);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{filename}")
    public ResponseEntity<?> get(@PathVariable String filename) {
        var image = imageStorageService.load(filename);
        return ResponseEntity.ok()
            .contentType(image.mediaType())
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(image.resource());
    }
}
