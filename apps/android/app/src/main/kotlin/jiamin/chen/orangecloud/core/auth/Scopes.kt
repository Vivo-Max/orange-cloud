package jiamin.chen.orangecloud.core.auth

/**
 * OAuth scope ID（来自 GET /client/v4/oauth/scopes 的 id 字段，与 iOS PermissionModels 一致）。
 * 注意：CF dash OAuth（Hydra 系）只在请求 offline_access 时才签发 refresh token（issue #44 踩坑实证），
 * 该 scope 由 AuthRepository.buildAuthorizationUri 在咽喉点统一追加，此处无需（也勿）列入目录。
 */
object Scopes {
    const val ACCOUNT_READ = "account-settings.read"
    const val ZONE_READ = "zone.read"
    const val ZONE_WRITE = "zone.write"
    const val DNS_READ = "dns.read"
    const val DNS_WRITE = "dns.write"
    const val WORKERS_READ = "workers-scripts.read"
    const val WORKERS_WRITE = "workers-scripts.write"
    const val WORKERS_ROUTES_READ = "workers-routes.read"
    const val WORKERS_ROUTES_WRITE = "workers-routes.write"
    const val WORKERS_TAIL_READ = "workers-tail.read"
    const val SNIPPETS_READ = "snippets.read"
    const val SNIPPETS_WRITE = "snippets.write"
    const val R2_READ = "workers-r2.read"
    const val R2_WRITE = "workers-r2.write"
    const val D1_READ = "d1.read"
    const val D1_WRITE = "d1.write"
    const val KV_READ = "workers-kv-storage.read"
    const val KV_WRITE = "workers-kv-storage.write"
    const val TUNNEL_READ = "argotunnel.read"
    const val TUNNEL_WRITE = "argotunnel.write"
    const val WAF_READ = "zone-waf.read"
    const val WAF_WRITE = "zone-waf.write"

    /** 机器人管控（GET/PUT /zones/{id}/bot_management）。全套餐可用，含免费版。 */
    const val BOT_MANAGEMENT_READ = "bot-management.read"
    const val BOT_MANAGEMENT_WRITE = "bot-management.write"

    /** 独立健康检查（/zones/{id}/healthchecks）。免费版不可用。 */
    const val HEALTHCHECK_READ = "healthcheck.read"
    const val HEALTHCHECK_WRITE = "healthcheck.write"

    /** Zone DNS 设置（/zones/{id}/dns_settings）。DNSSEC 走 dns.read/.write。 */
    const val ZONE_DNS_SETTINGS_READ = "zone-dns-settings.read"
    const val ZONE_DNS_SETTINGS_WRITE = "zone-dns-settings.write"

    /** Cloudflare Registrar。用新版 /registrar/registrations，旧 /registrar/domains 2026-09-27 停用。 */
    const val REGISTRAR_READ = "registrar-domains.read"
    const val REGISTRAR_ADMIN = "registrar-domains.admin"

    /** Cloudflare Trace。全套餐可用，但要求管理员角色。 */
    const val REQUEST_TRACER_READ = "request-tracer.read"

    /** R2 Data Catalog（Iceberg）。2026-08-03 起按目录操作计费。 */
    const val R2_CATALOG_READ = "r2-catalog.read"
    const val R2_CATALOG_WRITE = "r2-catalog.write"

    /** R2 SQL（api.sql.cloudflarestorage.com 独立主机；OAuth 接受度未实测）。2026-08-03 起按扫描量计费。 */
    const val R2_CATALOG_SQL_READ = "r2-catalog-sql.read"

    /** Turnstile 人机验证组件（/accounts/{id}/challenges/widgets）。 */
    const val CHALLENGE_WIDGETS_READ = "challenge-widgets.read"
    const val CHALLENGE_WIDGETS_WRITE = "challenge-widgets.write"

    /** Workers Builds（CI 构建记录与日志）。 */
    const val WORKERS_CI_READ = "workers-ci.read"
    const val WORKERS_CI_WRITE = "workers-ci.write"

    /** 托管请求/响应头（Managed Transforms）。 */
    const val MANAGED_HEADERS_READ = "managed-headers.read"
    const val MANAGED_HEADERS_WRITE = "managed-headers.write"

    /** URL Scanner v2（提交链接做安全扫描）。 */
    const val URL_SCANNER_READ = "url-scanner.read"
    const val URL_SCANNER_WRITE = "url-scanner.write"
    const val ZONE_SETTINGS_READ = "zone-settings.read"
    const val ZONE_SETTINGS_WRITE = "zone-settings.write"
    const val CACHE_PURGE = "cache.purge"
    // 域名安全（1.3 对齐 iOS 1.4.0）。SSL/TLS 加密模式与性能开关走 zone-settings；
    // 证书、Transform、IP 访问规则各有独立 scope。均经 [[cf-oauth-scopes]] 核对。
    const val SSL_CERTS_READ = "ssl-and-certificates.read"
    const val SSL_CERTS_WRITE = "ssl-and-certificates.write"
    const val TRANSFORM_READ = "zone-transform-rules.read"
    const val TRANSFORM_WRITE = "zone-transform-rules.write"
    const val FIREWALL_READ = "firewall-services.read"
    const val FIREWALL_WRITE = "firewall-services.write"
    const val ACCOUNT_ANALYTICS_READ = "account-analytics.read"
    const val ANALYTICS_READ = "analytics.read"
    // —— 1.4「G–J 爆发」新增 scope（共用 OAuth client 上 iOS 早已注册，均经 [[cf-oauth-scopes]] 核对）——
    const val CACHE_RULES_READ = "cache-settings.read"          // Cache Rules（Rulesets cache phase）
    const val CACHE_RULES_WRITE = "cache-settings.write"
    const val EMAIL_ADDR_READ = "email-routing-address.read"    // Email Routing 目标地址
    const val EMAIL_ADDR_WRITE = "email-routing-address.write"
    const val EMAIL_RULE_READ = "email-routing-rule.read"       // Email Routing 路由规则 + 设置
    const val EMAIL_RULE_WRITE = "email-routing-rule.write"
    const val EMAIL_SUPPRESSION_READ = "email-routing-suppression.read"   // 抑制列表
    const val EMAIL_SUPPRESSION_WRITE = "email-routing-suppression.write"
    const val REDIRECTS_READ = "mass-url-redirects.read"        // Bulk Redirects 列表
    const val REDIRECTS_WRITE = "mass-url-redirects.write"
    const val RULE_LISTS_READ = "account-rule-lists.read"       // Bulk Redirects 条目（rule lists）
    const val RULE_LISTS_WRITE = "account-rule-lists.write"
    const val LB_READ = "load-balancers.read"                   // Load Balancer
    const val LB_WRITE = "load-balancers.write"
    const val LB_POOLS_READ = "load-balancing-monitors-and-pools.read"
    const val LB_POOLS_WRITE = "load-balancing-monitors-and-pools.write"
    const val ACCESS_READ = "access.read"                       // Zero Trust Access 应用
    const val ACCESS_WRITE = "access.write"
    const val TEAMS_READ = "teams.read"                         // Zero Trust Gateway 规则
    const val TEAMS_WRITE = "teams.write"
    const val PAGES_READ = "page.read"                          // Cloudflare Pages
    const val PAGES_WRITE = "page.write"
    const val AI_READ = "ai.read"                               // Workers AI
    const val AI_WRITE = "ai.write"
    const val AIG_READ = "aig.read"                             // AI Gateway
    const val AIG_WRITE = "aig.write"
    const val QUEUES_READ = "queues.read"                       // Queues
    const val QUEUES_WRITE = "queues.write"
    const val HYPERDRIVE_READ = "query-cache.read"              // Hyperdrive（query-cache scope）
    const val HYPERDRIVE_WRITE = "query-cache.write"
    const val WORKERS_OBSERVABILITY_READ = "workers-observability.read" // Worker 日志/指标（并入 Workers 功能）
    // 通知 / 告警（CF Alerting，把告警推到推送端点；iOS 早已在共用 client 注册，经 [[cf-oauth-scopes]] 核对）
    const val NOTIFICATIONS_READ = "notifications.read"
    const val NOTIFICATIONS_WRITE = "notifications.write"

    // —— 扩展 scope（按自建 OAuth Client 实际注册集合逐项探测核对，2026-08-28）——
    const val ACCOUNT_WRITE = "account-settings.write"
    const val USER_DETAILS_READ = "user-details.read"           // 用户资料
    const val MEMBERSHIPS_READ = "memberships.read"             // 账户成员关系
    const val MEMBERSHIPS_WRITE = "memberships.write"
    const val LOGS_READ = "logs.read"                           // Logpush（域名级）
    const val LOGS_WRITE = "logs.write"
    const val ACCOUNT_LOGS_READ = "account-logs.read"           // Logpush（账户级）
    const val ACCOUNT_LOGS_WRITE = "account-logs.write"
    const val WAITING_ROOMS_READ = "waiting-rooms.read"         // Waiting Room 等候室
    const val WAITING_ROOMS_WRITE = "waiting-rooms.write"
    const val DNS_FIREWALL_READ = "dns-firewall.read"           // DNS Firewall
    const val DNS_FIREWALL_WRITE = "dns-firewall.write"
    const val PAGE_SHIELD_READ = "page-shield.read"             // Page Shield 脚本监测
    const val CUSTOM_PAGES_READ = "custom-pages.read"           // 自定义错误/质询页
    const val CUSTOM_PAGES_WRITE = "custom-pages.write"
    const val ZONE_VERSIONING_READ = "zone-versioning.read"     // 域名配置版本管理
    const val ZONE_VERSIONING_WRITE = "zone-versioning.write"
    const val ACCOUNT_RULESETS_READ = "account-rulesets.read"   // 账户级 Rulesets
    const val ACCOUNT_RULESETS_WRITE = "account-rulesets.write"
    const val VECTORIZE_READ = "vectorize.read"                 // Vectorize 向量库
    const val VECTORIZE_WRITE = "vectorize.write"
    const val BROWSER_RENDERING_READ = "browser-rendering.read" // Browser Rendering
    const val BROWSER_RENDERING_WRITE = "browser-rendering.write"
    const val PIPELINES_READ = "pipelines.read"                 // Pipelines 数据管道
    const val PIPELINES_WRITE = "pipelines.write"
    const val CONTAINERS_READ = "containers.read"               // Workers Containers
    const val CONTAINERS_WRITE = "containers.write"
    const val AI_SEARCH_READ = "ai-search.read"                 // AI Search（原 AutoRAG）
    const val AI_SEARCH_WRITE = "ai-search.write"
    const val SECRETS_STORE_READ = "secrets-store.read"         // 账户级 Secrets Store
    const val SECRETS_STORE_WRITE = "secrets-store.write"
    const val ZARAZ_READ = "zaraz.read"                         // Zaraz 第三方工具加载
    const val ZARAZ_WRITE = "zaraz.write"
    const val MAGIC_TRANSIT_READ = "magic-transit.read"         // Magic Transit
    const val MAGIC_TRANSIT_WRITE = "magic-transit.write"
    const val MAGIC_WAN_READ = "magic-wan.read"                 // Magic WAN
    const val MAGIC_WAN_WRITE = "magic-wan.write"
    const val MAGIC_FIREWALL_READ = "magic-firewall.read"       // Magic Firewall
    const val MAGIC_FIREWALL_WRITE = "magic-firewall.write"
    const val EMAIL_SENDING_READ = "email-sending.read"         // Email Sending
    const val EMAIL_SENDING_WRITE = "email-sending.write"
    const val INTEL_READ = "intel.read"                         // 威胁情报（Cloudforce One）
    const val CASB_READ = "casb.read"                           // CASB SaaS 安全扫描
    const val CASB_WRITE = "casb.write"

    // —— 规则中心（对齐 iOS zone_rules：五个 Rulesets phase + Page Rules + 自定义错误页）——
    const val DYNAMIC_REDIRECT_READ = "dynamic-redirect.read"   // 单条重定向（Single Redirects）
    const val DYNAMIC_REDIRECT_WRITE = "dynamic-redirect.write"
    const val ORIGIN_READ = "origin.read"                       // Origin Rules 源站规则
    const val ORIGIN_WRITE = "origin.write"
    const val CONFIG_SETTINGS_READ = "config-settings.read"     // Configuration Rules（含 URL 正规化）
    const val CONFIG_SETTINGS_WRITE = "config-settings.write"
    const val RESPONSE_COMPRESSION_READ = "response-compression.read"   // 压缩规则
    const val RESPONSE_COMPRESSION_WRITE = "response-compression.write"
    const val CUSTOM_ERRORS_READ = "custom-errors.read"         // 自定义错误页（规则）
    const val CUSTOM_ERRORS_WRITE = "custom-errors.write"
    const val PAGE_RULES_READ = "page-rules.read"               // 经典 Page Rules
    const val PAGE_RULES_WRITE = "page-rules.write"

    /**
     * 默认申请的权限集，覆盖全部已对表 iOS 的功能（账号/域名/DNS/Workers/tail/Snippets/
     * 存储/Tunnel/WAF/Zone 设置/分析）。对应 iOS PermissionModels.allFeatures 的全选默认。
     * 完整权限选择 UI（让用户按功能裁剪）见后续切片；裁剪前默认全量请求。
     */
    val defaultP0: List<String> = listOf(
        ACCOUNT_READ,
        ZONE_READ, ZONE_WRITE,
        DNS_READ, DNS_WRITE,
        WORKERS_READ, WORKERS_WRITE,
        WORKERS_ROUTES_READ, WORKERS_ROUTES_WRITE,
        WORKERS_TAIL_READ,
        SNIPPETS_READ, SNIPPETS_WRITE,
        R2_READ, R2_WRITE,
        D1_READ, D1_WRITE,
        KV_READ, KV_WRITE,
        TUNNEL_READ, TUNNEL_WRITE,
        WAF_READ, WAF_WRITE,
        BOT_MANAGEMENT_READ, BOT_MANAGEMENT_WRITE,
        HEALTHCHECK_READ, HEALTHCHECK_WRITE,
        ZONE_DNS_SETTINGS_READ, ZONE_DNS_SETTINGS_WRITE,
        REGISTRAR_READ, REGISTRAR_ADMIN,
        REQUEST_TRACER_READ,
        R2_CATALOG_READ, R2_CATALOG_WRITE,
        WORKERS_CI_READ, WORKERS_CI_WRITE,
        MANAGED_HEADERS_READ, MANAGED_HEADERS_WRITE,
        URL_SCANNER_READ, URL_SCANNER_WRITE,
        ZONE_SETTINGS_READ, ZONE_SETTINGS_WRITE, CACHE_PURGE,
        SSL_CERTS_READ, SSL_CERTS_WRITE,
        TRANSFORM_READ, TRANSFORM_WRITE,
        FIREWALL_READ, FIREWALL_WRITE,
        ACCOUNT_ANALYTICS_READ, ANALYTICS_READ,
        // 1.4 新增
        CACHE_RULES_READ, CACHE_RULES_WRITE,
        EMAIL_ADDR_READ, EMAIL_ADDR_WRITE, EMAIL_RULE_READ, EMAIL_RULE_WRITE,
        EMAIL_SUPPRESSION_READ, EMAIL_SUPPRESSION_WRITE,
        REDIRECTS_READ, REDIRECTS_WRITE, RULE_LISTS_READ, RULE_LISTS_WRITE,
        LB_READ, LB_WRITE, LB_POOLS_READ, LB_POOLS_WRITE,
        ACCESS_READ, ACCESS_WRITE, TEAMS_READ, TEAMS_WRITE,
        PAGES_READ, PAGES_WRITE,
        AI_READ, AI_WRITE, AIG_READ, AIG_WRITE,
        QUEUES_READ, QUEUES_WRITE,
        HYPERDRIVE_READ, HYPERDRIVE_WRITE,
        WORKERS_OBSERVABILITY_READ,
        NOTIFICATIONS_READ, NOTIFICATIONS_WRITE,
        // 扩展集（与自建 Client 注册的 130 项对齐）
        ACCOUNT_WRITE,
        USER_DETAILS_READ, MEMBERSHIPS_READ, MEMBERSHIPS_WRITE,
        LOGS_READ, LOGS_WRITE, ACCOUNT_LOGS_READ, ACCOUNT_LOGS_WRITE,
        WAITING_ROOMS_READ, WAITING_ROOMS_WRITE,
        DNS_FIREWALL_READ, DNS_FIREWALL_WRITE,
        PAGE_SHIELD_READ,
        CUSTOM_PAGES_READ, CUSTOM_PAGES_WRITE,
        ZONE_VERSIONING_READ, ZONE_VERSIONING_WRITE,
        ACCOUNT_RULESETS_READ, ACCOUNT_RULESETS_WRITE,
        VECTORIZE_READ, VECTORIZE_WRITE,
        BROWSER_RENDERING_READ, BROWSER_RENDERING_WRITE,
        PIPELINES_READ, PIPELINES_WRITE,
        CONTAINERS_READ, CONTAINERS_WRITE,
        AI_SEARCH_READ, AI_SEARCH_WRITE,
        SECRETS_STORE_READ, SECRETS_STORE_WRITE,
        ZARAZ_READ, ZARAZ_WRITE,
        MAGIC_TRANSIT_READ, MAGIC_TRANSIT_WRITE,
        MAGIC_WAN_READ, MAGIC_WAN_WRITE,
        MAGIC_FIREWALL_READ, MAGIC_FIREWALL_WRITE,
        EMAIL_SENDING_READ, EMAIL_SENDING_WRITE,
        INTEL_READ,
        CASB_READ, CASB_WRITE,
    )

    /** 空格分隔、排序去重的 scope 字符串，直接用于 OAuth scope 参数。 */
    fun scopeString(scopes: List<String> = defaultP0): String =
        scopes.toSortedSet().joinToString(" ")
}
