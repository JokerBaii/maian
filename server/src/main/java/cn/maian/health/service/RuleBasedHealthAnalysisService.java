package cn.maian.health.service;

import cn.maian.health.domain.HealthRiskLevel;
import cn.maian.health.dto.HealthAnalysisRequest;
import cn.maian.health.dto.HealthAnalysisResponse;
import cn.maian.health.dto.HealthIndicator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedHealthAnalysisService implements HealthAnalysisService {

    @Override
    public HealthAnalysisResponse analyze(HealthAnalysisRequest request) {
        List<EvaluatedIndicator> evaluated = request.indicators().stream()
            .map(this::evaluate)
            .filter(item -> item.severity() != Severity.NORMAL)
            .toList();
        long criticalCount = count(evaluated, Severity.CRITICAL);
        long moderateCount = count(evaluated, Severity.MODERATE);

        HealthRiskLevel riskLevel;
        if (criticalCount > 0 || moderateCount >= 2) {
            riskLevel = HealthRiskLevel.HIGH;
        } else if (!evaluated.isEmpty()) {
            riskLevel = HealthRiskLevel.MEDIUM;
        } else {
            riskLevel = HealthRiskLevel.LOW;
        }

        List<String> abnormalItems = evaluated.stream()
            .map(item -> "[" + item.severity().label + "] " + describe(item.indicator()))
            .toList();
        String summary = switch (riskLevel) {
            case HIGH -> criticalCount > 0
                ? "检出至少一项达到紧急阈值，不应被其他轻微异常稀释。"
                : "检出多项中度异常，建议尽快完成专业评估。";
            case MEDIUM -> "检出需要关注的指标，建议结合症状和既往病史复核。";
            case LOW -> "本次提交的指标未触发确定性异常阈值，请继续规律监测。";
        };
        List<String> recommendations = recommendations(riskLevel, criticalCount > 0);
        return new HealthAnalysisResponse(
            riskLevel, summary, abnormalItems, recommendations,
            HealthAnalysisResponse.MEDICAL_DISCLAIMER, "RULE_BASED_V2"
        );
    }

    private EvaluatedIndicator evaluate(HealthIndicator indicator) {
        Severity thresholdSeverity = switch (indicator.name()) {
            case "血压" -> evaluateBloodPressure(indicator.value());
            case "空腹血糖" -> evaluateRange(number(indicator.value()), 2.8, 7.0, 11.1);
            case "静息心率", "心率" -> evaluateHeartRate(number(indicator.value()));
            case "血红蛋白" -> evaluateHemoglobin(number(indicator.value()));
            case "低密度脂蛋白" -> atLeast(number(indicator.value()), 4.9, Severity.MODERATE);
            case "甘油三酯" -> atLeast(number(indicator.value()), 5.6, Severity.MODERATE);
            default -> Severity.NORMAL;
        };
        if (thresholdSeverity == Severity.NORMAL && indicator.abnormal()) {
            thresholdSeverity = Severity.MILD;
        }
        return new EvaluatedIndicator(indicator, thresholdSeverity);
    }

    private Severity evaluateBloodPressure(String value) {
        String[] parts = value == null ? new String[0] : value.split("/");
        if (parts.length != 2) return Severity.NORMAL;
        double systolic = number(parts[0]);
        double diastolic = number(parts[1]);
        if (systolic >= 180 || diastolic >= 120 || systolic < 80 || diastolic < 50) return Severity.CRITICAL;
        if (systolic >= 160 || diastolic >= 100) return Severity.MODERATE;
        if (systolic >= 140 || diastolic >= 90) return Severity.MILD;
        return Severity.NORMAL;
    }

    private Severity evaluateHeartRate(double value) {
        if (value >= 150 || (value > 0 && value <= 40)) return Severity.CRITICAL;
        if (value >= 120 || (value > 0 && value < 50)) return Severity.MODERATE;
        if (value > 100) return Severity.MILD;
        return Severity.NORMAL;
    }

    private Severity evaluateHemoglobin(double value) {
        if (value > 0 && value <= 80) return Severity.CRITICAL;
        if (value > 0 && value < 110) return Severity.MODERATE;
        return Severity.NORMAL;
    }

    private Severity evaluateRange(double value, double criticalLow, double moderateHigh, double criticalHigh) {
        if ((value > 0 && value <= criticalLow) || value >= criticalHigh) return Severity.CRITICAL;
        if (value >= moderateHigh) return Severity.MODERATE;
        return Severity.NORMAL;
    }

    private Severity atLeast(double value, double threshold, Severity severity) {
        return value >= threshold ? severity : Severity.NORMAL;
    }

    private double number(String value) {
        if (value == null) return Double.NaN;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private long count(List<EvaluatedIndicator> items, Severity severity) {
        return items.stream().filter(item -> item.severity() == severity).count();
    }

    private List<String> recommendations(HealthRiskLevel riskLevel, boolean critical) {
        List<String> recommendations = new ArrayList<>();
        if (critical) {
            recommendations.add("若伴随胸痛、呼吸困难、意识异常等症状，立即就医或拨打 120");
        }
        if (riskLevel != HealthRiskLevel.LOW) {
            recommendations.add("携带完整原始报告咨询医生");
            recommendations.add("在同等条件下复测异常指标");
        } else {
            recommendations.add("保持规律作息并按计划复查");
        }
        return List.copyOf(recommendations);
    }

    private String describe(HealthIndicator indicator) {
        String unit = indicator.unit() == null ? "" : " " + indicator.unit();
        String range = indicator.referenceRange() == null ? "" : "（参考范围：" + indicator.referenceRange() + "）";
        return indicator.name() + "：" + indicator.value() + unit + range;
    }

    private enum Severity {
        NORMAL("正常"), MILD("轻度"), MODERATE("中度"), CRITICAL("紧急");
        private final String label;
        Severity(String label) { this.label = label; }
    }

    private record EvaluatedIndicator(HealthIndicator indicator, Severity severity) {
    }
}
