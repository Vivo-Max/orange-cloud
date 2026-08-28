//
//  UsageFetcher.swift
//  OrangeCloudWidgets
//
//  用量卡自取数：按所选服务直查 GraphQL/REST（一次只查一个服务，控制刷新预算）。
//  账户指针与额度口径（账单日/套餐）来自 App Group 镜像；token 只读不刷新。
//  任何失败返回 nil，调用方回退 App 写入的快照。
//

import Foundation

nonisolated enum UsageFetcher {

    // MARK: - 上下文（App Group 镜像）

    private struct AccountPrefs: Codable {
        var billingCycleDay: Int = 1
        var workersPlanPaid: Bool = false
        var r2PlanPaid: Bool = false
    }

    private struct Context {
        let accountId: String
        let token: String
        let workersPaid: Bool
        let periodStart: Date
        let periodLabel: String
    }

    /// overrideAccountId/overrideSessionId 来自 Widget 配置（固定某账号）；
    /// 缺省时回退当前账号 + 当前身份 token。
    private static func loadContext(overrideAccountId: String? = nil, overrideSessionId: String? = nil) -> Context? {
        guard let defaults = UserDefaults(suiteName: WidgetSnapshot.appGroupID) else { return nil }
        guard let accountId = overrideAccountId ?? defaults.string(forKey: "currentAccountId") else { return nil }
        // 选定账号取其所属身份的 token；未指定身份则用当前身份
        let token: String?
        if let sid = overrideSessionId {
            token = SharedAuth.validAccessToken(sessionId: sid)
        } else {
            token = SharedAuth.currentValidAccessToken()
        }
        guard let token else { return nil }

        var prefs = AccountPrefs()
        if let data = defaults.data(forKey: "accountPrefsById"),
           let all = try? JSONDecoder().decode([String: AccountPrefs].self, from: data),
           let mine = all[accountId] {
            prefs = mine
        }
        let workersPaid = prefs.workersPlanPaid
        return Context(
            accountId: accountId,
            token: token,
            workersPaid: workersPaid,
            periodStart: BillingCycle.periodStart(billingDay: prefs.billingCycleDay),
            periodLabel: workersPaid ? (prefs.billingCycleDay != 1 ? String(localized: "账期") : String(localized: "本月")) : String(localized: "今日")
        )
    }

    // MARK: - 入口

    static func freshService(_ id: String, accountId: String? = nil, sessionId: String? = nil) async -> WidgetUsageService? {
        guard let context = loadContext(overrideAccountId: accountId, overrideSessionId: sessionId) else { return nil }
        switch id {
        case "workers": return await fetchWorkers(context)
        case "r2":      return await fetchR2(context)
        case "d1":      return await fetchD1(context)
        case "kv":      return await fetchKV(context)
        default:        return nil
        }
    }

    // MARK: - 各服务

    private static func fetchWorkers(_ ctx: Context) async -> WidgetUsageService? {
        let (monthStart, todayStart, now) = timeWindows(periodStart: ctx.periodStart)
        let query = """
        query ($accountTag: string!, $monthStart: Time!, $todayStart: Time!, $now: Time!) {
          viewer { accounts(filter: { accountTag: $accountTag }) {
            month: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: $monthStart, datetime_leq: $now }) { sum { requests } }
            today: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: $todayStart, datetime_leq: $now }) { sum { requests } }
          } }
        }
        """
        struct Node: Codable { let month: [Group]?; let today: [Group]? }
        struct Group: Codable { let sum: Sum? }
        struct Sum: Codable { let requests: Int? }

        guard let node: Node = await graphQLFirstAccount(
            token: ctx.token, query: query,
            variables: ["accountTag": ctx.accountId, "monthStart": monthStart, "todayStart": todayStart, "now": now]
        ) else { return nil }

        let used = ctx.workersPaid
            ? (node.month ?? []).reduce(0) { $0 + ($1.sum?.requests ?? 0) }
            : (node.today ?? []).reduce(0) { $0 + ($1.sum?.requests ?? 0) }
        let quota = ctx.workersPaid ? 10_000_000 : 100_000
        var rows = [
            WidgetUsageRow(title: String(localized: "请求 · \(ctx.periodLabel)"), used: used, quota: quota, valueText: compact(used)),
        ]
        // CPU 总量是较新 schema 字段，独立查询、失败不影响请求行
        if let cpuRow = await workersCpuRow(ctx) {
            rows.append(cpuRow)
        }
        return WidgetUsageService(id: "workers", name: "Workers", rows: rows)
    }

    /// Workers CPU 总用时：Paid 对照 30M ms/周期；Free 无周期额度，展示今日合计
    private static func workersCpuRow(_ ctx: Context) async -> WidgetUsageRow? {
        let (monthStart, todayStart, now) = timeWindows(periodStart: ctx.periodStart)
        let query = """
        query ($accountTag: string!, $monthStart: Time!, $todayStart: Time!, $now: Time!) {
          viewer { accounts(filter: { accountTag: $accountTag }) {
            month: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: $monthStart, datetime_leq: $now }) { sum { cpuTimeUs } }
            today: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: $todayStart, datetime_leq: $now }) { sum { cpuTimeUs } }
          } }
        }
        """
        struct Node: Codable { let month: [Group]?; let today: [Group]? }
        struct Group: Codable { let sum: Sum? }
        struct Sum: Codable { let cpuTimeUs: Int? }

        guard let node: Node = await graphQLFirstAccount(
            token: ctx.token, query: query,
            variables: ["accountTag": ctx.accountId, "monthStart": monthStart, "todayStart": todayStart, "now": now]
        ) else { return nil }

        if ctx.workersPaid {
            let ms = (node.month ?? []).reduce(0) { $0 + ($1.sum?.cpuTimeUs ?? 0) } / 1000
            return WidgetUsageRow(title: "CPU · \(ctx.periodLabel)", used: ms, quota: 30_000_000,
                                  valueText: compact(ms) + " ms")
        }
        let ms = (node.today ?? []).reduce(0) { $0 + ($1.sum?.cpuTimeUs ?? 0) } / 1000
        return WidgetUsageRow(title: String(localized: "CPU · 今日"), used: ms, quota: nil,
                              valueText: compact(ms) + " ms")
    }

    private static func fetchR2(_ ctx: Context) async -> WidgetUsageService? {
        let (monthStart, _, now) = timeWindows(periodStart: ctx.periodStart)
        let query = """
        query ($accountTag: string!, $monthStart: Time!, $now: Time!) {
          viewer { accounts(filter: { accountTag: $accountTag }) {
            ops: r2OperationsAdaptiveGroups(limit: 10000, filter: { datetime_geq: $monthStart, datetime_leq: $now }) {
              dimensions { actionType }
              sum { requests }
            }
          } }
        }
        """
        struct Node: Codable { let ops: [Group]? }
        struct Group: Codable { let dimensions: Dim?; let sum: Sum? }
        struct Dim: Codable { let actionType: String? }
        struct Sum: Codable { let requests: Int? }

        guard let node: Node = await graphQLFirstAccount(
            token: ctx.token, query: query,
            variables: ["accountTag": ctx.accountId, "monthStart": monthStart, "now": now]
        ) else { return nil }

        var classA = 0
        var classB = 0
        for group in node.ops ?? [] {
            guard let action = group.dimensions?.actionType else { continue }
            let count = group.sum?.requests ?? 0
            if R2OperationClass.classA.contains(action) { classA += count }
            else if R2OperationClass.classB.contains(action) { classB += count }
        }

        var rows = [
            WidgetUsageRow(title: String(localized: "A 类操作 · 本周期"), used: classA, quota: 1_000_000, valueText: compact(classA)),
            WidgetUsageRow(title: String(localized: "B 类操作 · 本周期"), used: classB, quota: 10_000_000, valueText: compact(classB)),
        ]
        // 存储走 REST（与 Dashboard 同源）
        if let metrics = await r2Metrics(ctx) {
            rows.insert(
                WidgetUsageRow(title: String(localized: "存储"), used: metrics, quota: 10_000_000_000, valueText: bytes(metrics)),
                at: 0
            )
        }
        return WidgetUsageService(id: "r2", name: "R2", rows: rows)
    }

    private static func fetchD1(_ ctx: Context) async -> WidgetUsageService? {
        let (periodDay, todayDay, untilDay) = dateWindows(periodStart: ctx.periodStart)
        let query = """
        query ($accountTag: string!, $periodStart: Date!, $todayStart: Date!, $until: Date!) {
          viewer { accounts(filter: { accountTag: $accountTag }) {
            period: d1AnalyticsAdaptiveGroups(limit: 10000, filter: { date_geq: $periodStart, date_leq: $until }) { sum { rowsRead rowsWritten } }
            today: d1AnalyticsAdaptiveGroups(limit: 10000, filter: { date_geq: $todayStart, date_leq: $until }) { sum { rowsRead rowsWritten } }
          } }
        }
        """
        struct Node: Codable { let period: [Group]?; let today: [Group]? }
        struct Group: Codable { let sum: Sum? }
        struct Sum: Codable { let rowsRead: Int?; let rowsWritten: Int? }

        guard let node: Node = await graphQLFirstAccount(
            token: ctx.token, query: query,
            variables: ["accountTag": ctx.accountId, "periodStart": periodDay, "todayStart": todayDay, "until": untilDay]
        ) else { return nil }

        func totals(_ groups: [Group]?) -> (Int, Int) {
            (groups ?? []).reduce(into: (0, 0)) { $0.0 += $1.sum?.rowsRead ?? 0; $0.1 += $1.sum?.rowsWritten ?? 0 }
        }
        let window = ctx.workersPaid ? totals(node.period) : totals(node.today)
        var rows = [
            WidgetUsageRow(title: String(localized: "行读取 · \(ctx.periodLabel)"), used: window.0,
                           quota: ctx.workersPaid ? 25_000_000_000 : 5_000_000, valueText: compact(window.0)),
            WidgetUsageRow(title: String(localized: "行写入 · \(ctx.periodLabel)"), used: window.1,
                           quota: ctx.workersPaid ? 50_000_000 : 100_000, valueText: compact(window.1)),
        ]
        if let storage = await d1StorageBytes(ctx) {
            rows.append(WidgetUsageRow(title: String(localized: "存储"), used: storage, quota: 5_000_000_000,
                                       valueText: bytes(storage)))
        }
        return WidgetUsageService(id: "d1", name: "D1", rows: rows)
    }

    /// D1 存储：REST 数据库列表 file_size 求和（需 d1.read，失败省略该行）
    private static func d1StorageBytes(_ ctx: Context) async -> Int? {
        struct Envelope: Decodable {
            let result: [Database]?
            struct Database: Decodable {
                let fileSize: Int?
                enum CodingKeys: String, CodingKey {
                    case fileSize = "file_size"
                }
            }
        }
        var request = URLRequest(
            url: URL(string: "https://api.cloudflare.com/client/v4/accounts/\(ctx.accountId)/d1/database?per_page=100")!
        )
        request.setValue("Bearer \(ctx.token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 10
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let decoded = try? JSONDecoder().decode(Envelope.self, from: data),
              let databases = decoded.result else { return nil }
        return databases.reduce(0) { $0 + ($1.fileSize ?? 0) }
    }

    private static func fetchKV(_ ctx: Context) async -> WidgetUsageService? {
        let (periodDay, todayDay, untilDay) = dateWindows(periodStart: ctx.periodStart)
        let query = """
        query ($accountTag: string!, $periodStart: Date!, $todayStart: Date!, $until: Date!) {
          viewer { accounts(filter: { accountTag: $accountTag }) {
            period: kvOperationsAdaptiveGroups(limit: 10000, filter: { date_geq: $periodStart, date_leq: $until }) { dimensions { actionType } sum { requests } }
            today: kvOperationsAdaptiveGroups(limit: 10000, filter: { date_geq: $todayStart, date_leq: $until }) { dimensions { actionType } sum { requests } }
          } }
        }
        """
        struct Node: Codable { let period: [Group]?; let today: [Group]? }
        struct Group: Codable { let dimensions: Dim?; let sum: Sum? }
        struct Dim: Codable { let actionType: String? }
        struct Sum: Codable { let requests: Int? }

        guard let node: Node = await graphQLFirstAccount(
            token: ctx.token, query: query,
            variables: ["accountTag": ctx.accountId, "periodStart": periodDay, "todayStart": todayDay, "until": untilDay]
        ) else { return nil }

        func totals(_ groups: [Group]?) -> (reads: Int, writes: Int) {
            (groups ?? []).reduce(into: (0, 0)) {
                let count = $1.sum?.requests ?? 0
                switch $1.dimensions?.actionType {
                case "read": $0.0 += count
                case "write": $0.1 += count
                default: break
                }
            }
        }
        let window = ctx.workersPaid ? totals(node.period) : totals(node.today)
        var rows = [
            WidgetUsageRow(title: String(localized: "读取 · \(ctx.periodLabel)"), used: window.reads,
                           quota: ctx.workersPaid ? 10_000_000 : 100_000, valueText: compact(window.reads)),
            WidgetUsageRow(title: String(localized: "写入 · \(ctx.periodLabel)"), used: window.writes,
                           quota: ctx.workersPaid ? 1_000_000 : 1_000, valueText: compact(window.writes)),
        ]
        if let storage = await kvStorageBytes(ctx) {
            rows.append(WidgetUsageRow(title: String(localized: "存储"), used: storage, quota: 1_000_000_000,
                                       valueText: bytes(storage)))
        }
        return WidgetUsageService(id: "kv", name: "KV", rows: rows)
    }

    /// KV 存储：各 namespace 当日 max byteCount 求和（独立查询，失败省略该行）
    private static func kvStorageBytes(_ ctx: Context) async -> Int? {
        let (_, todayDay, untilDay) = dateWindows(periodStart: ctx.periodStart)
        let query = """
        query ($accountTag: string!, $todayStart: Date!, $until: Date!) {
          viewer { accounts(filter: { accountTag: $accountTag }) {
            storage: kvStorageAdaptiveGroups(limit: 1000, filter: { date_geq: $todayStart, date_leq: $until }) {
              dimensions { namespaceId }
              max { byteCount }
            }
          } }
        }
        """
        struct Node: Codable { let storage: [Group]? }
        struct Group: Codable { let max: Max? }
        struct Max: Codable { let byteCount: Int? }

        guard let node: Node = await graphQLFirstAccount(
            token: ctx.token, query: query,
            variables: ["accountTag": ctx.accountId, "todayStart": todayDay, "until": untilDay]
        ) else { return nil }
        return (node.storage ?? []).reduce(0) { $0 + ($1.max?.byteCount ?? 0) }
    }

    // MARK: - 网络基础

    /// GraphQL 信封（泛型函数内不能嵌套类型，提到类型级）
    private struct GraphQLEnvelope<Node: Decodable>: Decodable {
        let data: Payload?
        struct Payload: Decodable { let viewer: Viewer }
        struct Viewer: Decodable { let accounts: [Node] }
    }

    /// 只探 GraphQL 错误层的 authz 码（账户级无权限）；与数据解码分开，HTTP 200 也要看 body
    private struct GraphQLErrorPeek: Decodable {
        let errors: [Item]?
        struct Item: Decodable {
            let message: String?
            let extensions: Ext?
            struct Ext: Decodable { let code: String? }
        }
        var isAuthz: Bool {
            errors?.contains { $0.extensions?.code == "authz" || $0.message == "not authorized for that account" } ?? false
        }
    }

    /// 通用 GraphQL：取第一个 account 节点解码为 N
    private static func graphQLFirstAccount<N: Decodable>(
        token: String, query: String, variables: [String: String]
    ) async -> N? {
        let body: [String: Any] = ["query": query, "variables": variables]
        guard let encoded = try? JSONSerialization.data(withJSONObject: body) else { return nil }

        var request = URLRequest(url: URL(string: "https://api.cloudflare.com/client/v4/graphql")!)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 12
        request.httpBody = encoded

        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        // GraphQL authz（账户级无权限）：HTTP 仍 200、错误在 body。落标志到 App Group，
        // 让用量 Widget 即便 App 未打开也能显示「账户级用量不可用」。
        if let peek = try? JSONDecoder().decode(GraphQLErrorPeek.self, from: data), peek.isAuthz {
            WidgetDataStore.saveAccountAnalyticsAvailable(false)
            return nil
        }
        guard let decoded = try? JSONDecoder().decode(GraphQLEnvelope<N>.self, from: data) else { return nil }
        if decoded.data?.viewer.accounts.first != nil {
            WidgetDataStore.saveAccountAnalyticsAvailable(true)
        }
        return decoded.data?.viewer.accounts.first
    }

    /// R2 账号级存储（REST，Standard 类字节数）
    private static func r2Metrics(_ ctx: Context) async -> Int? {
        struct Envelope: Decodable {
            let result: Metrics?
            struct Metrics: Decodable { let standard: ClassMetrics? }
            struct ClassMetrics: Decodable { let published: Snapshot?; let unpublished: Snapshot? }
            struct Snapshot: Decodable { let payloadSize: Int?; let metadataSize: Int? }
        }
        var request = URLRequest(
            url: URL(string: "https://api.cloudflare.com/client/v4/accounts/\(ctx.accountId)/r2/metrics")!
        )
        request.setValue("Bearer \(ctx.token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 10
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let decoded = try? JSONDecoder().decode(Envelope.self, from: data),
              let standard = decoded.result?.standard else { return nil }
        let published = (standard.published?.payloadSize ?? 0) + (standard.published?.metadataSize ?? 0)
        let unpublished = (standard.unpublished?.payloadSize ?? 0) + (standard.unpublished?.metadataSize ?? 0)
        return published + unpublished
    }

    // MARK: - 窗口与格式化

    private static func timeWindows(periodStart: Date) -> (month: String, today: String, now: String) {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        // 「今日」日界跟随主 App 设置（DayBoundary，默认 UTC）；
        // dateWindows 的 date 维度按 UTC 天聚合，不随设置变化
        let calendar = DayBoundary.current.calendar
        let now = Date()
        return (
            formatter.string(from: periodStart),
            formatter.string(from: calendar.startOfDay(for: now)),
            formatter.string(from: now)
        )
    }

    private static func dateWindows(periodStart: Date) -> (period: String, today: String, until: String) {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")!
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let now = Date()
        return (
            formatter.string(from: periodStart),
            formatter.string(from: calendar.startOfDay(for: now)),
            formatter.string(from: now)
        )
    }

    private static func compact(_ value: Int) -> String {
        value.formatted(.number.notation(.compactName))
    }

    private static func bytes(_ value: Int) -> String {
        Int64(value).ocBytes
    }
}

/// R2 A/B 类操作归类（与主 App 的计价口径一致）
nonisolated enum R2OperationClass {

    static let classA: Set<String> = [
        "ListBuckets", "PutBucket", "ListObjects", "PutObject", "CopyObject",
        "CompleteMultipartUpload", "CreateMultipartUpload", "UploadPart",
        "UploadPartCopy", "ListMultipartUploads", "ListParts",
        "PutBucketEncryption", "PutBucketCors", "PutBucketLifecycleConfiguration",
        "LifecycleStorageTierTransition",
    ]

    static let classB: Set<String> = [
        "HeadBucket", "HeadObject", "GetObject", "UsageSummary",
        "GetBucketEncryption", "GetBucketLocation", "GetBucketCors",
        "GetBucketLifecycleConfiguration",
    ]
}
