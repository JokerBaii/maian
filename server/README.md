# 脉安驰援服务端

## 技术栈

- Java 21
- Spring Boot 3.5.16
- Spring AI 1.1.8
- Spring Data JPA
- MySQL 8
- Flyway

## 本地启动

先安装并启动 MySQL 8，创建 `maian` 数据库和开发账号，然后执行：

```bash
export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/maian?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export MYSQL_USER=maian
export MYSQL_PASSWORD=maian_dev
mvn spring-boot:run
```

Flyway 会自动执行全部数据库迁移，不依赖 Docker。

使用 Docker 部署时，先复制并修改环境变量，再启动：

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

默认只将 MySQL 端口绑定到宿主机回环地址；后端监听 `${SERVER_PORT:-8080}`，上传文件保存在 `maian_uploads` 数据卷。部署到公网前必须修改数据库密码和 `CORS_ALLOWED_ORIGINS`。

默认地址：`http://localhost:8080`

健康检查：`GET /actuator/health`

上传图片默认保存在 `server/data/uploads`。生产环境请通过 `UPLOAD_DIRECTORY` 指向持久化目录，并配置 `CORS_ALLOWED_ORIGINS`。

## Spring AI

默认使用确定性规则生成健康分析，保证没有模型密钥时项目也能运行。

启用 Spring AI：

```bash
export APP_AI_ENABLED=true
export OPENAI_API_KEY=your-key
mvn spring-boot:run
```

健康分析由体检报告录入接口内部调用：

```bash
curl -X POST http://localhost:8080/api/v1/health-reports \
  -H 'Content-Type: application/json' \
  -d '{
    "checkupDate": "2026-07-25",
    "hospital": "示例体检机构",
    "indicators": [
      {
        "name": "收缩压",
        "value": "152",
        "unit": "mmHg",
        "referenceRange": "90-139",
        "abnormal": true
      }
    ]
  }'
```

返回结果包含风险级别、异常项、建议、医疗免责声明以及分析来源。

## 主要接口

```text
GET/POST        /api/v1/emergency-devices
GET             /api/v1/emergency-devices/mine
GET             /api/v1/emergency-devices/reviews/pending
GET/PUT/DELETE  /api/v1/emergency-devices/{id}
PATCH           /api/v1/emergency-devices/{id}/review
PATCH           /api/v1/emergency-devices/{id}/status
PATCH           /api/v1/emergency-devices/{id}/location
GET/POST        /api/v1/rescue-calls
GET             /api/v1/rescue-calls/responder-tasks
GET             /api/v1/rescue-calls/{id}
POST            /api/v1/rescue-calls/{id}/accept
PATCH           /api/v1/rescue-calls/{id}/responder-progress
PATCH           /api/v1/rescue-calls/{id}/status
POST            /api/v1/rescue-calls/{id}/match-attempts
GET/POST        /api/v1/rescue-calls/{id}/feedback
GET/POST        /api/v1/health-reports
GET/DELETE      /api/v1/health-reports/{id}
POST            /api/v1/health-reports/recognition
GET             /api/v1/health-monitoring
POST            /api/v1/heart-rate-readings
GET/PUT/DELETE  /api/v1/wearable-device
GET/PUT         /api/v1/settings
GET/POST        /api/v1/emergency-contacts
PUT/DELETE      /api/v1/emergency-contacts/{id}
GET             /api/v1/profile
POST            /api/v1/profile/identity-verification
GET/POST        /api/v1/science-submissions
GET             /api/v1/science-submissions/approved
GET             /api/v1/science-submissions/reviews/pending
GET/DELETE      /api/v1/science-submissions/{id}
PATCH           /api/v1/science-submissions/{id}/review
GET             /api/v1/science-submissions/count
GET/PUT         /api/v1/science-articles/{articleId}/interaction
GET/POST        /api/v1/files/images
```

所有接口使用统一 `ApiResponse` 包装，实体不会直接暴露给前端。

## AED 快速匹配

创建呼救后会在同一个事务中立即匹配 AED：

1. MySQL 通过 `category + status + latitude + longitude` 索引和地理包围盒快速缩小候选集；
2. 剔除超过 120 秒未上报位置的移动设备、超过车主服务半径的设备；
3. 用 Haversine 精确距离计算 ETA。移动 AED 按车辆送达时间计算，固定 AED 按骑行往返取送时间计算；
4. 按 ETA 而非单纯直线距离排序，并用条件 `UPDATE` 原子占用设备，避免并发重复派单；
5. 暂无候选时，救援详情页轮询期间每 3 轮（约 9 秒）发起一次受锁保护的重试，累计上限 60 次。

调度参数可通过 `DISPATCH_*` 环境变量调整，默认搜索半径 3km、候选上限 80、移动位置有效期 120 秒、移动设备 35km/h、固定设备取回 18km/h。搜索半径按"能及时取回"设定：半径过大会把数公里外的设备算作候选，预计到达时间失去参考意义。

当前 Java 评分器的回归性能门禁为 25 万候选不超过 2 秒；本地测试约 52ms。当前耗时主要在数据库候选检索与网络 I/O，因此暂不引入 JNI/Rust FFI。候选规模达到百万级或接入大规模路网矩阵后，再考虑将纯计算评分内核迁移到 Rust。

## 数据真实性

健康监测、穿戴设备绑定、提醒设置、科普投稿、体检报告、救援评价、设备与救援记录均已由 MySQL 持久化。没有心率记录时接口返回空数据状态，不再生成模拟读数。设备修改、救援查询、投稿管理和评价提交均按当前用户隔离：评价只允许呼救方在救援完成后提交一次。

体检报告识别（`POST /api/v1/health-reports/recognition`）当前返回预置指标样例，按图片地址稳定选取以便复现，尚未接入 OCR 引擎；响应中的提示要求用户逐项核对后再保存。

首次建库会写入一组比赛展示数据，用于开箱展示地图点位、健康趋势、报告和多角色审核流程；界面按正常业务数据呈现。正式接入实际业务前，应以审核后的现场采集数据替换这组展示数据。
