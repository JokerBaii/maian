package cn.maian.agent.controller;

import cn.maian.agent.service.AgentDocumentParser;
import cn.maian.agent.service.DocumentUnderstandingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import cn.maian.common.api.ApiResponse;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.media.domain.*;
import cn.maian.media.repository.MediaAssetRepository;
import cn.maian.media.service.MediaStorageService;
import cn.maian.user.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/assistant/documents")
public class AgentDocumentController {
    private final MediaStorageService storage;
    private final MediaAssetRepository assets;
    private final CurrentUserService users;
    private final DocumentUnderstandingService understanding;
    public AgentDocumentController(MediaStorageService storage, MediaAssetRepository assets, CurrentUserService users, DocumentUnderstandingService understanding) {
        this.storage = storage; this.assets = assets; this.users = users; this.understanding = understanding;
    }
    public record AnalysisRequest(@Size(max = 300) String instruction, boolean useModel) {}
    @PostMapping("/{id}/analysis")
    public ApiResponse<DocumentUnderstandingService.Analysis> analyze(@PathVariable UUID id, @Valid @RequestBody AnalysisRequest request) {
        return ApiResponse.ok(understanding.analyze(id, request.instruction(), request.useModel()));
    }
    public record Document(UUID id, String name, long sizeBytes, String contentType, Instant createdAt, String text, String notice) {}
    @PostMapping
    public ApiResponse<Document> upload(@RequestPart("file") MultipartFile file) {
        var result = storage.store(file, MediaPurpose.AGENT_DOCUMENT);
        return ApiResponse.ok(dto(result.asset(), result.document().text(), result.document().notice()));
    }
    @GetMapping
    public ApiResponse<List<Document>> list() {
        return ApiResponse.ok(assets.findTop100ByOwnerUserIdAndPurposeOrderByCreatedAtDesc(users.currentUserId(), MediaPurpose.AGENT_DOCUMENT)
            .stream().map(a -> dto(a, null, null)).toList());
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable UUID id) {
        var asset = assets.findById(id).filter(a -> a.getOwnerUserId().equals(users.currentUserId()) && a.getPurpose() == MediaPurpose.AGENT_DOCUMENT)
            .orElseThrow(() -> new ResourceNotFoundException("文件不存在"));
        storage.deleteOwned(asset.getId());
        return ApiResponse.ok(true);
    }
    private Document dto(MediaAsset asset, String text, String notice) {
        return new Document(asset.getId(), asset.getOriginalFilename(), asset.getSizeBytes(), asset.getContentType(), asset.getCreatedAt(), text, notice);
    }
}
