CREATE TABLE health_reports (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    checkup_date DATE NOT NULL,
    hospital VARCHAR(120) NOT NULL,
    source_image_url VARCHAR(500) NULL,
    risk_level VARCHAR(20) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    disclaimer VARCHAR(1000) NOT NULL,
    analysis_source VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_health_report_user_date (user_id, checkup_date),
    CONSTRAINT fk_health_report_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id)
        ON DELETE CASCADE
);

CREATE TABLE health_report_indicators (
    report_id CHAR(36) NOT NULL,
    position INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    value_text VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NULL,
    reference_range VARCHAR(100) NULL,
    abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (report_id, position),
    CONSTRAINT fk_health_indicator_report
        FOREIGN KEY (report_id) REFERENCES health_reports (id)
        ON DELETE CASCADE
);

CREATE TABLE health_report_recommendations (
    report_id CHAR(36) NOT NULL,
    position INT NOT NULL,
    recommendation VARCHAR(500) NOT NULL,
    PRIMARY KEY (report_id, position),
    CONSTRAINT fk_health_recommendation_report
        FOREIGN KEY (report_id) REFERENCES health_reports (id)
        ON DELETE CASCADE
);

INSERT INTO health_reports (
    id, user_id, checkup_date, hospital, source_image_url,
    risk_level, summary, disclaimer, analysis_source, created_at
) VALUES (
    '60000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    '2026-07-01',
    '浙江大学医学院附属第一医院',
    NULL,
    'MEDIUM',
    '本次报告发现 2 项异常指标，建议结合症状和既往病史进一步评估。',
    '本结果仅用于健康信息整理，不能替代医生诊断；如有胸痛、呼吸困难、意识异常等急症表现，请立即呼叫急救。',
    'RULE_BASED',
    UTC_TIMESTAMP(6)
);

INSERT INTO health_report_indicators (
    report_id, position, name, value_text, unit, reference_range, abnormal
) VALUES
    ('60000000-0000-0000-0000-000000000001', 0, '血红蛋白', '128', 'g/L', '130-175', TRUE),
    ('60000000-0000-0000-0000-000000000001', 1, '总胆固醇', '5.8', 'mmol/L', '2.8-5.2', TRUE),
    ('60000000-0000-0000-0000-000000000001', 2, '空腹血糖', '5.2', 'mmol/L', '3.9-6.1', FALSE),
    ('60000000-0000-0000-0000-000000000001', 3, '血小板计数', '220', '×10⁹/L', '125-350', FALSE);

INSERT INTO health_report_recommendations (report_id, position, recommendation) VALUES
    ('60000000-0000-0000-0000-000000000001', 0, '携带完整报告咨询医生，结合既往病史复核异常指标'),
    ('60000000-0000-0000-0000-000000000001', 1, '保持清淡饮食、规律作息，并按医生建议复查');
