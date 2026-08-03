package cn.maian.health.service;

import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.health.dto.CreateHealthReportRequest;
import cn.maian.health.dto.HealthAnalysisRequest;
import cn.maian.health.dto.HealthReportResponse;
import cn.maian.health.repository.HealthReportRepository;
import cn.maian.user.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HealthReportService {

    private final HealthReportRepository healthReportRepository;
    private final HealthAnalysisService healthAnalysisService;
    private final CurrentUserService currentUserService;

    public HealthReportService(
        HealthReportRepository healthReportRepository,
        HealthAnalysisService healthAnalysisService,
        CurrentUserService currentUserService
    ) {
        this.healthReportRepository = healthReportRepository;
        this.healthAnalysisService = healthAnalysisService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public HealthReportResponse create(CreateHealthReportRequest request) {
        var summary = request.checkupDate() + " 于" + request.hospital().trim() + "完成体检";
        var analysis = healthAnalysisService.analyze(
            new HealthAnalysisRequest(summary, request.indicators())
        );
        var report = cn.maian.health.domain.HealthReport.create(
            currentUserService.currentUserId(),
            request.checkupDate(),
            request.hospital(),
            request.sourceImageUrl(),
            request.indicators(),
            analysis
        );
        return HealthReportResponse.from(healthReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<HealthReportResponse> listCurrentUserReports() {
        return healthReportRepository
            .findAllByUserIdOrderByCheckupDateDesc(currentUserService.currentUserId())
            .stream()
            .map(HealthReportResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public HealthReportResponse get(UUID id) {
        var report = healthReportRepository.findByIdAndUserId(id, currentUserService.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("体检报告不存在"));
        return HealthReportResponse.from(report);
    }

    @Transactional
    public void delete(UUID id) {
        var report = healthReportRepository.findByIdAndUserId(id, currentUserService.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("体检报告不存在"));
        healthReportRepository.delete(report);
    }
}
