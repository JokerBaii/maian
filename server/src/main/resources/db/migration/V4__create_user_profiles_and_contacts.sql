CREATE TABLE user_profiles (
    id CHAR(36) PRIMARY KEY,
    nickname VARCHAR(60) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    role VARCHAR(30) NOT NULL,
    real_name VARCHAR(60) NULL,
    id_card_masked VARCHAR(30) NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE emergency_contacts (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(60) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    relation_name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_contact_user_created (user_id, created_at),
    CONSTRAINT fk_contact_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id)
        ON DELETE CASCADE
);

INSERT INTO user_profiles (
    id, nickname, phone, role, real_name, id_card_masked, verified, created_at
) VALUES (
    '30000000-0000-0000-0000-000000000001',
    '张明',
    '138****6789',
    'VOLUNTEER',
    '张明',
    '3301********1234',
    TRUE,
    CURRENT_TIMESTAMP(6)
);

INSERT INTO emergency_contacts (
    id, user_id, name, phone, relation_name, created_at
) VALUES
    ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '李芳', '139****5678', '配偶', CURRENT_TIMESTAMP(6)),
    ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '张伟', '137****4321', '父亲', CURRENT_TIMESTAMP(6)),
    ('31000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', '王小红', '136****9012', '朋友', CURRENT_TIMESTAMP(6));
