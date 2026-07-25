CREATE TABLE emergency_devices (
    id CHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DOUBLE NOT NULL,
    latitude DOUBLE NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_phone VARCHAR(40) NULL,
    service_time VARCHAR(80) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_device_type_status (type, status),
    INDEX idx_device_location (latitude, longitude)
);

INSERT INTO emergency_devices (
    id, type, category, name, address, longitude, latitude, status, owner_phone, service_time, created_at
) VALUES
    ('10000000-0000-0000-0000-000000000001', 'FIXED', 'AED', '紫金港校区体育馆 AED', '杭州市西湖区余杭塘路866号体育馆入口', 120.0869, 30.3055, 'AVAILABLE', '0571-88200000', '全天', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000002', 'FIXED', 'AED', '西湖文化广场 AED', '杭州市拱墅区中山北路西湖文化广场B1层', 120.1633, 30.2794, 'AVAILABLE', '0571-85100000', '06:00-23:00', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000003', 'FIXED', '急救箱', '城西银泰服务台急救箱', '杭州市拱墅区萍水街城西银泰城1楼服务台', 120.1198, 30.2942, 'AVAILABLE', '0571-88000000', '10:00-22:00', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000004', 'FIXED', 'AED', '杭州东站候车层 AED', '杭州市上城区天城路1号候车大厅B区', 120.2133, 30.2907, 'MAINTENANCE', '0571-56700000', '全天', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000005', 'FIXED', '急救包', '市民中心共享急救包', '杭州市上城区新业路311号市民中心L楼', 120.2128, 30.2547, 'AVAILABLE', '0571-87000000', '08:30-17:30', CURRENT_TIMESTAMP(6)),
    ('20000000-0000-0000-0000-000000000001', 'MOBILE', 'AED', '西湖区移动 AED 志愿者', '实时位置·西湖区', 120.1300, 30.2700, 'AVAILABLE', '13500007890', '全天', CURRENT_TIMESTAMP(6)),
    ('20000000-0000-0000-0000-000000000002', 'MOBILE', '急救包', '拱墅区移动急救包', '实时位置·拱墅区', 120.1500, 30.3200, 'AVAILABLE', '13600004567', '工作日 08:00-18:00', CURRENT_TIMESTAMP(6)),
    ('20000000-0000-0000-0000-000000000003', 'MOBILE', 'AED', '上城区移动 AED 志愿者', '实时位置·上城区', 120.1900, 30.2400, 'OFFLINE', '18600007890', '工作日 09:00-18:00', CURRENT_TIMESTAMP(6));
