#!/bin/bash
# Runs once, on first container start (empty data dir), after the mysql
# entrypoint has created $MYSQL_DATABASE. Imports SQL/*.sql in dependency
# order (users -> categories -> products -> ... ) since each dump file
# resets FOREIGN_KEY_CHECKS at its end, so plain alphabetical order breaks.
set -euo pipefail

SQL_DIR="/docker-entrypoint-initdb.d/sql"

ORDERED_FILES=(
  bigwork_users.sql
  bigwork_categories.sql
  bigwork_products.sql
  bigwork_carts.sql
  bigwork_cart_items.sql
  bigwork_orders.sql
  bigwork_order_items.sql
  bigwork_product_ratings.sql
  bigwork_wallets.sql
  bigwork_wallet_transactions.sql
  bigwork_bank_accounts.sql
  bigwork_password_reset_tokens.sql
  bigwork_user_search_tokens.sql
  bigwork_water_logs.sql
)

for f in "${ORDERED_FILES[@]}"; do
  echo "[init-db] importing $f"
  mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$SQL_DIR/$f"
done

echo "[init-db] done"
