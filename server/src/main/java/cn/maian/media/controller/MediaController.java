package cn.maian.media.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.media.domain.MediaPurpose;
import cn.maian.media.domain.MediaVisibility;
import cn.maian.media.dto.MediaDownloadResponse;
import cn.maian.media.dto.MediaUploadResponse;
import cn.maian.media.service.MediaStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    public MediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MediaUploadResponse>> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam MediaPurpose purpose
    ) {
        var result = mediaStorageService.store(file, purpose);
        var response = new MediaUploadResponse(
            result.asset().getId(), result.asset().getContentType(), result.asset().getSizeBytes(),
            result.download().url(), result.download().expiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/{mediaId}/download-token")
    public ApiResponse<MediaDownloadResponse> issueDownload(@PathVariable UUID mediaId) {
        var download = mediaStorageService.issueDownload(mediaId);
        return ApiResponse.ok(new MediaDownloadResponse(download.url(), download.expiresAt()));
    }

    @GetMapping("/{mediaId}/content")
    public ResponseEntity<?> content(
        @PathVariable UUID mediaId,
        @RequestParam String token
    ) {
        var content = mediaStorageService.loadSigned(mediaId, token);
        var builder = ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(content.asset().getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .header("X-Content-Type-Options", "nosniff");
        if (content.asset().getVisibility() == MediaVisibility.PRIVATE) {
            builder.cacheControl(CacheControl.noStore().mustRevalidate());
        } else {
            builder.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
        }
        if (content.xAccelPath() != null) {
            return builder.header("X-Accel-Redirect", content.xAccelPath()).build();
        }
        return builder.body(content.resource());
    }
}
