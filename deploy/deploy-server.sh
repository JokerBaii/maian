#!/usr/bin/env bash
set -euo pipefail

test -d /var/www/maian
test -f /tmp/maian-server-1.0.0.jar
test -f /tmp/maian-h5.tar.gz

install -d -o maian -g maian -m 750 /var/lib/maian/media
install -o root -g maian -m 640 /dev/null /opt/maian/app.env
jwt_value="$(openssl rand -hex 48)"
printf '%s\n' \
  'SPRING_PROFILES_ACTIVE=demo' \
  'DEMO_MODE=true' \
  "JWT_SECRET=${jwt_value}" \
  'JWT_ISSUER=maian-server' \
  'JWT_ACCESS_TOKEN_MINUTES=15' \
  'SERVER_ADDRESS=127.0.0.1' \
  'SERVER_PORT=8080' \
  'CORS_ALLOWED_ORIGINS=https://121.41.195.165,http://121.41.195.165' \
  'MEDIA_DIRECTORY=/var/lib/maian/media' \
  'MEDIA_USE_X_ACCEL=true' \
  'MEDIA_X_ACCEL_PREFIX=/protected-media/' \
  'MEDIA_CAPACITY_BYTES=21474836480' \
  'MEDIA_WARNING_RATIO=0.70' \
  'MEDIA_REJECTION_RATIO=0.85' \
  'APP_AI_ENABLED=false' \
  'OPENAI_API_KEY=disabled' \
  'BAIDU_OCR_ENABLED=false' \
  'BAIDU_OCR_API_KEY=' \
  'BAIDU_OCR_SECRET_KEY=' \
  > /opt/maian/app.env
unset jwt_value

install -o root -g root -m 644 /tmp/maian-server.service /etc/systemd/system/maian-server.service

sed -i '/proxy_set_header X-Demo-User-Id/d' /etc/nginx/conf.d/maian.conf
perl -0pi -e 's@\n    location /uploads/ \{.*?\n    \}\n@\n    location ^~ /protected-media/ {\n        internal;\n        alias /var/lib/maian/media/;\n        add_header Cache-Control "private, no-store" always;\n        add_header X-Content-Type-Options "nosniff" always;\n    }\n@s' /etc/nginx/conf.d/maian.conf
sed -i 's/proxy_read_timeout 60s;/proxy_read_timeout 35m;/' /etc/nginx/conf.d/maian.conf
if ! grep -q 'proxy_cache off;' /etc/nginx/conf.d/maian.conf; then
  sed -i '/location \/api\/ {/a\        proxy_buffering off;\n        proxy_cache off;' /etc/nginx/conf.d/maian.conf
fi
nginx -t

systemctl stop maian-server
install -o maian -g maian -m 640 /tmp/maian-server-1.0.0.jar /opt/maian/maian-server.jar
find /var/www/maian -mindepth 1 -depth -delete
tar -xzf /tmp/maian-h5.tar.gz -C /var/www/maian
chown -R www-data:www-data /var/www/maian
find /var/www/maian -type d -exec chmod 755 {} +
find /var/www/maian -type f -exec chmod 644 {} +

systemctl daemon-reload
systemctl enable maian-server >/dev/null
systemctl start maian-server
systemctl reload nginx

rm -f /tmp/maian-server-1.0.0.jar /tmp/maian-h5.tar.gz /tmp/maian-server.service /tmp/deploy-server.sh
