//
//  PagesModels.swift
//  Orange Cloud
//
//  Cloudflare Pages（account 级）：项目 + 部署。读 page.read，写 page.write。
//  字段名核对自 Cloudflare 官方 SDK（cloudflare-python types/pages）。
//  注意：GET 项目时 secret_text 类型的环境变量 value 为 null（已脱敏），故 App 内环境变量只读展示。
//

import Foundation

// MARK: - 项目

nonisolated struct PagesProject: Codable, Identifiable, Sendable {
    let name:              String
    let subdomain:         String?
    let domains:           [String]?
    let productionBranch:  String?
    let createdOn:         String?
    let buildConfig:       PagesBuildConfig?
    let deploymentConfigs: PagesDeploymentConfigs?
    let latestDeployment:  PagesDeployment?
    let source:            PagesSource?

    var id: String { name }

    enum CodingKeys: String, CodingKey {
        case name, subdomain, domains, source
        case productionBranch  = "production_branch"
        case createdOn         = "created_on"
        case buildConfig       = "build_config"
        case deploymentConfigs = "deployment_configs"
        case latestDeployment  = "latest_deployment"
    }
}

nonisolated struct PagesBuildConfig: Codable, Sendable {
    var buildCommand:   String?
    var destinationDir: String?
    var rootDir:        String?

    enum CodingKeys: String, CodingKey {
        case buildCommand   = "build_command"
        case destinationDir = "destination_dir"
        case rootDir        = "root_dir"
    }
}

nonisolated struct PagesDeploymentConfigs: Codable, Sendable {
    var production: PagesEnvConfig?
    var preview:    PagesEnvConfig?
}

nonisolated struct PagesEnvConfig: Codable, Sendable {
    var envVars: [String: PagesEnvVar]?

    enum CodingKeys: String, CodingKey {
        case envVars = "env_vars"
    }
}

nonisolated struct PagesEnvVar: Codable, Sendable {
    var type:  String?     // plain_text | secret_text
    var value: String?     // secret_text 时为 null

    var isSecret: Bool { type == "secret_text" }
}

nonisolated struct PagesSource: Codable, Sendable {
    let type:   String?    // github | gitlab
    let config: PagesSourceConfig?
}

nonisolated struct PagesSourceConfig: Codable, Sendable {
    let owner:            String?
    let repoName:         String?
    let productionBranch: String?

    enum CodingKeys: String, CodingKey {
        case owner
        case repoName         = "repo_name"
        case productionBranch = "production_branch"
    }

    /// owner/repo 展示
    var repoLabel: String? {
        guard let repoName else { return nil }
        return owner.map { "\($0)/\(repoName)" } ?? repoName
    }
}

// MARK: - 部署

nonisolated struct PagesDeployment: Codable, Identifiable, Sendable {
    let id:                String
    let shortId:           String?
    let projectName:       String?
    let environment:       String?    // production | preview
    let url:               String?
    let createdOn:         String?
    let modifiedOn:        String?
    let aliases:           [String]?
    let isSkipped:         Bool?
    let latestStage:       PagesStage?
    let stages:            [PagesStage]?
    let deploymentTrigger: PagesDeploymentTrigger?

    enum CodingKeys: String, CodingKey {
        case id, environment, url, stages, aliases
        case shortId           = "short_id"
        case projectName       = "project_name"
        case createdOn         = "created_on"
        case modifiedOn        = "modified_on"
        case isSkipped         = "is_skipped"
        case latestStage       = "latest_stage"
        case deploymentTrigger = "deployment_trigger"
    }

    /// 整体状态（取最新阶段）
    var status: PagesDeployStatus { PagesDeployStatus(rawValue: latestStage?.status ?? "") ?? .unknown }

    var isProduction: Bool { environment == "production" }
}

nonisolated struct PagesStage: Codable, Identifiable, Sendable {
    let name:      String?
    let status:    String?    // success | idle | active | failure | canceled
    let startedOn: String?
    let endedOn:   String?

    var id: String { name ?? UUID().uuidString }

    enum CodingKeys: String, CodingKey {
        case name, status
        case startedOn = "started_on"
        case endedOn   = "ended_on"
    }

    var statusValue: PagesDeployStatus { PagesDeployStatus(rawValue: status ?? "") ?? .unknown }
}

nonisolated struct PagesDeploymentTrigger: Codable, Sendable {
    let type:     String?
    let metadata: PagesTriggerMetadata?
}

nonisolated struct PagesTriggerMetadata: Codable, Sendable {
    let branch:        String?
    let commitHash:    String?
    let commitMessage: String?

    enum CodingKeys: String, CodingKey {
        case branch
        case commitHash    = "commit_hash"
        case commitMessage = "commit_message"
    }

    /// 短哈希
    var shortHash: String? { commitHash.map { String($0.prefix(8)) } }
}

// MARK: - 部署状态

nonisolated enum PagesDeployStatus: String, Sendable {
    case success, idle, active, failure, canceled
    case unknown = ""

    var label: String {
        switch self {
        case .success:  String(localized: "成功")
        case .idle:     String(localized: "排队中")
        case .active:   String(localized: "进行中")
        case .failure:  String(localized: "失败")
        case .canceled: String(localized: "已取消")
        case .unknown:  String(localized: "未知")
        }
    }
}

// MARK: - 自定义域名

/// 项目自定义域名。GET /accounts/{id}/pages/projects/{name}/domains
nonisolated struct PagesDomain: Codable, Identifiable, Sendable {
    let id:                   String
    let name:                 String
    let status:               String?   // initializing | pending | active | deactivated | blocked | error
    let zoneTag:              String?   // 域名所在 Zone（在当前 Cloudflare 上才有意义）
    let createdOn:            String?
    let certificateAuthority: String?
    let validationData:       PagesDomainValidationData?
    let verificationData:     PagesDomainVerificationData?

    enum CodingKeys: String, CodingKey {
        case id, name, status
        case zoneTag              = "zone_tag"
        case createdOn            = "created_on"
        case certificateAuthority = "certificate_authority"
        case validationData       = "validation_data"
        case verificationData     = "verification_data"
    }

    var statusValue: PagesDomainStatus { PagesDomainStatus(rawValue: status ?? "") ?? .unknown }
}

/// 证书验证信息（method == txt 时给出待添加的 TXT 记录）
nonisolated struct PagesDomainValidationData: Codable, Sendable {
    let status:       String?
    let method:       String?    // http | txt
    let txtName:      String?
    let txtValue:     String?
    let errorMessage: String?

    enum CodingKeys: String, CodingKey {
        case status, method
        case txtName      = "txt_name"
        case txtValue     = "txt_value"
        case errorMessage = "error_message"
    }
}

/// 域名归属验证信息
nonisolated struct PagesDomainVerificationData: Codable, Sendable {
    let status:       String?
    let errorMessage: String?

    enum CodingKeys: String, CodingKey {
        case status
        case errorMessage = "error_message"
    }
}

nonisolated enum PagesDomainStatus: String, Sendable {
    case initializing, pending, active, deactivated, blocked, error
    case unknown = ""

    var label: String {
        switch self {
        case .active:       String(localized: "生效中")
        case .pending:      String(localized: "验证中")
        case .initializing: String(localized: "初始化")
        case .deactivated:  String(localized: "已停用")
        case .blocked:      String(localized: "已封锁")
        case .error:        String(localized: "错误")
        case .unknown:      String(localized: "未知")
        }
    }
}

/// POST .../domains 请求体
nonisolated struct PagesDomainAddRequest: Codable, Sendable {
    let name: String
}

// MARK: - 写入载荷

/// PATCH 项目：仅传要改的字段（顶层合并，省略字段不变）。环境变量不在此（脱敏风险，App 内只读）。
nonisolated struct PagesProjectUpdate: Codable, Sendable {
    var buildConfig:      PagesBuildConfig?
    var productionBranch: String?

    enum CodingKeys: String, CodingKey {
        case buildConfig      = "build_config"
        case productionBranch = "production_branch"
    }
}

/// POST /accounts/{id}/pages/projects 请求体。仅建一个 Direct Upload 空项目
/// （手机端无法上传构建产物 / 连 Git，建后需用 Wrangler 或 Dashboard 部署）。
nonisolated struct PagesCreateRequest: Codable, Sendable {
    let name:             String
    let productionBranch: String

    enum CodingKeys: String, CodingKey {
        case name
        case productionBranch = "production_branch"
    }
}

/// retry / rollback 的空 POST 体
nonisolated struct PagesEmptyBody: Codable, Sendable {}

// MARK: - 直接上传部署（Direct Upload）

/// GET .../upload-token 的 result（资源上传用的短期 JWT）
nonisolated struct PagesUploadToken: Codable, Sendable {
    let jwt: String
}

/// POST /pages/assets/upload 的单条载荷（key=资源哈希，value=base64 内容）
nonisolated struct PagesAssetUpload: Codable, Sendable {
    let key:      String
    let value:    String
    let metadata: PagesAssetMetadata
    let base64:   Bool
}

nonisolated struct PagesAssetMetadata: Codable, Sendable {
    let contentType: String     // CF 期望 camelCase contentType
}

/// check-missing / upsert-hashes 的请求体
nonisolated struct PagesHashesBody: Codable, Sendable {
    let hashes: [String]
}

/// 待部署的单个文件。path 以 / 开头（如 /index.html）；contentType 按扩展名推断。
nonisolated struct PagesDeployFile: Sendable, Identifiable {
    let path: String
    let data: Data

    var id: String { path }
    var contentType: String { PagesMime.type(forPath: path) }
}

/// 按扩展名推断 MIME（覆盖常见静态资源，其余回退 octet-stream）
nonisolated enum PagesMime {
    static func type(forPath path: String) -> String {
        switch (path as NSString).pathExtension.lowercased() {
        case "html", "htm":   "text/html"
        case "css":           "text/css"
        case "js", "mjs":     "application/javascript"
        case "json":          "application/json"
        case "map":           "application/json"
        case "webmanifest":   "application/manifest+json"
        case "svg":           "image/svg+xml"
        case "png":           "image/png"
        case "jpg", "jpeg":   "image/jpeg"
        case "gif":           "image/gif"
        case "webp":          "image/webp"
        case "avif":          "image/avif"
        case "ico":           "image/x-icon"
        case "txt":           "text/plain"
        case "md":            "text/markdown"
        case "xml":           "application/xml"
        case "pdf":           "application/pdf"
        case "wasm":          "application/wasm"
        case "woff":          "font/woff"
        case "woff2":         "font/woff2"
        case "ttf":           "font/ttf"
        case "otf":           "font/otf"
        default:              "application/octet-stream"
        }
    }
}
