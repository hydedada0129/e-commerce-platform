#!/bin/bash
# Fetches MySQL's own bootstrap account (root password, app DB user/pass) from
# Consul KV before handing off to the real MySQL entrypoint. Only matters on
# first init of an empty data volume — docker-entrypoint.sh ignores these env
# vars entirely once the datadir already exists.
set -euo pipefail

CONSUL_ADDR="${CONSUL_HTTP_ADDR:-http://consul:8500}"
TIMEOUT=60
INTERVAL=2
ELAPSED=0

fetch() {
  curl -sf "${CONSUL_ADDR}/v1/kv/$1?raw" 2>/dev/null || true
}

echo "Waiting for MySQL bootstrap credentials in Consul KV at ${CONSUL_ADDR}..."
while true; do
  ROOT_PW="$(fetch config/bigwork/mysql/root-password)"
  APP_USER="$(fetch config/bigwork/mysql/username)"
  APP_PW="$(fetch config/bigwork/mysql/password)"

  if [ -n "$ROOT_PW" ] && [ -n "$APP_USER" ] && [ -n "$APP_PW" ]; then
    break
  fi
  if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
    echo "Timed out waiting for config/bigwork/mysql/{root-password,username,password} in Consul KV." >&2
    echo "Run docker/consul-seed.sh (with MYSQL_ROOT_PASSWORD, DB_USERNAME, DB_PASSWORD set) first." >&2
    exit 1
  fi
  sleep "$INTERVAL"
  ELAPSED=$((ELAPSED + INTERVAL))
done

export MYSQL_ROOT_PASSWORD="$ROOT_PW"
export MYSQL_USER="$APP_USER"
export MYSQL_PASSWORD="$APP_PW"

# Healthcheck runs as a separate `docker exec`-style process and does not see
# vars this script `export`s, so hand it the root password via a root-only file.
umask 077
printf '%s' "$ROOT_PW" > /tmp/mysql-root-pw

echo "Fetched MySQL bootstrap credentials from Consul (values not printed)."
exec docker-entrypoint.sh mysqld
