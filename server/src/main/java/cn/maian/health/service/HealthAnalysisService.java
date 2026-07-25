package cn.maian.health.service;

import cn.maian.health.dto.HealthAnalysisRequest;
import cn.maian.health.dto.HealthAnalysisResponse;

public interface HealthAnalysisService {

    HealthAnalysisResponse analyze(HealthAnalysisRequest request);
}
