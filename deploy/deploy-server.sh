#!/usr/bin/env bash
set -euo pipefail

test -d /var/www/maian
test -f /tmp/maian-server-1.0.0.jar
test -f /tmp/maian-h5.tar.gz

# Provision accounts, database, secrets and Nginx separately; a release must preserve them.
test -s /opt/maian/app.env
test -s /opt/maian/db.env
test -f /tmp/maian-server.service

web_stage="$(mktemp -d /var/www/.maian-release.XXXXXX)"
trap 'rm -rf "$web_stage"' EXIT
tar -xzf /tmp/maian-h5.tar.gz -C "$web_stage"
test -s "$web_stage/index.html"
chown -R www-data:www-data "$web_stage"
find "$web_stage" -type d -exec chmod 755 {} +
find "$web_stage" -type f -exec chmod 644 {} +

install -d -o maian -g maian -m 750 /var/lib/maian/media
install -o root -g root -m 644 /tmp/maian-server.service /etc/systemd/system/maian-server.service

nginx -t

systemctl stop maian-server
install -o maian -g maian -m 640 /tmp/maian-server-1.0.0.jar /opt/maian/maian-server.jar
find /var/www/maian -mindepth 1 -depth -delete
cp -a "$web_stage/." /var/www/maian/

systemctl daemon-reload
systemctl enable maian-server >/dev/null
systemctl start maian-server
systemctl reload nginx

rm -f /tmp/maian-server-1.0.0.jar /tmp/maian-h5.tar.gz /tmp/maian-server.service /tmp/deploy-server.sh
