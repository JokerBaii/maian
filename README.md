# 脉安驰援

面向 iOS 的共享急救设备、紧急救援协同与健康监测应用。前端使用 uni-app + Vue 3，后端使用 Spring Boot 3 + Spring AI；H5 用于快速界面预览。

## 项目结构

```text
src/       uni-app 前端页面、API 客户端与本地素材
server/    Spring Boot 服务端
docs/      设计规范与素材规划
```

## 本地开发

复制环境变量示例并按需填写地图配置：

```bash
cp .env.example .env
npm install
npm run dev:h5
```

本机安装并启动 MySQL 8，创建开发库后另开终端启动后端：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS maian CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'maian'@'localhost' IDENTIFIED BY 'maian_dev'; GRANT ALL PRIVILEGES ON maian.* TO 'maian'@'localhost';"
cd server
export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/maian?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export MYSQL_USER=maian
export MYSQL_PASSWORD=maian_dev
mvn spring-boot:run
```

数据库账号可按本机实际配置调整；Flyway 会在首次启动时自动完成表结构和基础账号初始化，不依赖 Docker。

H5 开发服务器会把 `/api` 代理到 `http://localhost:8080`。

## 验证

```bash
npm run type-check
npm test
npm run build:ios
npm run build:h5
npm run build:mp-weixin
cd server && mvn package
```

iOS App 资源位于 `dist/build/app`，需要使用 HBuilderX 云端打包或原生工程离线打包生成 IPA。发布前需要：

- 在 `src/manifest.json` 填写 DCloud AppID；
- 配置 Apple Developer Team、Bundle ID、签名证书和描述文件；
- 将生产接口配置为 HTTPS，并填写 `VITE_API_BASE_URL`；
- 在 App Store Connect 中按实际业务申报位置、健康、身份信息、联系电话和用户上传图片；
- 在真机上验证定位、系统地图、相机/相册和 BLE 设备连接。

- 在生产环境配置持久化的 `MEDIA_DIRECTORY`，并由 Nginx `X-Accel-Redirect` 传输私有媒体；
- H5 底图默认使用高德栅格瓦片，与 App 内的 GCJ-02 坐标系一致，无需密钥；填写 `VITE_AMAP_KEY` 与 `VITE_AMAP_SECURITY_CODE` 后可启用高德 JS API 的 POI 搜索与选点。

Spring AI 默认关闭，没有模型密钥时健康分析使用本地规则。配置方式和接口示例见 [server/README.md](server/README.md)。

AED 调度 ETA 适用于校园、社区、场馆、园区和城市道路，不是校园专用模型。不同区域应根据实际路线、交通方式、门禁和时段重新标定，详见 [ETA 与标定说明](docs/ETA_CALIBRATION.md)。

121 演示环境的构建门禁、29 个注册页面实抓和双端救援小屏检查，详见 [线上页面验收报告](docs/PAGE_QA_REPORT.md)。

## 比赛演示闭环

“我的 → 演示身份切换”提供三个比赛演示身份。每次切换都由仅在 Demo Profile 存在的登录接口签发短时 JWT，后端不再信任客户端传入的用户 ID。正式 Profile 禁止 Demo 登录，使用手机号、BCrypt 密码与 JWT。

推荐演示顺序：

1. 使用“普通用户”录入固定或移动急救设备，设备进入“待平台审核”。
2. 切换“平台审核员”，进入“设备审核”并通过设备；设备随后进入地图和 AED 调度候选。
3. 切回“普通用户”发起紧急呼救，系统实时匹配就近 AED 并给出预计到达时间；误发可在详情页取消，设备随即释放。
4. 切换“救援志愿者”，先上报真实位置，只查看附近脱敏任务；确认响应后按固定 AED 取用/到场/施救/提交完成流程推进。
5. 切回“普通用户”确认完成，志愿者归还固定 AED，随后查看救援记录并评价。
6. 上传体检报告原图，核对识别出的机构、日期与关键指标后保存，查看规则分析结果与历史档案对比。
7. 投稿科普内容，切换“平台审核员”通过后，该内容出现在科普频道列表。

首次启动会载入比赛展示数据，覆盖 AED 地图、移动设备、健康趋势、体检报告、联系人和待审核投稿，便于在一台设备上完成演示闭环。正式运营前需替换为经过审核的现场数据。

演示身份切换只用于现场展示多角色业务流程。设备位置更新、AED 匹配、救援状态流转、救援评价、媒体权限、设备与投稿审核、健康数据读写均使用真实接口和数据库状态。体检报告识别使用百度文字识别 API，未配置已轮换密钥时明确降级为人工录入，不生成伪造 OCR 结果。DeepSeek 通过 Spring AI 可选接入，两者外发前都强制校验 `healthDataShare` 授权。

## 部署检查

部署前必须在 `.env` 设置真机可访问的 HTTPS 接口地址，例如 `VITE_API_BASE_URL=https://api.example.com`。不要在移动端包中使用 `localhost`。`src/manifest.json` 中的 DCloud AppID、Apple Bundle ID、签名证书和描述文件必须使用参赛账号的真实配置。
