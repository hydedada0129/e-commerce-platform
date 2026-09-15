# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A multi-seller e-commerce marketplace demo (like Shopee/Amazon), split into two independently-run, decoupled parts:

- **`bigwork/`** — Java 21 / Spring Boot 3.5.7 backend. RESTful API, JWT auth, JPA/Hibernate over MySQL.
- **`frontend-html/`** — Vanilla JavaScript (ES6+ async/await) + HTML5/CSS3 frontend, no framework, no build step. Talks to the backend purely via `fetch`.

There is no shared build system between the two — they run as separate processes and communicate over HTTP/CORS.

## Commands

All backend commands run from `bigwork/`:

```bash
./mvnw spring-boot:run          # run the backend (localhost:8080)
./mvnw compile                  # compile only
./mvnw test                     # run all tests
./mvnw test -Dtest=ClassName    # run a single test class
./mvnw package                  # build the jar
```

There is currently only one test (`BigworkApplicationTests`, a context-load smoke test) — no meaningful test suite exists yet.

Local infrastructure runs from the repo root (`docker-compose.yml`):

```bash
docker compose up -d consul mysql   # start Consul + MySQL (mysql waits on consul's healthcheck)
docker/consul-seed.sh               # push app + MySQL-bootstrap credentials into Consul KV (see below)
```

## Local Infrastructure (`docker-compose.yml`)

Two services (`consul`, `mysql`), no app container — the Spring Boot app itself still runs on the host via `./mvnw spring-boot:run`. Every credential flows through Consul KV, never through a `.env` file or docker-compose variable substitution. Fresh-clone startup order: `docker compose up -d consul` → `docker/consul-seed.sh` (see Configuration & Secrets) → `docker compose up -d mysql` → `./mvnw spring-boot:run`; on an already-initialized volume, `docker compose up -d consul mysql` can run together.

**Full details (MySQL entrypoint wiring, healthcheck/polling behavior, volume-init semantics) → `docs/本地開發環境.md`.**

## Development Practices

- **TDD**: write a failing test first, then implement until it passes. `spring-boot-starter-test` and `spring-security-test` are already on the classpath (JUnit 5, Mockito, `@SpringBootTest`/`@WebMvcTest`/`@DataJpaTest`) — use them; no new test dependencies should be needed. Since the suite is currently just the one smoke test, new features should build up real coverage as they land rather than deferring it.
- **SOLID**: keep the existing layering honest —
  - controllers stay thin and delegate to `service/`; put business logic in `service/Impl/`, not the controller.
  - depend on the `service/*Service` interfaces (constructor-injected, matching the existing pattern), not directly on `Impl` classes — this is what makes services mockable in tests.
  - one reason to change per class: a new responsibility (e.g. masking/encrypting a field) belongs in its own small collaborator (e.g. a converter/util invoked by the service or a JPA `AttributeConverter`), not bolted onto an existing service method or entity getter/setter.
  - extend via new implementations/strategies rather than branching on type flags inside existing classes.
- **Clean Code**:
  - **Names reveal intent**: a variable/method/class name should make its purpose obvious without reading the implementation (`MaskingUtil`, `NGramTokenizer`, `RandomIvStringConverter` are good examples already in this codebase). Avoid abbreviations that need a comment to decode.
  - **One function, one job**: a method should operate at one level of abstraction. If a service method is simultaneously validating input, running business logic, and assembling a response shape, split it into private helpers or extract a collaborator — this is the same instinct as the SOLID "one reason to change" rule above, applied at method granularity.
  - **Guard clauses over nesting**: prefer early `return`/`throw` for invalid states over multi-level `if/else` nesting; the validation logic in `OrderServiceImpl`/`AdminReportServiceImpl` is a reasonable model to follow.
  - **Comments explain "why", never "what"**: the code itself should say what it does. Only comment where there's a non-obvious constraint, workaround, or invariant a future editor could easily break (e.g. "why the three sub-keys must stay independent" in the encryption doc) — never a restatement of the line below it.
  - **No magic numbers/strings**: use the existing enums (`OrderStatus`, `UserRole`, `TransactionType`) for status/role/transaction-type comparisons instead of comparing against raw string literals.
  - **Handle errors honestly**: don't swallow exceptions (`catch (Exception e) {}` with no action or log-only), and don't let one broad `catch (Exception e)` block flatten error types that callers actually need to distinguish. The controller pattern of catching and mapping to an HTTP status (e.g. `AuthController`) is worth following, but avoid returning the same generic message for every failure mode.
  - **DRY, but don't over-abstract**: three similar lines are better than a one-off abstraction used only once. Extract a shared helper only once the same logic shows up a third time and is clearly going to be reused again, not preemptively.

Frontend: no build step. Open `frontend-html/html/*.html` with a static server (e.g. VS Code "Live Server" on port 5500). There is no npm/package.json — it's plain HTML/CSS/JS served statically.

## Configuration & Secrets

- `bigwork/src/main/resources/application.yml` has no `spring.datasource` block, and no `spring.mail`/`jwt.secret`/`ecpay.*` credential fields — all of it comes from Consul KV via `spring.config.import: "consul:localhost:8500"` (`config/bigwork/data`). **Never hardcode any of these back into `application.yml`.** `fail-fast: true` — the app refuses to start if Consul is unreachable, by design.
- `docker/consul-seed.sh` is what pushes credentials into Consul KV; it requires `MYSQL_ROOT_PASSWORD`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET`, `ECPAY_MERCHANT_ID`, `ECPAY_HASH_KEY`, `ECPAY_HASH_IV`, `DATA_ENCRYPTION_KEY` set in your shell env, and must be re-run after changing any of those.
- `spring.jpa.hibernate.ddl-auto=validate` — schema is **not** auto-generated. It's created/managed manually via `SQL/*.sql`; if you change a JPA entity's mapping, the corresponding `.sql` file and live schema must be updated to match, or the app fails to start.
- `ECPAY_RETURN_URL` and `MYSQL_DATABASE` are the two deliberate exceptions that stay as plain (non-Consul) values, since they're environment-specific, not credentials.
- Consul here is a local dev-mode single-node agent with no ACLs — not a production secrets store. Production should follow the AWS Secrets Manager path in `docs/資料保護策略總表.md`.

**Full details (exact Consul KV paths, ngrok/ECPay callback wiring, which values are optional vs. required) → `docs/設定與機敏資訊.md`.**

## Backend Architecture (`bigwork/src/main/java/demo/bigwork/`)

Standard layered architecture, one package per layer:

- `controller/` — REST endpoints, thin, delegate to services.
- `service/` + `service/Impl/` — interfaces and implementations; business logic and transaction boundaries (`@Transactional`) live here.
- `dao/` — Spring Data JPA repositories.
- `model/po/` — persistent entities (JPA `@Entity`), suffixed `PO`.
- `model/vo/` — request/response DTOs, suffixed `VO`, used at the controller boundary so entities never leak directly to JSON.
- `model/enums/` — `OrderStatus`, `TransactionType`, `UserRole`.
- `config/` — `SecurityConfig` (JWT filter chain + RBAC rules), `WebConfig` (serves `/uploads/**` from the local `uploads/` folder as static files), `JwtAuthenticationFilter`, `StaticResourceConfig`.
- `util/` — `JwtUtil`, `ECPayUtil` (SHA-256 checksum handling for ECPay).

### Auth & RBAC

- Stateless JWT auth (`spring-boot-starter-security` + `jjwt`), no sessions, CSRF disabled (`SecurityConfig`).
- Roles: `ADMIN`, `BUYER`, `SELLER`, plus public/unauthenticated endpoints. URL-based authorization is centralized in `SecurityConfig.filterChain` (e.g. `/api/admin/**` → `ADMIN`, `/api/cart/**`/`/api/orders/**` → `BUYER`, `/api/products/**`/`/api/seller/**` → `SELLER`, `/api/public/**` and `/api/auth/**` → open). `@EnableMethodSecurity` is also on, so `@PreAuthorize` can be used at the method level when URL patterns aren't precise enough.
- Admin login uses a separate `admin_code` field on the `users` table rather than a distinct table.
- When adding a new endpoint, register its access rule in `SecurityConfig` explicitly — the catch-all is `anyRequest().authenticated()`, so unlisted endpoints default to "any logged-in user," not to the role you might expect.

### Field-level encryption & masking (`demo.bigwork.crypto`)

Four columns are encrypted at rest with AES-256-GCM, transparently via JPA `AttributeConverter`s — `UserPO.phone`, `UserPO.defaultAddress`, `UserPO.adminCode`, `BankAccountPO.accountNumber`.

- Two converter variants: `RandomIvStringConverter` (random IV, used for `phone`/`defaultAddress`/`accountNumber` — never queried by exact match) and `DeterministicStringConverter` (IV derived from the plaintext via HMAC, used **only** for `adminCode` since `UserDAO.findByAdminCode` needs exact-match lookups against a `UNIQUE KEY` column).
- Masking is a separate concern from encryption: encryption is transparent everywhere (every reader gets plaintext back), while masking (`MaskingUtil`) is applied explicitly, only at VO-construction time, only for admin list views (`AdminUserSummaryVO`). Self-service views always show plaintext.
- **No SQL `LIKE` on encrypted columns — ever.** Fuzzy search instead goes through a blind-index table (`user_search_tokens`, HMAC-hashed n-grams) — see `docs/欄位加密與模糊搜尋.md` for the full design, why it's correct (no false negatives), its cost tradeoffs, and the fixed local-dev seed key.

**Full details (key derivation, the blind-index search mechanism, seed-data regeneration) → `docs/欄位加密與模糊搜尋.md`.**

### Order checkout & multi-seller split (core domain logic)

`OrderServiceImpl` is the most business-critical class. Checkout (`checkoutFromMyCart`) does **not** create one order per cart — it groups cart items by `seller_id` and creates one `OrderPO` (with its own `OrderItemPO` children) per seller, splitting stock deduction and wallet crediting per seller inside a single `@Transactional` method. `processEcpayCheckout` mirrors this same split logic for the ECPay payment path (difference: buyer's wallet is *not* debited since ECPay collected the money externally; only seller wallets are credited).

`OrderItemPO` stores a **price snapshot** (`pricePerUnit`) at order-creation time, decoupled from the live `ProductPO.price`, so historical orders don't change if a seller edits their price later. `ProductRatingPO` links to `OrderItemPO` (not directly to `ProductPO`) — this is intentional, enforcing that only buyers who actually purchased and received an item can rate it.

Because cart/order entity collections are LAZY, service methods manually call `Hibernate.initialize(...)` on collections before converting `PO` → `VO` inside the transaction — VOs must never be built from a detached/lazy entity outside a transaction.

### Payments

Two independent payment paths:
1. **Internal wallet** — `WalletPO`/`WalletTransactionPO`, simple balance debit/credit with a `TransactionType` (`TOPUP`, `PAYMENT`/`PURCHASE`, `WITHDRAWAL`, `PAYMENT_RECEIVED`).
2. **ECPay (綠界)** — `ECPayController` builds/redirects to the checkout form, `NotifyController` receives the async webhook at `/notify`, verified via SHA-256 checksum (`ECPayUtil`, using `commons-codec`). This endpoint has no auth (external caller) — do not add JWT requirements to it.

### Categories

`CategoryPO` is self-referencing (`parent_category_id`) to form an arbitrary-depth tree. Querying a parent category recursively pulls products from all descendant categories (see `CategoryService`/`CategoryServiceImpl`) — keep this in mind when changing category queries, since a naive `category_id = ?` filter will silently drop child-category products.

### File uploads

Product images are stored on local disk under `bigwork/uploads/` (via `FileStorageServiceImpl`) and served back as static resources through `WebConfig`'s `/uploads/**` resource handler — they are not stored in the DB, only the path/filename is (`ProductPO.imageUrl`).

## Frontend Architecture (`frontend-html/`)

- Each `html/*.html` page has a matching `js/*.js` file of the same base name (e.g. `products.html` ↔ `products.js`, `cart.html` ↔ `cart.js`) — this is the convention to follow when adding a new page.
- No bundler, no modules system beyond plain `<script>` tags; API calls go through `fetch` with the JWT attached as an `Authorization` header.
- CORS is restricted server-side to a fixed set of origins (`SecurityConfig.corsConfigurationSource`): `127.0.0.1:5500` / `localhost:5500` (Live Server) are the ones actually used; `5173`/`3000` entries are unused framework leftovers. If you serve the frontend from a different port, add it there.
- Note: `frontend-html/js/login (js_version).js` is a stray/duplicate file (space and parenthesis in the filename) distinct from `login.js` — verify which one a given HTML page actually references before editing.

## API Endpoint Map

| Path prefix | Controller | Access |
|---|---|---|
| `/api/auth/**` | `AuthController` | public |
| `/api/public/**` | `PublicProductController` | public |
| `/api/profile/**` | `UserProfileController` | authenticated |
| `/api/wallet/**` | `WalletController` | authenticated |
| `/api/cart/**` | `CartController` | BUYER |
| `/api/orders/**` | `OrderController` | BUYER |
| `/api/ratings/**` | `RatingController` | BUYER (write), public (read via `/api/public`) |
| `/api/products/**` | `ProductController` | SELLER |
| `/api/seller/account` | `BankAccountController` | SELLER |
| `/api/seller/orders/**` | `SellerOrderController` | SELLER |
| `/api/seller/ratings/**` | `SellerRatingController` | SELLER |
| `/api/admin/**` | `AdminReportController`, `AdminMemberController`, `AdminUserController` | ADMIN |
| `/createOrder` | `ECPayController` | authenticated |
| `/notify` | `NotifyController` | public (ECPay webhook) |

### 專案知識庫與參考準則
當討論到以下主題時，請強制參考對應的文件，以確保符合規範：

* **資料安全 (Data Security)**：執行任何涉及 API 或用戶敏感資料的操作時，必須參考 `/home/oem/e-commerce-platform/docs/後台系統資料保護策略.md` and `/home/oem/e-commerce-platform/docs/資料保護策略總表.md`
* **欄位加密 / 模糊搜尋 (Field Encryption / Fuzzy Search)**：新增/修改任何涉及 `demo.bigwork.crypto`、加密欄位、或 `user_search_tokens` 的邏輯時，必須參考 `/home/oem/e-commerce-platform/docs/欄位加密與模糊搜尋.md`
* **Consul / 本地環境設定 (Local Infra & Secrets)**：新增機敏設定、修改 `docker-compose.yml`、或調整 `docker/consul-seed.sh` 時，必須參考 `/home/oem/e-commerce-platform/docs/本地開發環境.md` and `/home/oem/e-commerce-platform/docs/設定與機敏資訊.md`
<!-- * **支付流程**：修改支付代碼時，必須參考 `docs/business/payment-flow.md`。
* **API 規格**：調用後端服務時，務必參考 `docs/api/spring-cloud-spec.yaml`。 -->