<div align="center">

# 🚑 脉安驰援（MaiAn Rescue）

**面向移动端的共享急救设备、紧急救援协同与健康监测平台**

共享急救资源 · AED 智能调度 · 志愿者协同 · 健康档案 · AI 智能助手

![Platform](https://img.shields.io/badge/Platform-App%20%7C%20H5%20%7C%20WeChat%20Mini%20Program-0ea5e9?style=flat-square)
![Frontend](https://img.shields.io/badge/Frontend-uni--app%20%2B%20Vue%203-42b883?style=flat-square)
![Backend](https://img.shields.io/badge/Backend-Spring%20Boot%203-6db33f?style=flat-square)
![Database](https://img.shields.io/badge/Database-MySQL%208-4479a1?style=flat-square)
![AI](https://img.shields.io/badge/AI-Spring%20AI%20%2B%20DeepSeek-7c3aed?style=flat-square)
![OCR](https://img.shields.io/badge/OCR-Baidu%20OCR-f97316?style=flat-square)
![Map](https://img.shields.io/badge/Map-AMap%20%7C%20GCJ--02-10b981?style=flat-square)

[核心功能](#-核心功能) · [技术架构](#-技术架构) · [快速开始](#-快速开始) · [比赛演示](#-比赛演示) · [部署指南](#-部署指南) · [项目文档](#-项目文档)

</div>

---

## 📖 项目简介

**脉安驰援** 是面向移动场景的综合急救服务平台，围绕急救资源共享、紧急呼救、AED 匹配调度、志愿者协同救援以及个人健康管理，提供从设备登记与审核到救援完成与复盘的业务闭环。

项目采用 **uni-app + Vue 3** 构建多端前端，使用 **Spring Boot 3** 提供业务接口，并通过 **Spring AI** 按需接入智能分析与工具执行能力。H5 用于快速界面预览和开发调试。

平台设有 **普通用户、急救志愿者、平台审核员** 三类门户，角色与权限由服务端账户控制，不支持在前端直接切换身份。

## ✨ 核心功能

### 🩺 共享急救设备管理

- 登记固定或移动急救设备，维护设备位置与状态。
- 平台审核通过后，设备进入共享地图和 AED 调度候选集合。
- 支持设备取用、归还、释放以及资源变化后的救援关联检查。
- 资源异常时检查可用替代设备，辅助后续业务处理。

### 🚨 紧急呼救与 AED 智能调度

- 发起紧急呼救，根据位置和实际设备状态匹配附近可用 AED。
- 支持误发取消、关联设备释放和救援任务状态跟踪。
- 基于实际路线与区域标定参数估计 AED 调度 ETA。
- 缺少路线服务时，仅展示资源与现场位置，不绘制可能穿越建筑或水面的伪路线。

### 🤝 志愿者协同救援

- 志愿者上报真实位置，按权限查看附近脱敏救援任务。
- 响应任务后，按 **取用 → 到场 → 施救 → 完成确认** 推进业务状态。
- 普通用户可确认救援完成并评价；固定 AED 支持志愿者归还流程。

### 🧾 健康档案与报告识别

- 上传体检报告原图，使用百度文字识别 API 解析报告内容。
- 对 OCR 结果和健康分析结果进行人工核对后保存至个人档案。
- 未配置 OCR 凭据时，明确降级为人工录入，不生成虚构识别结果。
- 健康分析默认采用本地规则；配置 Spring AI 后可选用模型服务。
- 向第三方服务发送健康信息前，校验 `healthDataShare` 授权。

### 🤖 AI 智能助手「脉脉」

- 已在 **28 个业务页面** 集成可拖动、吸边的悬浮助手。
- 支持自然语言指令、连续补充、当前对象指代和文字确认 / 取消。
- 支持在授权范围内修改设置、管理联系人与设备、上传文档、办理投稿和健康报告入档。
- 提供业务入口查找、已审核文章推荐、站内提醒与工具执行过程展示。
- 提供资源协同与 **救援复盘与待办 Agent**，关联救援事件、AED 归还、评价及科普资料，并在业务记录变化时提示更新。

### 📰 科普投稿与后台审核

- 普通用户提交急救科普文章，审核通过后在科普频道展示。
- 平台审核员负责设备审核、内容审核和账户角色授权。
- 业务权限在服务端校验，不依赖前端显示状态进行授权。

## 🏗️ 技术架构

| 层次 | 技术方案 | 用途 |
| --- | --- | --- |
| 多端前端 | uni-app、Vue 3 | App / H5 / 微信小程序页面与交互 |
| 后端服务 | Spring Boot 3 | 账户、设备、救援、健康等业务接口 |
| AI 能力 | Spring AI、DeepSeek（可选） | 健康分析与智能助手扩展 |
| 数据库 | MySQL 8 | 业务数据持久化 |
| 数据迁移 | Flyway | 数据库结构版本管理 |
| 文字识别 | 百度文字识别 API | 体检报告 OCR |
| 地图服务 | 高德地图、GCJ-02 | 资源展示、POI、选点与路线规划 |
| 工程构建 | npm、Maven | 前后端构建与验证 |
| 服务部署 | Nginx、Spring Boot | HTTPS 接入与私有媒体传输 |

```text
       📱 App / 🌐 H5 / 🟢 微信小程序
                    │
             uni-app + Vue 3
                    │
                HTTPS / API
                    │
              Spring Boot 3
         ┌──────────┼──────────┐
         │          │          │
      业务服务    Spring AI   第三方接口
         │          │          ├── 高德地图
         │       DeepSeek      └── 百度 OCR
         │       （可选）
         ▼
      MySQL 8
       Flyway
```

## 📂 项目结构

```text
.
├── src/                 # uni-app 前端页面、API 客户端与本地素材
├── server/              # Spring Boot 服务端
├── scripts/             # 本机预览、数据准备与回归检查
├── deploy/              # 服务端配置模板与发布脚本
├── docs/                # 设计规范、工作流与验收记录
├── .env.example         # 环境变量示例
├── package.json         # 前端依赖与脚本
└── README.md            # 项目说明
```

## 🚀 快速开始

### 1. 环境准备

准备项目兼容的 Node.js / npm、JDK / Maven 以及 **MySQL 8**。移动端打包还需要相应平台的构建工具与签名环境。

### 2. 安装前端依赖

在项目根目录执行：

```bash
cp .env.example .env
npm install
```

按需填写 `.env` 中的接口和地图配置。

### 3. 启动 H5 开发服务器

```bash
npm run dev:h5
```

开发服务器将 `/api` 代理至 `http://localhost:8080`。

### 4. 初始化本地数据库

先安装并启动 MySQL 8，再创建开发数据库与账户：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS maian CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'maian'@'localhost' IDENTIFIED BY 'maian_dev'; GRANT ALL PRIVILEGES ON maian.* TO 'maian'@'localhost';"
```

以上仅为**本地开发示例凭据**，请勿直接用于生产环境。

### 5. 启动 Spring Boot 后端

在另一个终端执行：

```bash
cd server
export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/maian?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export MYSQL_USER=maian
export MYSQL_PASSWORD=maian_dev
mvn spring-boot:run
```

Flyway 会在首次启动时初始化数据库表结构，**不会自动写入真实或虚构的用户、救援、设备及健康记录**。后端环境变量和接口示例见 [服务端说明](server/README.md)。

## 🎬 比赛演示

项目提供独立的本机预览环境：

```bash
npm run dev:preview
```

该环境启动独立演示数据库、三类角色测试账号及救援复盘样例；虚构样例仅保存在演示库，不污染普通开发数据库。

**推荐演示闭环：**

1. **设备登记：** 普通用户注册后录入固定或移动 AED，设备进入待审核状态。
2. **平台审核：** 审核员核准设备，设备出现在共享地图并进入调度候选。
3. **紧急呼救：** 普通用户发起求助，系统实时匹配附近 AED；误发时可取消并释放设备。
4. **协同救援：** 志愿者上报位置、查看附近脱敏任务，响应后完成取用、到场、施救及确认。
5. **救援收尾：** 用户确认完成并评价，志愿者归还固定 AED。
6. **健康管理：** 用户上传体检报告原图，核对 OCR 与健康分析结果后存入档案。
7. **科普审核：** 用户投稿急救科普内容，审核通过后在科普频道展示。

访问地址、账号、测试命令与重置方法见 [演示环境说明](docs/DEMO_ENVIRONMENT.md)。

## 🧪 构建与验证

在项目根目录执行以下前端检查与构建命令：

```bash
npm run type-check
npm test
npm run build:ios
npm run build:h5
npm run build:mp-weixin
```

后端打包：

```bash
cd server
mvn package
```

全项目回归结果、清理范围与实机验证边界参见 [项目清理与回归记录](docs/PAGE_QA_REPORT.md)。

## 🔐 账户与数据安全

- 登录页仅提供**手机号注册与密码登录**。
- 角色包括普通用户、急救志愿者和平台审核员，均由**服务端账户角色**决定。
- 首次管理员通过 `BOOTSTRAP_ADMIN_PHONE` 与 `BOOTSTRAP_ADMIN_PASSWORD` **显式初始化**，初始化密码至少 **12 位**。
- 创建首位管理员后应移除这两个环境变量；此后可通过「账户权限」授权已注册账户。
- 媒体访问、设备与投稿审核、救援状态流转、健康数据读写均依赖真实接口和持久化状态。
- 生产环境应配置持久化 `MEDIA_DIRECTORY`，由 Nginx `X-Accel-Redirect` 传输私有媒体。
- OCR 和可选模型服务对外传输健康信息前均须校验 `healthDataShare` 授权。

## 🗺️ 地图服务与 ETA 说明

H5 底图默认使用高德栅格瓦片，与 App 内 **GCJ-02** 坐标系保持一致。基础底图无需填写地图密钥。

配置以下变量后，可使用高德 JS API 的 POI 搜索、选点与救援道路路线规划：

```dotenv
VITE_AMAP_KEY=your_amap_key
VITE_AMAP_SECURITY_CODE=your_amap_security_code
```

> **注意：** 未配置路线服务时，救援地图仅展示资源和现场位置，不绘制未经道路规划验证的直线路线。

AED 调度 ETA 支持校园、社区、场馆、园区及城市道路等场景，**并非校园专用模型**。切换部署场景时，应根据真实道路、交通方式、门禁和时段重新标定。详见 [ETA 与标定说明](docs/ETA_CALIBRATION.md)。

## 📦 部署指南

### 生产接口与服务配置

在 `.env` 中配置真机可访问的 HTTPS API 地址：

```dotenv
VITE_API_BASE_URL=https://api.example.com
```

**移动端安装包不能使用 `localhost` 指向后端**。同时需要配置生产数据库、持久化媒体目录、Nginx 私有媒体转发及对应 HTTPS 证书。具体步骤见 [部署说明](deploy/README.md)。

### iOS 构建与发布

`npm run build:ios` 生成的 App 资源位于 `dist/build/app`，还需要通过 **HBuilderX 云端打包** 或原生工程离线打包得到 IPA。

发布前请完成以下检查：

- 在 `src/manifest.json` 填写项目实际的 **DCloud AppID**。
- 配置 Apple Developer Team、Bundle ID、签名证书及描述文件。
- 确保生产 API 使用 HTTPS，且移动设备可以实际访问。
- 在 App Store Connect 中如实申报位置、健康、身份信息、联系电话及用户上传图片等数据处理情况。
- 在真机上验证定位、系统地图、相机 / 相册及 **BLE** 设备连接。

## 📚 项目文档

| 文档 | 内容 |
| --- | --- |
| [演示环境说明](docs/DEMO_ENVIRONMENT.md) | 演示启动、账号、测试命令与重置 |
| [服务端说明](server/README.md) | 后端运行、Spring AI 配置与接口示例 |
| [ETA 与标定说明](docs/ETA_CALIBRATION.md) | AED 调度模型适用范围与参数标定 |
| [资源协同与助手说明](docs/AGENT_WORKFLOWS.md) | Agent 工具执行、权限边界与演示流程 |
| [助手与业务自测记录](docs/ASSISTANT_QA.md) | 28 页面助手、救援闭环与功能验证 |
| [项目清理与回归记录](docs/PAGE_QA_REPORT.md) | 回归结果、清理范围及验证边界 |
| [部署说明](deploy/README.md) | 服务端配置与发布流程 |

---

<div align="center">

**🚑 脉安驰援 · 让急救资源更易触达，让协同救援更加高效**

</div>
