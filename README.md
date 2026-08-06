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

- 在生产环境配置持久化的 `UPLOAD_DIRECTORY`；
- H5 底图默认使用高德栅格瓦片，与 App 内的 GCJ-02 坐标系一致，无需密钥；填写 `VITE_AMAP_KEY` 与 `VITE_AMAP_SECURITY_CODE` 后可启用高德 JS API 的 POI 搜索与选点。

Spring AI 默认关闭，没有模型密钥时健康分析使用本地规则。配置方式和接口示例见 [server/README.md](server/README.md)。

## 比赛演示闭环

“我的 → 演示身份切换”提供三个本地演示身份。前端通过 `X-Demo-User-Id` 请求头切换数据视角，仅用于在一台移动设备上完成现场演示，不是生产登录方案。

推荐演示顺序：

1. 使用“普通用户”录入固定或移动急救设备，设备进入“待平台审核”。
2. 切换“平台审核员”，进入“设备审核”并通过设备；设备随后进入地图和 AED 调度候选。
3. 切回“普通用户”发起紧急呼救，系统实时匹配就近 AED 并给出预计到达时间；误发可在详情页取消，设备随即释放。
4. 切换“救援志愿者”，进入“救援任务”，依次执行接单、赶往现场和完成救援。
5. 切回“普通用户”，查看救援详情与记录中的状态变化，并对本次救援做星级评价。
6. 上传体检报告原图，核对识别出的机构、日期与关键指标后保存，查看规则分析结果与历史档案对比。
7. 投稿科普内容，切换“平台审核员”通过后，该内容出现在科普频道列表。

首次启动会载入比赛展示数据，覆盖 AED 地图、移动设备、健康趋势、体检报告、联系人和待审核投稿，便于在一台设备上完成演示闭环。正式运营前需替换为经过审核的现场数据。

演示身份切换只用于现场展示多角色业务流程。设备位置更新、AED 匹配、救援状态流转、救援评价、设备与投稿审核、健康数据读写均使用真实接口和数据库状态。体检报告识别当前返回预置指标样例（按图片稳定选取），需人工核对后保存，尚未接入 OCR 引擎；健康分析默认使用本地规则，未配置模型密钥时不调用外部 AI。

## 部署检查

部署前必须在 `.env` 设置真机可访问的 HTTPS 接口地址，例如 `VITE_API_BASE_URL=https://api.example.com`。不要在移动端包中使用 `localhost`。`src/manifest.json` 中的 DCloud AppID、Apple Bundle ID、签名证书和描述文件必须使用参赛账号的真实配置。
