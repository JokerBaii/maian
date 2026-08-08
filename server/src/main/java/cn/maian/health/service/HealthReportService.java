package cn.maian.health.service;

import cn.maian.health.dto.CreateHealthReportRequest;
import cn.maian.health.dto.HealthAnalysisRequest;
import cn.maian.health.dto.HealthReportResponse;
import cn.maian.user.service.CurrentUserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HealthReportService {

    private final HealthAnalysisService healthAnalysisService;
    private final HealthReportPersistenceService persistenceService;
    private final CurrentUserService currentUserService;

    public HealthReportService(
        HealthAnalysisService healthAnalysisService,
        HealthReportPersistenceService persistenceService,
        CurrentUserService currentUserService
    ) {
        this.healthAnalysisService = healthAnalysisService;
        this.persistenceService = persistenceService;
        this.currentUserService = currentUserService;
    }

    public HealthReportResponse create(CreateHealthReportRequest request) {
        String summary = request.checkupDate() + " 于" + request.hospital().trim() + "完成体检";
        var analysis = healthAnalysisService.analyze(
            new HealthAnalysisRequest(summary, request.indicators())
        );
        return persistenceService.create(request, analysis, currentUserService.currentUserId());
    }

    public List<HealthReportResponse> listCurrentUserReports() {
        return persistenceService.list(currentUserService.currentUserId());
    }

    public HealthReportResponse get(UUID id) {
        return persistenceService.get(id, currentUserService.currentUserId());
    }

    public void delete(UUID id) {
        persistenceService.delete(id, currentUserService.currentUserId());
    }
}
