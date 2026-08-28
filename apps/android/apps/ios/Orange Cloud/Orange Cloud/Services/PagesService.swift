//
//  PagesService.swift
//  Orange Cloud
//
//  Cloudflare Pages（account 级）：项目与部署 CRUD。读 page.read，写 page.write。
//  端点核对自 Cloudflare 官方 SDK（cloudflare-python resources/pages）。
//

import Foundation

struct PagesService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    // MARK: - 项目

    /// 项目列表（CF Pages 列表端点只认 `page`：传 `per_page` 会被拒——
    /// “Invalid list options provided”，故用服务端默认页大小逐页取，靠 total_pages 收尾）
    func listProjects(accountId: String) async throws -> [PagesProject] {
        var all: [PagesProject] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<PagesProject> = try await client.get(
                "accounts/\(accountId)/pages/projects",
                queryItems: [URLQueryItem(name: "page", value: String(page))]
            )
            guard response.success else { throw response.toAPIError() }
            let pageItems = response.result ?? []
            all.append(contentsOf: pageItems)
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages, !pageItems.isEmpty, page < 20 else { break }
            page += 1
        }
        return all
    }

    func getProject(accountId: String, projectName: String) async throws -> PagesProject {
        let response: CFAPIResponse<PagesProject> = try await client.get(
            "accounts/\(accountId)/pages/projects/\(projectName)"
        )
        guard response.success, let project = response.result else {
            throw response.toAPIError()
        }
        return project
    }

    /// 创建项目（page.write）。建一个 Direct Upload 空项目，返回新建的 PagesProject。
    func createProject(accountId: String, name: String, productionBranch: String) async throws -> PagesProject {
        let response: CFAPIResponse<PagesProject> = try await client.post(
            "accounts/\(accountId)/pages/projects",
            body: PagesCreateRequest(name: name, productionBranch: productionBranch)
        )
        guard response.success, let project = response.result else {
            throw response.toAPIError()
        }
        return project
    }

    /// PATCH 项目（构建配置 / 生产分支）。仅传要改的字段，顶层合并。
    func updateProject(accountId: String, projectName: String, update: PagesProjectUpdate) async throws -> PagesProject {
        let response: CFAPIResponse<PagesProject> = try await client.patch(
            "accounts/\(accountId)/pages/projects/\(projectName)",
            body: update
        )
        guard response.success, let project = response.result else {
            throw response.toAPIError()
        }
        return project
    }

    func deleteProject(accountId: String, projectName: String) async throws {
        try await client.delete("accounts/\(accountId)/pages/projects/\(projectName)")
    }

    // MARK: - 部署

    /// 部署列表（CF Pages 默认每页 25，须翻页，否则活跃项目的旧部署 / 回滚目标取不到）。
    /// 同 listProjects：只传 `page`，`per_page` 会被端点拒。
    func listDeployments(accountId: String, projectName: String) async throws -> [PagesDeployment] {
        var all: [PagesDeployment] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<PagesDeployment> = try await client.get(
                "accounts/\(accountId)/pages/projects/\(projectName)/deployments",
                queryItems: [URLQueryItem(name: "page", value: String(page))]
            )
            guard response.success else { throw response.toAPIError() }
            let pageItems = response.result ?? []
            all.append(contentsOf: pageItems)
            let totalPages = response.resultInfo?.totalPages ?? 1
            // 安全上限：避免极端情况下无限翻页（默认每页 25，取近 10 页≈250 条足够）
            guard page < totalPages, !pageItems.isEmpty, page < 10 else { break }
            page += 1
        }
        return all
    }

    func getDeployment(accountId: String, projectName: String, deploymentId: String) async throws -> PagesDeployment {
        let response: CFAPIResponse<PagesDeployment> = try await client.get(
            "accounts/\(accountId)/pages/projects/\(projectName)/deployments/\(deploymentId)"
        )
        guard response.success, let deployment = response.result else {
            throw response.toAPIError()
        }
        return deployment
    }

    /// 重试部署（重新构建并部署）
    func retryDeployment(accountId: String, projectName: String, deploymentId: String) async throws -> PagesDeployment {
        let response: CFAPIResponse<PagesDeployment> = try await client.post(
            "accounts/\(accountId)/pages/projects/\(projectName)/deployments/\(deploymentId)/retry",
            body: PagesEmptyBody()
        )
        guard response.success, let deployment = response.result else {
            throw response.toAPIError()
        }
        return deployment
    }

    /// 回滚到某次部署（使其重新生效）
    func rollbackDeployment(accountId: String, projectName: String, deploymentId: String) async throws -> PagesDeployment {
        let response: CFAPIResponse<PagesDeployment> = try await client.post(
            "accounts/\(accountId)/pages/projects/\(projectName)/deployments/\(deploymentId)/rollback",
            body: PagesEmptyBody()
        )
        guard response.success, let deployment = response.result else {
            throw response.toAPIError()
        }
        return deployment
    }

    func deleteDeployment(accountId: String, projectName: String, deploymentId: String) async throws {
        try await client.delete("accounts/\(accountId)/pages/projects/\(projectName)/deployments/\(deploymentId)")
    }

    // MARK: - 自定义域名

    /// 项目自定义域名列表
    func listDomains(accountId: String, projectName: String) async throws -> [PagesDomain] {
        let response: CFAPIResponseArray<PagesDomain> = try await client.get(
            "accounts/\(accountId)/pages/projects/\(projectName)/domains"
        )
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    /// 挂载自定义域名（page.write）。挂载后仍需 DNS 指向 <project>.pages.dev 才能生效。
    func addDomain(accountId: String, projectName: String, name: String) async throws -> PagesDomain {
        let response: CFAPIResponse<PagesDomain> = try await client.post(
            "accounts/\(accountId)/pages/projects/\(projectName)/domains",
            body: PagesDomainAddRequest(name: name)
        )
        guard response.success, let domain = response.result else { throw response.toAPIError() }
        return domain
    }

    /// 重新验证域名（PATCH 触发重试；DNS 记录补好后用它催一次）
    func retryDomain(accountId: String, projectName: String, domainName: String) async throws {
        let response: CFAPIResponse<JSONValue> = try await client.patch(
            "accounts/\(accountId)/pages/projects/\(projectName)/domains/\(domainName)",
            body: PagesEmptyBody()
        )
        guard response.success else { throw response.toAPIError() }
    }

    /// 从项目移除自定义域名（不动 DNS 记录）
    func deleteDomain(accountId: String, projectName: String, domainName: String) async throws {
        try await client.delete("accounts/\(accountId)/pages/projects/\(projectName)/domains/\(domainName)")
    }

    // MARK: - 直接上传部署（Direct Upload）
    //
    // 流程对齐 wrangler：① 取上传 JWT → ② check-missing 问服务端缺哪些资源
    // → ③ 把缺的资源 base64 分批 upload → ④ upsert-hashes 关联全部哈希
    // → ⑤ 带 manifest（路径→哈希）创建部署。资源端点用 JWT（非 OAuth token）鉴权。
    //

    /// 取资源上传用的短期 JWT
    func uploadToken(accountId: String, projectName: String) async throws -> String {
        let response: CFAPIResponse<PagesUploadToken> = try await client.get(
            "accounts/\(accountId)/pages/projects/\(projectName)/upload-token"
        )
        guard response.success, let token = response.result else { throw response.toAPIError() }
        return token.jwt
    }

    /// 询问服务端缺哪些资源哈希（仅缺的才需上传）
    func checkMissingAssets(jwt: String, hashes: [String]) async throws -> [String] {
        let body = try JSONEncoder().encode(PagesHashesBody(hashes: hashes))
        let data = try await client.bearerJSON(method: "POST", path: "pages/assets/check-missing", bearer: jwt, body: body)
        let response = try JSONDecoder().decode(CFAPIResponse<[String]>.self, from: data)
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    /// 上传一批资源（base64）
    func uploadAssets(jwt: String, payloads: [PagesAssetUpload]) async throws {
        let body = try JSONEncoder().encode(payloads)
        let data = try await client.bearerJSON(method: "POST", path: "pages/assets/upload", bearer: jwt, body: body)
        // result 可能是布尔或计数，只关心 success
        let response = try JSONDecoder().decode(CFAPIResponse<JSONValue>.self, from: data)
        guard response.success else { throw response.toAPIError() }
    }

    /// 关联（保活）本次部署涉及的全部哈希
    func upsertHashes(jwt: String, hashes: [String]) async throws {
        let body = try JSONEncoder().encode(PagesHashesBody(hashes: hashes))
        let data = try await client.bearerJSON(method: "POST", path: "pages/assets/upsert-hashes", bearer: jwt, body: body)
        let response = try JSONDecoder().decode(CFAPIResponse<JSONValue>.self, from: data)
        guard response.success else { throw response.toAPIError() }
    }

    /// 带 manifest 创建部署（manifest 为「/路径」→ 资源哈希 的 JSON）
    func createDeployment(accountId: String, projectName: String, manifest: [String: String]) async throws -> PagesDeployment {
        let manifestJSON = String(decoding: try JSONEncoder().encode(manifest), as: UTF8.self)
        let response: CFAPIResponse<PagesDeployment> = try await client.multipartFields(
            method: "POST",
            "accounts/\(accountId)/pages/projects/\(projectName)/deployments",
            fields: ["manifest": manifestJSON]
        )
        guard response.success, let deployment = response.result else { throw response.toAPIError() }
        return deployment
    }
}
