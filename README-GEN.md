# Spring Boot 全端電商平台 (E-Shop Demo)

## 專案描述

這是一個功能完整的全端電商平台專案，模擬一個多賣家 (Multi-Seller) 的市集（如蝦皮或 Amazon）。專案分為兩個完全解耦、各自獨立執行的部分：

* **後端 (`bigwork/`)** — Java 21 / Spring Boot 3.5.7，提供 RESTful API、JWT 無狀態身份驗證、JPA/Hibernate 操作 MySQL，並具備欄位級加密與多角色 (RBAC) 權限控制。
* **前端 (`frontend-html/`)** — 純 Vanilla JavaScript (ES6+ async/await) + HTML5/CSS3，不依賴任何框架、無需建置流程，純粹透過 `fetch` 與後端以 HTTP/CORS 溝通。

兩者之間沒有共用的建置系統，各自以獨立程序運行。

### 核心架構亮點

* **多賣家拆單系統** — 結帳時後端依 `seller_id` 自動將購物車拆分為多筆子訂單，分別扣庫存、入賣家錢包，全程在單一交易 (`@Transactional`) 內完成。
* **欄位級加密 (AES-256-GCM)** — `UserPO.phone`、`defaultAddress`、`adminCode`、`BankAccountPO.accountNumber` 透過 JPA `AttributeConverter` 於持久層透明加解密，敏感欄位在資料庫中永遠是密文。
* **可模糊搜尋的盲索引 (Blind Index)** — 加密欄位無法用 `LIKE` 查詢，因此以 HMAC-SHA256 trigram 雜湊建立 `user_search_tokens` 搜尋索引，在不解密全表的前提下支援模糊搜尋。
* **RBAC 角色權限控制** — Spring Security + JWT，明確劃分 `ADMIN`、`BUYER`、`SELLER` 與公開端點的存取權限。
* **雙軌金流系統** — 內部電子錢包（儲值/提款/交易紀錄）與第三方金流「綠界科技 (ECPay)」（SHA-256 驗簽的非同步 Webhook 回調）並存。
* **遞迴分類查詢** — 商品分類為自參照樹狀結構，查詢父分類會遞迴帶出所有子孫分類商品。
* **集中式設定管理** — 所有機敏設定（DB 帳密、JWT 密鑰、郵件帳密、ECPay 金鑰、資料加密金鑰）皆透過 Consul KV 集中管理，`application.yml` 中不出現任何明碼憑證。

---

## 功能說明

平台依角色區分為「公開訪客」、「買家 (BUYER)」、「賣家 (SELLER)」與「管理員 (ADMIN)」四種視角。

### 公開功能
* 買家 / 賣家分離式登入與註冊
* JWT 無狀態身份驗證
* Email 驗證碼註冊、忘記密碼 / 重設密碼流程
* 商品瀏覽、分類篩選、商品評價公開查詢

### 買家中心 (BUYER)
* 商品瀏覽與檢索、商品詳情與真實顧客評價
* 購物車管理，即時計算金額並與錢包餘額比對
* 電子錢包查詢與模擬儲值
* 個人資料與預設收貨地址編輯
* 結帳（自動依賣家拆單）：內部錢包扣款 或 綠界金流付款
* 歷史訂單查詢、對已完成訂單項目撰寫評價

### 賣家中心 (SELLER)
* 商品上架 / 修改 / 下架管理
* 收款銀行帳戶設定（含格式驗證）
* 銷售餘額檢視與提款申請
* 收到的訂單與商品評價查詢

### 後台管理 (ADMIN)
* 專屬管理員編號 (Admin Code) 登入
* 營運儀表板：訂單金額分佈、新會員成長、熱銷品類 Top 5、Excel 報表匯出
* 會員管理：查看與管理買家 / 賣家帳號、訂單與商品，支援依關鍵字模糊搜尋（含加密欄位）

### API 端點總覽

| 路徑前綴 | Controller | 存取權限 |
| :--- | :--- | :--- |
| `/api/auth/**` | `AuthController` | 公開 |
| `/api/public/**` | `PublicProductController` | 公開 |
| `/api/profile/**` | `UserProfileController` | 已登入 |
| `/api/wallet/**` | `WalletController` | 已登入 |
| `/api/cart/**` | `CartController` | BUYER |
| `/api/orders/**` | `OrderController` | BUYER |
| `/api/ratings/**` | `RatingController` | BUYER（寫入）/ 公開（讀取） |
| `/api/products/**` | `ProductController` | SELLER |
| `/api/seller/account` | `BankAccountController` | SELLER |
| `/api/seller/orders/**` | `SellerOrderController` | SELLER |
| `/api/seller/ratings/**` | `SellerRatingController` | SELLER |
| `/api/admin/**` | `AdminReportController` / `AdminMemberController` / `AdminUserController` | ADMIN |
| `/createOrder` | `ECPayController` | 已登入 |
| `/notify` | `NotifyController` | 公開（ECPay Webhook） |

---

## 🛠️ 技術棧

| 類別 | 技術 |
| :--- | :--- |
| **核心框架** | Java 21, Spring Boot 3.5.7 |
| **設定中心** | Spring Cloud Consul Config（`spring-cloud-starter-consul-config`） |
| **安全與認證** | Spring Security, JWT (`jjwt` 0.12.5) |
| **資料庫與 ORM** | MySQL 8.0, Spring Data JPA (Hibernate)，`ddl-auto=validate`（schema 由 `SQL/` 手動管理） |
| **加密** | AES-256-GCM 欄位加密（JPA `AttributeConverter`）+ HMAC-SHA256 盲索引搜尋 |
| **工具與報表** | Apache POI 5.2.5（Excel 匯出）, Lombok, Spring Mail |
| **前端** | Vanilla JavaScript (ES6+ Async/Await), Fetch API, HTML5, CSS3 |
| **本地基礎設施** | Docker Compose（Consul + MySQL，見 `docker-compose.yml`） |

---

## 安裝步驟

### 必備環境
* Java JDK 21
* Apache Maven（或使用內附的 `./mvnw`）
* Docker / Docker Compose
* MySQL 用戶端工具（可選，方便檢查資料）
* VS Code（前端開發用，建議安裝 `Live Server` 擴充套件）
* Ngrok（本地開發要接收 ECPay 綠界金流回調時需要）

### 1. 啟動本地基礎設施 (Consul + MySQL)

專案的所有機敏設定都透過 Consul KV 管理，**不使用 `.env` 檔案**。首次啟動一個全新的資料庫 volume 時，依下列順序執行：

```bash
# 於 repo 根目錄
docker compose up -d consul

# 設定下列環境變數後執行種子腳本，將憑證寫入 Consul KV
# 必要變數：MYSQL_ROOT_PASSWORD, DB_USERNAME, DB_PASSWORD, MAIL_USERNAME,
#           MAIL_PASSWORD, JWT_SECRET, ECPAY_MERCHANT_ID, ECPAY_HASH_KEY,
#           ECPAY_HASH_IV, DATA_ENCRYPTION_KEY
docker/consul-seed.sh

docker compose up -d mysql
```

> 若資料庫 volume（`bigwork-mysql-data`）已經初始化過，之後可直接 `docker compose up -d consul mysql` 一起啟動即可，因為 MySQL 只有在首次初始化時才需要 Consul 提供的憑證。

### 2. 匯入資料庫 Schema

於 MySQL 中建立 `bigwork` schema，並依序匯入 `SQL/` 資料夾內的腳本（`docker/init-db.sh` 中已定義正確順序，`bigwork_user_search_tokens.sql` 需最後匯入，因為它外鍵參照 `users`）。

### 3. 啟動後端 (Spring Boot)

```bash
cd bigwork
./mvnw spring-boot:run
```

後端會透過 `spring.config.import: consul:localhost:8500` 從 Consul KV 拉取設定；若 Consul 無法連線，應用程式會直接拒絕啟動（`fail-fast: true`，設計如此，避免在缺少憑證的情況下靜默啟動）。伺服器預設運行於 `http://localhost:8080`。

### 4. 啟動前端 (HTML/JS)

以 VS Code 開啟 `frontend-html/` 資料夾，在 `html/index.html` 上按右鍵選擇 `Open with Live Server`（預設 port 5500，需與 `SecurityConfig` 中允許的 CORS 來源一致）。

### 5.（選用）ECPay 綠界金流 + Ngrok

綠界金流需要一個公開網址接收付款結果通知 (Webhook)，本地開發需用 Ngrok 建立隧道：

```bash
ngrok http 8080
```

並將 `ECPAY_RETURN_URL` 環境變數設為 `<ngrok網址>/notify`，讓 `/notify` 端點（`permitAll()`，無需 JWT）能接收綠界伺服器的回調。

---

## 快速開始指南

1. `docker compose up -d consul` → `docker/consul-seed.sh`（帶好所有必要環境變數）→ `docker compose up -d mysql`
2. 匯入 `SQL/` 目錄下的資料表結構與種子資料
3. `cd bigwork && ./mvnw spring-boot:run` 啟動後端（`http://localhost:8080`）
4. 用 Live Server 開啟 `frontend-html/html/index.html`（`http://127.0.0.1:5500`）
5. 瀏覽器開啟前端頁面，即可用種子資料中的帳號測試買家 / 賣家 / 管理員三種角色流程

**常用後端指令**（於 `bigwork/` 目錄下）：

```bash
./mvnw spring-boot:run          # 啟動後端
./mvnw compile                  # 只編譯
./mvnw test                     # 執行所有測試
./mvnw test -Dtest=ClassName    # 執行單一測試類別
./mvnw package                  # 打包成 jar
```

---

## 貢獻指南

1. **Fork / 建立分支**：從 `main` 切出功能分支，例如 `feature/xxx`。
2. **遵循分層架構**：
   * Controller 保持精簡，僅負責委派給 `service/`。
   * 業務邏輯與交易邊界 (`@Transactional`) 放在 `service/Impl/`。
   * 依賴 `service/*Service` 介面（建構子注入），不要直接依賴 `Impl` 類別。
   * 新增欄位級加密/遮罩等單一職責邏輯時，獨立成小型 collaborator（如 converter/util），不要塞進既有 service 方法或 entity getter/setter。
3. **測試先行 (TDD)**：`spring-boot-starter-test` 與 `spring-security-test` 已在 classpath 上，新功能請先寫失敗測試再實作。
4. **機敏資訊絕不進版控**：任何新增的憑證（DB、JWT、郵件、ECPay、加密金鑰）一律透過 Consul KV（`docker/consul-seed.sh`）管理，絕不寫回 `application.yml` 或提交到 git。
5. **異動 JPA Entity 時同步更新 SQL**：`ddl-auto=validate`，schema 由 `SQL/*.sql` 手動維護，異動 entity mapping 後務必同步修改對應的 SQL 腳本與實際 schema。
6. **新增端點務必註冊權限規則**：於 `SecurityConfig.filterChain` 明確指定存取角色，避免落入預設的 `anyRequest().authenticated()`。
7. 送出 Pull Request 前執行 `./mvnw test` 確認測試通過，並在 PR 描述中說明變更動機與影響範圍。

---

## 📄 授權 (License)

本專案採用 MIT 授權。
