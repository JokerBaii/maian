# 服务端发布

`deploy-server.sh` 仅更新已配置服务器上的应用文件，保留数据库、媒体文件、JWT、AI/OCR 密钥和 Nginx 配置。首次初始化与日常发布分开，避免每次发布重置登录状态或关闭已启用的服务。

首次部署先创建 `maian` 服务账号，准备 `/opt/maian`、`/var/www/maian`，把 [app.env.example](app.env.example) 复制到 `/opt/maian/app.env` 后填写随机 JWT、实际域名及需要启用的服务凭据。另建 `/opt/maian/db.env` 配置 `MYSQL_URL`、`MYSQL_USER`、`MYSQL_PASSWORD`；两个文件均由 `root:maian` 持有，权限为 `0640`。数据库账号与初始化管理员见 [服务端说明](../server/README.md)。

Nginx 应提前配置 HTTPS、H5 静态目录和 API 代理。私有媒体路由必须使用 `internal`，禁止直接公开媒体目录：

```nginx
location ^~ /protected-media/ {
    internal;
    alias /var/lib/maian/media/;
    add_header Cache-Control "private, no-store" always;
    add_header X-Content-Type-Options "nosniff" always;
}

location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_read_timeout 35m;
    proxy_buffering off;
    proxy_cache off;
}
```

每次发布准备 `/tmp/maian-server-1.0.0.jar`、`/tmp/maian-h5.tar.gz` 和 `/tmp/maian-server.service`，再以服务器管理员身份执行发布脚本。H5 压缩包根目录应包含 `index.html`。脚本先检查配置、解包验证静态资源并执行 `nginx -t`，通过后才停止后端和替换文件；不再执行旧版本 Nginx 文本迁移。

## 当前公网实例（2026-10-26）

- 应用入口：`https://121.41.195.165/maian/`；HTTP 应用路径自动跳转 HTTPS。原有 HTTP 首页继续使用原网站。
- 登录页已提供「平台管理员登录」，首页右上角「平台管理」可进入同一入口；管理员也可直接访问 `https://121.41.195.165/maian/#/pages/auth/login?portal=admin`。使用已有管理员账户的手机号和密码，登录后进入审核及账户权限功能。
- 管理员入口前端更新位于 `/opt/maian/releases/admin-entry-20261026T153759Z/`，原前端备份为同目录 `before-h5.tar.gz`。此次发布保留公网地图配置及旧版静态资源，未更换后端文件。
- 前端以 `VITE_API_BASE_URL=/maian-api npm run build:h5` 构建；Nginx 将 `/maian-api/` 转发至本机 `127.0.0.1:8080/`，避免占用现有网站接口。
- Java 21、MySQL 8、Nginx 和 `maian-server.service` 已运行，服务开机启动。MySQL 只监听本机，17 个 Flyway 迁移已在空库实际执行成功。
- 约 1.6GB 内存的实例使用 Java `-Xmx448m`、MySQL 128MB 缓冲池和 2GB swap；Java 配置位于 `/etc/systemd/system/maian-server.service.d/resources.conf`。私有媒体由后端校验签名后读取，`MEDIA_USE_X_ACCEL=false`，媒体目录不公开。
- AI 与 OCR 使用独立服务端环境配置；首个管理员创建后已移除一次性初始化参数。公网数据库独立于本机 H2，未搬迁本机业务资料或设备样例。
- 普通用户和志愿者测试账号为 `19900000001`、`19900000002`。密码仅保存在本机忽略目录 `.cache/deploy/public-20261026/test-accounts.json`；管理员凭据位于同目录 `admin-credentials.json`，均为 `0600`，不放入前端或仓库。
- 使用 Let's Encrypt IP 证书，参考[官方 Certbot 配置说明](https://letsencrypt.org/2026/03/11/shorter-certs-certbot/)。Certbot 5.8 独立安装在 `/opt/maian-certbot`；`maian-certbot-renew.timer` 每日检查两次，续期后检查并重载 Nginx。已通过 staging 签发、正式证书校验及续期 dry-run，不使用自签名证书。
- 初始数据库和原有 Nginx 配置备份在服务器 `/root/maian-backups/`；当前发布文件和脚本在 `/opt/maian/releases/20261026/`。本机校验清单、截图和测试记录在 `.cache/deploy/public-20261026/`，不包含在发布静态资源中。

已通过 8 组公网浏览器和接口检查：登录与角色、Agent 修改设置、真实模型阅读 DOCX、报告内容分流、私有签名下载、跨账号拒绝，以及授权后真实百度 OCR；HTTPS 启用后再次完整通过。另有 4 组检查通过：重启后管理员登录、字体图标、地图底图，以及 HTTPS 浏览器定位（坐标由 CDP 模拟，不代替手机实地定位验收）。临时验证账号及附件已清理，仅保留明确创建的测试账号和管理员。

常用运维命令：

```bash
systemctl status maian-server nginx mysql
journalctl -u maian-server -n 80 --no-pager
systemctl list-timers maian-certbot-renew.timer
/opt/maian-certbot/bin/certbot renew --cert-name maian-ip --dry-run --run-deploy-hooks --no-random-sleep-on-renew
curl -fsS https://121.41.195.165/maian-api/actuator/health
```
