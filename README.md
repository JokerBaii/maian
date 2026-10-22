# 脉安驰援

面向 iOS 的共享急救设备、紧急救援协同与健康监测应用。前端使用 uni-app + Vue 3，后端使用 Spring Boot 3 + Spring AI；H5 用于快速界面预览。

## 项目结构

```text
src/       uni-app 前端页面、API 客户端与本地素材
server/    Spring Boot 服务端
docs/      设计规范与素材规划
```

## 本地开发

本机测试与比赛演示可执行 `npm run dev:preview`，启动独立数据库、三种角色测试账号及救援复盘样例。访问地址、账号、测试命令和重置方法见 [演示环境说明](docs/DEMO_ENVIRONMENT.md)。

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

数据库账号可按本机实际配置调整；Flyway 会在首次启动时自动完成表结构初始化，不写入用户、救援、设备或健康记录。

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
- H5 底图默认使用高德栅格瓦片，与 App 内的 GCJ-02 坐标系一致，无需密钥；填写 `VITE_AMAP_KEY` 与 `VITE_AMAP_SECURITY_CODE` 后可启用高德 JS API 的 POI 搜索、选点与救援道路路线规划。未配置路线服务时，救援地图只显示资源和现场位置，不绘制可能穿越建筑或水面的伪路线。

Spring AI 默认关闭，没有模型密钥时健康分析使用本地规则。配置方式和接口示例见 [server/README.md](server/README.md)。

AED 调度 ETA 适用于校园、社区、场馆、园区和城市道路，不是校园专用模型。不同区域应根据实际路线、交通方式、门禁和时段重新标定，详见 [ETA 与标定说明](docs/ETA_CALIBRATION.md)。

本地构建门禁和自动化测试结果见 [项目验收报告](docs/PAGE_QA_REPORT.md)。

## 比赛演示闭环

资源协同与全局执行助手：设备变化后自动检查救援关联与替代资源；脉脉可按指令修改设置、管理联系人与设备、上传文档并办理投稿或健康报告入档，也保留入口查找与已审核文章推荐。执行规则、权限边界和演示方法见 [资源协同与助手说明](docs/AGENT_WORKFLOWS.md)。

已结束的救援详情支持“救援复盘与待办 Agent”，串联真实救援事件、AED 归还、评价与科普资料，展示工具执行过程并在业务记录变化后提示更新。

28个业务页面支持悬浮小助手「脉脉」：可拖动吸边，就地输入指令办理业务，支持连续输入补充、当前对象指代、文字确认/取消、直接传文件与站内提醒，并查看实际执行过程。功能验证、完整救援闭环和复现命令见 [助手与业务自测记录](docs/ASSISTANT_QA.md)。

登录页只提供手机号注册与密码登录。系统保留普通用户、急救志愿者和平台审核员三种门户，全部由服务端账户角色控制，不提供前端身份切换。

推荐演示顺序：

1. 普通用户注册后录入固定或移动急救设备，设备进入待审核状态。
2. 平台审核员进入“设备审核”并确认设备，设备随后进入共享地图和 AED 调度候选。
3. 普通用户发起紧急呼救，系统实时匹配就近 AED；误发可取消，设备随即释放。
4. 急救志愿者上报真实位置，只查看附近脱敏任务；响应后按取用、到场、施救和完成确认推进状态。
5. 普通用户确认救援完成并评价；固定 AED 由志愿者完成归还。
6. 普通用户上传体检报告原图，核对 OCR 与健康分析结果后保存到个人档案。
7. 普通用户投稿科普内容，平台审核员审核通过后进入科普频道。

常规 MySQL 环境不预置人物或业务记录；`dev:preview` 的虚构样例仅保存在独立演示库。首次管理员通过 `BOOTSTRAP_ADMIN_PHONE` 和 `BOOTSTRAP_ADMIN_PASSWORD` 显式创建，登录后可在“账户权限”中把已注册账户授权为志愿者或审核员。初始化密码至少 12 位；首个管理员创建成功后应移除这两个环境变量。

设备位置更新、AED 匹配、救援状态流转、救援评价、媒体权限、设备与投稿审核、健康数据读写均使用真实接口和数据库状态。体检报告识别使用百度文字识别 API，未配置凭据时明确降级为人工录入，不生成伪造结果。DeepSeek 通过 Spring AI 可选接入，两者外发前都校验 `healthDataShare` 授权。

## 部署检查

部署前必须在 `.env` 设置真机可访问的 HTTPS 接口地址，例如 `VITE_API_BASE_URL=https://api.example.com`。不要在移动端包中使用 `localhost`。`src/manifest.json` 中的 DCloud AppID、Apple Bundle ID、签名证书和描述文件必须使用参赛账号的真实配置。
