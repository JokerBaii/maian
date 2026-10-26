#!/usr/bin/env bash
set -euo pipefail
umask 077

root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
runtime="$root/.cache/preview"
mkdir -p "$runtime/tools" "$runtime/logs"
cd "$root"

skip_build=false
if [[ ${1:-} == --skip-build ]]; then
  skip_build=true
elif [[ $# -gt 0 ]]; then
  echo '用法：bash scripts/dev-preview.sh [--skip-build]' >&2
  exit 1
fi
for tool in node npm python3 curl tar; do
  command -v "$tool" >/dev/null || { echo "缺少依赖：$tool" >&2; exit 1; }
done

# Refuse occupied ports before rebuilding or extracting libraries used by a running server.
node - <<'JS'
const net = require('node:net');
Promise.all([5173, 8080].map(port => new Promise((resolve, reject) => {
  const server = net.createServer();
  server.once('error', () => reject(new Error(`端口 ${port} 已占用，请先停止对应服务。`)));
  server.listen(port, '127.0.0.1', () => server.close(resolve));
}))).catch(error => { console.error(error.message); process.exit(1); });
JS

# Local binaries only; no system package installation or global configuration.
if [[ ! -x "$runtime/tools/jdk/bin/javac" ]]; then
  if [[ $(uname -s) != Linux || $(uname -m) != x86_64 ]]; then
    echo '自动下载支持 Linux x86_64；其他平台请将 Java 21 JDK 放到 .cache/preview/tools/jdk。' >&2
    exit 1
  fi
  curl -fL --retry 2 --connect-timeout 15 --max-time 300 \
    'https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse' \
    -o "$runtime/tools/jdk.tar.gz"
  mkdir -p "$runtime/tools/jdk"
  tar -xzf "$runtime/tools/jdk.tar.gz" -C "$runtime/tools/jdk" --strip-components=1
fi
export JAVA_HOME="$runtime/tools/jdk"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ "$skip_build" == false ]]; then
  if ! command -v mvn >/dev/null; then
    if [[ ! -x "$runtime/tools/maven/bin/mvn" ]]; then
      curl -fL --retry 2 --connect-timeout 15 --max-time 120 \
        'https://repo.huaweicloud.com/apache/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz' \
        -o "$runtime/tools/maven.tar.gz"
      mkdir -p "$runtime/tools/maven"
      tar -xzf "$runtime/tools/maven.tar.gz" -C "$runtime/tools/maven" --strip-components=1
    fi
    export PATH="$runtime/tools/maven/bin:$PATH"
  fi
  # The cached dependencies also permit rebuilding without network access.
  mvn -B -o -f server/pom.xml -DskipTests package || mvn -B -f server/pom.xml -DskipTests package
fi

jar="$root/server/target/maian-server-1.0.0.jar"
[[ -f "$jar" ]] || { echo '缺少后端构建产物，请去掉 --skip-build 后启动。' >&2; exit 1; }
[[ -d node_modules ]] || npm ci
python3 - "$jar" "$runtime/lib" "$runtime/classes" <<'PY'
import os, pathlib, shutil, sys, zipfile
destination = pathlib.Path(sys.argv[2])
destination.mkdir(parents=True, exist_ok=True)
classes = pathlib.Path(sys.argv[3])
if classes.exists():
    shutil.rmtree(classes)
classes.mkdir()
for old in destination.glob('*.jar'):
    old.unlink()
with zipfile.ZipFile(sys.argv[1]) as archive:
    for name in archive.namelist():
        if name.startswith('BOOT-INF/lib/') and name.endswith('.jar'):
            (destination / pathlib.PurePosixPath(name).name).write_bytes(archive.read(name))
        elif name.startswith('BOOT-INF/classes/') and not name.endswith('/'):
            relative = pathlib.PurePosixPath(name.removeprefix('BOOT-INF/classes/'))
            if '..' in relative.parts:
                raise ValueError('Invalid classpath archive entry')
            target = classes / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(name))
# Opt-in for source-only development after a successful Maven test/compile, without repackaging.
if os.environ.get('PREVIEW_USE_COMPILED_CLASSES') == 'true':
    compiled = pathlib.Path(sys.argv[1]).parent / 'classes'
    if not (compiled / 'cn/maian/MaianServerApplication.class').exists():
        raise ValueError('Missing compiled server classes; run Maven test first')
    shutil.copytree(compiled, classes, dirs_exist_ok=True)
PY

# H2 remains a test dependency and never enters the production server artifact.
h2=$(find "$HOME/.m2/repository/com/h2database/h2" -name 'h2-*.jar' -type f | sort -V | tail -n 1)
[[ -f "$h2" ]] || { echo '缺少 H2 测试依赖，请先执行后端 Maven 构建。' >&2; exit 1; }
if [[ ! -f "$runtime/password" ]]; then
  printf '%s' "${PREVIEW_PASSWORD:-MaianDemo2026!}" > "$runtime/password"
fi
if [[ ! -f "$runtime/jwt-secret" ]]; then
  node -e 'process.stdout.write(require("node:crypto").randomBytes(48).toString("hex"))' > "$runtime/jwt-secret"
fi
export APP_PREVIEW_PASSWORD=$(cat "$runtime/password")
export JWT_SECRET=$(cat "$runtime/jwt-secret")
preview_ai=false
if [[ -s "$runtime/ai-key" ]]; then
  export OPENAI_API_KEY=$(cat "$runtime/ai-key")
  export OPENAI_MODEL=$(cat "$runtime/ai-model" 2>/dev/null || printf '%s' 'deepseek-flash')
  preview_ai=true
fi

backend_pid=''
frontend_pid=''
cleanup() {
  trap - EXIT INT TERM
  [[ -z "$frontend_pid" ]] || kill "$frontend_pid" 2>/dev/null || true
  [[ -z "$backend_pid" ]] || kill "$backend_pid" 2>/dev/null || true
  wait 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

(
  # Load local backend credentials only into the server process.
  if [[ -f "$root/.env" ]]; then
    set -a
    source "$root/.env"
    set +a
  fi
  exec java -Dh2.bindAddress=127.0.0.1 -cp "$runtime/classes:$runtime/lib/*:$h2" \
  scripts/PreviewServer.java \
  "--app.preview.directory=$runtime" \
  "--spring.datasource.url=jdbc:h2:file:$runtime/preview;MODE=MySQL;AUTO_SERVER=TRUE" \
  --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa \
  --spring.datasource.password= --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=update \
  --server.address=127.0.0.1 --server.port=8080 --server.shutdown=immediate --debug=false --logging.level.root=INFO \
  --logging.level.org.hibernate.SQL=WARN "--app.ai.enabled=$preview_ai" \
  --app.bootstrap-admin.phone= --app.bootstrap-admin.password= "--app.media.directory=$runtime/media" \
) > "$runtime/logs/backend.log" 2>&1 &
backend_pid=$!

for attempt in {1..60}; do
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    tail -n 60 "$runtime/logs/backend.log"
    exit 1
  fi
  # ApplicationRunner must finish seeding as well as opening the HTTP socket.
  if curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null 2>&1 && \
      [[ -f "$runtime/demo.json" ]]; then
    break
  fi
  if [[ "$attempt" == 60 ]]; then
    tail -n 60 "$runtime/logs/backend.log"
    exit 1
  fi
  sleep 1
done

# Proxy always targets this isolated preview backend, even if a local .env has production values.
VITE_API_BASE_URL='' VITE_DEV_API_PROXY_TARGET='http://127.0.0.1:8080' \
  node node_modules/@dcloudio/vite-plugin-uni/bin/uni.js --host 127.0.0.1 --port 5173 --strictPort \
  > "$runtime/logs/frontend.log" 2>&1 &
frontend_pid=$!
echo '演示环境：http://localhost:5173/maian/'
echo '账号：19900000001（普通用户）、19900000002（志愿者）、19900000003（管理员）'
echo '密码保存在 .cache/preview/password；日志在 .cache/preview/logs/。按 Ctrl+C 停止前后端。'
python3 - "$runtime/demo.json" <<'PY'
import json, sys
with open(sys.argv[1]) as file:
    identifier = json.load(file)['completedRescueId']
print(f'救援复盘：http://localhost:5173/maian/#/pages/rescue/detail?id={identifier}')
PY
wait -n "$backend_pid" "$frontend_pid"
