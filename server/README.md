# 脉安驰援服务端

Java 21 + Spring Boot 3.5 + Spring Security JWT + Spring Data JPA + MySQL 8 + Flyway。生产部署为 systemd + Nginx，不依赖 Docker。

## 启动

```bash
export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/maian?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export MYSQL_USER=maian
export MYSQL_PASSWORD='replace-me'
export JWT_SECRET='at-least-32-random-characters'
mvn spring-boot:run
```

正式环境不要启用 `DEMO_MODE`。比赛演示使用 `spring.profiles.active=demo`，演示身份通过短时 JWT 登录，不再信任可伪造的请求头。正式接口提供手机号密码注册/登录：

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/demo       # 仅 Demo Profile 存在
```

## 媒体与 OCR

图片由 `media_assets` 统一管理，上传后会解码重编码并清除 EXIF。体检报告和救援现场图保持私有，仅所有者、救援参与者或管理员可申请 5 分钟下载 URL；已审核设备图和科普封面才转为公开资源。生产建议：

```bash
export MEDIA_DIRECTORY=/var/lib/maian/media
export MEDIA_USE_X_ACCEL=true
export MEDIA_X_ACCEL_PREFIX=/protected-media/
```

OCR 使用百度文字识别 HTTP API，不包含或提交任何密钥。配置新申请且已轮换的凭证后才启用：

```bash
export BAIDU_OCR_ENABLED=true
export BAIDU_OCR_API_KEY='...'
export BAIDU_OCR_SECRET_KEY='...'
```

未启用 OCR 时接口明确返回 `OCR_UNAVAILABLE`，前端继续支持人工录入。只有 `healthDataShare=true` 时才允许向 OCR 或 DeepSeek 发送健康数据。DeepSeek 通过 Spring AI 的 OpenAI 兼容接口接入，默认关闭：

```bash
export APP_AI_ENABLED=true
export OPENAI_API_KEY='rotated-key'
export OPENAI_MODEL=deepseek-chat
```

## 救援工作流

呼救先落库，附件异步上传。服务端调度器独立执行重匹配与超时，不依赖客户端页面。志愿者只收到附近可接任务，接单前 DTO 不含精确坐标、地址和图片；原子接单后开放参与者数据。

固定 AED：`MATCHING → EN_ROUTE_TO_AED → EN_ROUTE_TO_REQUESTER → ARRIVED → RESCUING → PENDING_CONFIRMATION → COMPLETED → AED_RETURNED`。

移动 AED 跳过取用/归还环节。完成由施救者提交、求救者确认，10 分钟未操作则服务端自动确认。匹配时 ETA/坐标保存在快照字段，持续位置上报单独返回 `liveTracking`。

前台通过 `/api/v1/rescue-events/stream` 接收 SSE，客户端保留轮询兜底。系统级 APNs/厂商 Push 仍需部署方提供证书和设备 Token 后启用外部推送供应商。

## 验证

```bash
mvn clean test
mvn clean package
```

生产仅公开 `/actuator/health`。应用默认监听 `127.0.0.1:8080`，由 Nginx 反向代理，私有媒体目录不得直接暴露。
