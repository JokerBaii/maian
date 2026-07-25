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

另开终端启动后端及其基础设施：

```bash
cd server
docker compose up -d
mvn spring-boot:run
```

H5 开发服务器会把 `/api` 代理到 `http://localhost:8080`。

## 验证

```bash
npm run type-check
npm run build:ios
npm run build:h5
cd server && mvn package
```

iOS App 资源位于 `dist/build/app`，需要使用 HBuilderX 云端打包或原生工程离线打包生成 IPA。发布前需要：

- 在 `src/manifest.json` 填写 DCloud AppID；
- 配置 Apple Developer Team、Bundle ID、签名证书和描述文件；
- 将生产接口配置为 HTTPS，并填写 `VITE_API_BASE_URL`；
- 在 App Store Connect 中按实际业务申报位置、健康、身份信息、联系电话和用户上传图片；
- 在真机上验证定位、系统地图、相机/相册和 BLE 设备连接。

- 在生产环境配置持久化的 `UPLOAD_DIRECTORY`；
- H5 预览如需高德地图能力，填写 `VITE_AMAP_KEY` 与 `VITE_AMAP_SECURITY_CODE`；未配置时使用 OpenStreetMap。

Spring AI 默认关闭，没有模型密钥时健康分析使用本地规则。配置方式和接口示例见 [server/README.md](server/README.md)。
