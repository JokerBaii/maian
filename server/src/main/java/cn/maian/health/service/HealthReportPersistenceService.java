package cn.maian.health.service;

import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.health.domain.HealthReport;
import cn.maian.health.dto.CreateHealthReportRequest;
import cn.maian.health.dto.HealthAnalysisResponse;
import cn.maian.health.dto.HealthReportResponse;
import cn.maian.health.repository.HealthReportRepository;
import cn.maian.media.domain.MediaPurpose;
import cn.maian.media.service.MediaStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HealthReportPersistenceService {

    private final HealthReportRepository healthReportRepository;
    private final MediaStorageService mediaStorageService;

    public HealthReportPersistenceService(
        HealthReportRepository healthReportRepository,
        MediaStorageService mediaStorageService
    ) {
        this.healthReportRepository = healthReportRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public HealthReportResponse create(
        CreateHealthReportRequest request,
        HealthAnalysisResponse analysis,
        UUID userId
    ) {
        HealthReport report = HealthReport.create(
            userId, request.checkupDate(), request.hospital(), request.sourceMediaId(),
            request.indicators(), analysis
        );
        report = healthReportRepository.save(report);
        if (request.sourceMediaId() != null) {
            mediaStorageService.attachOwned(
                request.sourceMediaId(), MediaPurpose.HEALTH_REPORT,
                "HEALTH_REPORT", report.getId(), false
            );
        }
        return HealthReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public List<HealthReportResponse> list(UUID userId) {
        return healthReportRepository.findAllByUserIdOrderByCheckupDateDesc(userId)
            .stream().map(HealthReportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public HealthReportResponse get(UUID id, UUID userId) {
        return HealthReportResponse.from(findOwned(id, userId));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        HealthReport report = findOwned(id, userId);
        UUID sourceMediaId = report.getSourceMediaId();
        healthReportRepository.delete(report);
        if (sourceMediaId != null) {
            mediaStorageService.deleteOwned(sourceMediaId);
        }
    }

    private HealthReport findOwned(UUID id, UUID userId) {
        return healthReportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("体检报告不存在"));
    }
}
