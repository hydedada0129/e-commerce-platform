#!/usr/bin/env bash
# Pushes the bigwork app's credential + datasource settings into Consul KV at
# config/bigwork/data, plus the flat config/bigwork/mysql/* keys the MySQL
# container's own entrypoint (docker/mysql-consul-entrypoint.sh) reads to
# bootstrap its account.
# Reads values from the caller's shell env vars — never hardcode secrets in this file.
set -euo pipefail

CONSUL_ADDR="${CONSUL_HTTP_ADDR:-http://localhost:8500}"

required_vars=(
  MYSQL_ROOT_PASSWORD
  DB_USERNAME DB_PASSWORD
  MAIL_USERNAME MAIL_PASSWORD
  JWT_SECRET
  ECPAY_MERCHANT_ID ECPAY_HASH_KEY ECPAY_HASH_IV
  DATA_ENCRYPTION_KEY
)

missing=()
for v in "${required_vars[@]}"; do
  if [ -z "${!v:-}" ]; then
    missing+=("$v")
  fi
done
if [ ${#missing[@]} -ne 0 ]; then
  echo "Missing required env vars: ${missing[*]}" >&2
  exit 1
fi

# Not secrets — connection topology / pool tuning. Optional, sensible local-dev defaults.
DB_URL="${DB_URL:-jdbc:mysql://localhost:3306/bigwork?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true}"
DB_POOL_MAX="${DB_POOL_MAX:-10}"
DB_POOL_MIN="${DB_POOL_MIN:-2}"

payload=$(cat <<EOF
spring:
  datasource:
    url: "${DB_URL}"
    username: "${DB_USERNAME}"
    password: "${DB_PASSWORD}"
    hikari:
      maximum-pool-size: ${DB_POOL_MAX}
      minimum-idle: ${DB_POOL_MIN}
  mail:
    username: "${MAIL_USERNAME}"
    password: "${MAIL_PASSWORD}"
jwt:
  secret: "${JWT_SECRET}"
ecpay:
  merchantId: "${ECPAY_MERCHANT_ID}"
  hashKey: "${ECPAY_HASH_KEY}"
  hashIV: "${ECPAY_HASH_IV}"
security:
  data-encryption-key: "${DATA_ENCRYPTION_KEY}"
EOF
)

curl -sf --request PUT "${CONSUL_ADDR}/v1/kv/config/bigwork/data" --data-binary "${payload}" > /dev/null

curl -sf --request PUT "${CONSUL_ADDR}/v1/kv/config/bigwork/mysql/root-password" --data-binary "${MYSQL_ROOT_PASSWORD}" > /dev/null
curl -sf --request PUT "${CONSUL_ADDR}/v1/kv/config/bigwork/mysql/username" --data-binary "${DB_USERNAME}" > /dev/null
curl -sf --request PUT "${CONSUL_ADDR}/v1/kv/config/bigwork/mysql/password" --data-binary "${DB_PASSWORD}" > /dev/null

echo "Wrote config/bigwork/data and config/bigwork/mysql/* to Consul at ${CONSUL_ADDR} (values not printed)."
