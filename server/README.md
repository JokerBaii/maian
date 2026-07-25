# 脉安驰援服务端

## 技术栈

- Java 21
- Spring Boot 3.5.16
- Spring AI 1.1.8
- Spring Data JPA
- MySQL 8
- Flyway

## 本地启动

```bash
docker compose up -d
mvn spring-boot:run
```

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
GET/PUT/DELETE  /api/v1/emergency-devices/{id}
PATCH           /api/v1/emergency-devices/{id}/status
GET/POST        /api/v1/rescue-calls
GET             /api/v1/rescue-calls/{id}
PATCH           /api/v1/rescue-calls/{id}/status
GET/POST        /api/v1/health-reports
GET             /api/v1/health-reports/{id}
GET             /api/v1/health-monitoring
GET/POST        /api/v1/emergency-contacts
PUT/DELETE      /api/v1/emergency-contacts/{id}
GET             /api/v1/profile
POST            /api/v1/profile/identity-verification
POST            /api/v1/science-submissions
GET             /api/v1/science-submissions/count
GET/POST        /api/v1/files/images
```

所有接口使用统一 `ApiResponse` 包装，实体不会直接暴露给前端。
