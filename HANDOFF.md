# HANDOFF

> 交接/進度紀錄檔。目的是讓下一個接手的人（或未來的自己）不用重新爬一次對話記錄就能知道目前狀態。每次有重大進展或交接前，更新這份檔案。

**分支**：`feature/consul-config-and-field-encryption`（目前跟 `main` commit 完全相同——**所有工作都還沒 commit**，全部是working tree 裡的未提交變更，見下方「尚未 commit」）
**最後更新**：2026-08-06

---

## 目前狀態總覽

這個分支在做兩件主要的事：**(1)** 把設定/機敏資訊從 `application.properties` 遷移到 Consul KV（`application.yml` + `spring-cloud-starter-consul-config`），**(2)** 幫使用者敏感欄位（電話、地址、管理員編號、銀行帳號）加上 AES-256-GCM 欄位級加密 + 可模糊搜尋的盲索引。這兩塊的程式碼看起來已經**大致完成**（有測試、有 SQL migration），但**都還沒 commit**。

本次 session 額外做的事：串接 Sentry 錯誤監控、把 `CLAUDE.md` 拆分精簡。細節見下方。

---

## 已完成

### 1. Consul 設定中心遷移
- `bigwork/src/main/resources/application.properties` 已刪除，改成新的 `application.yml`（目前是 untracked 新檔案）
- `docker-compose.yml`（根目錄）+ `docker/` 資料夾（entrypoint script、seed script等）都是新增、還沒進版控
- `bigwork/pom.xml` 加了 `spring-cloud-starter-consul-config` + `spring-cloud-dependencies` BOM（`2025.0.3`）
- 完整設計/操作細節已寫進 `docs/本地開發環境.md`、`docs/設定與機敏資訊.md`（**這個 `docs/` 資料夾整個被 `.gitignore` 排除，不會進版控**——如果之後要讓其他協作者看到這些文件，需要重新考慮這個 `.gitignore` 規則）

### 2. 欄位級加密 + 模糊搜尋
- 新增 `demo.bigwork.crypto` package：`EncryptionKeyProvider`、`AesGcmCipher`、`RandomIvStringConverter`、`DeterministicStringConverter`、`AbstractEncryptedStringConverter`、`NGramTokenizer`、`SearchTokenHasher`（皆有對應單元測試，`bigwork/src/test/java/demo/bigwork/crypto/`）
- `UserPO.phone`/`defaultAddress`/`adminCode`、`BankAccountPO.accountNumber` 已接上 `@Convert`
- 新增 `user_search_tokens` 盲索引表（`SQL/bigwork_user_search_tokens.sql`、`UserSearchTokenPO`、`UserSearchTokenDAO`、`UserSearchIndexService`/`Impl`）支援模糊搜尋
- 新增 `MaskingUtil`，`AdminUserSummaryVO` 建構子呼叫它遮罩後台列表的 phone/address
- `AdminMemberServiceImpl`/`AdminUserManageServiceImpl` 的關鍵字搜尋邏輯已改用 token-hash 查詢
- 完整設計原理寫在 `docs/欄位加密與模糊搜尋.md`

### 3. Sentry 錯誤監控（本次 session 新增）
- `bigwork/pom.xml`：`sentry-spring-boot-starter-jakarta` + `sentry-logback` 8.36.0（自動錯誤上報），`sentry-maven-plugin` 0.11.0 放在獨立的 Maven profile `sentry-source-upload`（只有設定 `SENTRY_AUTH_TOKEN` 環境變數時才會啟動，避免沒 token 時弄壞平常的 `./mvnw compile/test/package`）
- `application.yml`：新增 `sentry.dsn`（可用 `SENTRY_DSN` 覆寫，預設值已內建），**刻意不開 `send-default-pii`**（會把 IP/Authorization header 送到 Sentry，跟 `docs/後台系統資料保護策略.md` 的 Log 遮罩原則衝突）
- 已用使用者實際申請的 token 驗證過 `./mvnw package` 會成功把 source bundle 上傳到 Sentry（org=`dada-r4`, project=`java-spring-boot`），但**目前 Sentry 專案裡還沒有任何 issue**——因為只上傳了原始碼上下文（source context），應用程式還沒真的丟出並回報過任何例外
- 加了一個 Claude Code MCP server 連線（`sentry`，HTTP，已認證），可以直接用 `mcp__sentry__*` 工具查 issue，不用開網頁

### 4. CLAUDE.md 精簡重構（本次 session）
- 把「Local Infrastructure」「Configuration & Secrets」「Field-level encryption & masking」三段最厚重的細節搬到 `docs/本地開發環境.md`、`docs/設定與機敏資訊.md`、`docs/欄位加密與模糊搜尋.md`，CLAUDE.md 只留摘要 + 指標連結
- 新增「Clean Code」原則小節（命名意圖、單一職責、guard clause、註解只寫 why、禁 magic number、誠實處理錯誤、DRY 但不過度抽象），跟既有 TDD/SOLID 並列
- 底部「專案知識庫與參考準則」新增兩條強制參考規則，對應新拆出去的文件

---

## 尚未完成 / 中斷的工作

### A. 全部變更都還沒 commit
`git status` 顯示大量 modified + untracked 檔案（Consul 遷移、欄位加密、Sentry、CLAUDE.md 都算在內）。這個分支目前跟 `main` 是同一個 commit——換句話說，**這整個分支的工作內容目前只存在於 working tree**，還沒有任何 commit 歷史。下一步要做的話，建議先確認哪些檔案該一起 commit、要不要拆成多個邏輯 commit（例如 Consul 遷移一個、欄位加密一個、Sentry 一個、CLAUDE.md 重構一個）。

### B. 多裝置登入核准功能——已規劃但完全沒動工
使用者原本想設計一個「登入需要另一台已登入裝置核准」的功能（類似 WhatsApp Web）。完整計畫已經寫在 `/home/oem/.claude/plans/quizzical-strolling-key.md`（新增 `active_device_sessions`/`pending_login_approvals` 兩張表、`LoginApprovalService`、`JwtUtil` 加 `sid` claim、前端輪詢機制等），使用者也確認過兩個關鍵決策（管理員豁免、新增真正的登出 API）。**但這個計畫從未執行、也沒有正式核准離開 Plan Mode**——對話中途被使用者切換到別的主題，這個功能目前完全沒有程式碼。如果要接續，直接讀那份計畫檔案即可。

### C. 跨時區紀錄功能 + 每日銷售清單——只做完調查，設計尚未定案
使用者想要「DB 存 UTC、前端顯示使用者當地時區」+「每日購物總清單，記錄每天銷售」。已經用兩個 Explore agent 調查完現狀，發現：
- **一個目前就存在的真實 bug**：`docker/consul-seed.sh` 預設的 JDBC URL 帶 `serverTimezone=Asia/Taipei`，但 MySQL container 本身沒設時區、預設是 UTC——這代表 TIMESTAMP 欄位讀寫現在可能就已經有 8 小時的時差問題，這件事獨立於這個新功能本身，值得優先確認/修正。
- 目前所有 PO/VO 的時間欄位都是 `java.sql.Timestamp`，沒有任何時區感知設定（JDBC/Hibernate/Jackson 都沒配置），前端目前用 `toLocaleString('zh-TW')` 各頁面各自處理，`admin-dashboard.js` 甚至直接把 `createdAt` 原始字串印出來，完全沒解析。
- 「每日購物總清單」可以幾乎零成本沿用既有的 `AdminReportServiceImpl.generateReport(startDate, endDate)`（傳同一天當 start/end 即可）；但如果要做「多天清單」（一天一列），目前系統完全沒有 `GROUP BY DATE(created_at)` 這種按天分組的查詢，需要新寫。

有 4 個關鍵決策問題問過使用者兩次、都被中斷沒回答到（要不要順便修時區 bug、Timestamp 型別要不要遷移到 Instant/OffsetDateTime、清單要單日還是多天、「一天」的日界線要用 UTC 還是 Asia/Taipei）。**這個功能完全沒有動任何程式碼**，下一步應該先把這 4 個問題問清楚再開始設計/實作。

---

## 已知環境問題（不是程式碼壞了，是這台機器的環境設定）

- **本機預設 JDK 版本不是 21**：`JAVA_HOME` 常指向 Java 17（甚至 PATH 上的 `java` 是 11），但專案要求 Java 21（`pom.xml` `<java.version>21</java.version>`）。`target/` 裡若殘留舊版編譯出的 `.class`，會出現 `class file version 65.0` 相關的 surefire fork 錯誤。跑測試/build 前先確認：
  ```bash
  export JAVA_HOME=<你機器上的 JDK 21 路徑>
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- **Consul/MySQL 沒啟動時**，`./mvnw test`/`spring-boot:run` 一定會在 `BigworkApplicationTests.contextLoads`（或應用程式啟動）卡住失敗，這是刻意的 `fail-fast: true` 設計，不是 bug。要跑完整測試/啟動服務前，先 `docker compose up -d consul mysql` + 跑過 `docker/consul-seed.sh`。
- 扣掉 `contextLoads`，其餘 54 個單元測試（crypto、`UserSearchIndexServiceImplTest`、`AdminUserManageServiceImplTest` 等）目前全數通過。

---

## 建議下一步（依優先順序）

1. 決定要不要、要怎麼拆 commit，把目前 working tree 的四塊工作（Consul 遷移、欄位加密、Sentry、CLAUDE.md）存進 git 歷史。
2. 確認 `docs/` 被整個 `.gitignore` 排除是否真的是預期行為（尤其現在 CLAUDE.md 強制參考好幾份放在 `docs/` 裡的文件，但這些文件不會隨 repo 一起分享給其他協作者）。
3. 先解決「JDBC serverTimezone=Asia/Taipei vs MySQL 預設 UTC」這個既有時差 bug，再談新的跨時區顯示功能設計。
4. 視使用者意願決定要不要撿回「多裝置登入核准」或「跨時區 + 每日銷售清單」其中一個繼續做。
