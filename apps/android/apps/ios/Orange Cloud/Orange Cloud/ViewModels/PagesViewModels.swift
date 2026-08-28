//
//  PagesViewModels.swift
//  Orange Cloud
//
//  Cloudflare Pages：项目列表 + 项目详情（部署列表 + 重试/回滚/删除部署、改构建配置、删项目）。
//

import Foundation
import Observation

@Observable
@MainActor
final class PagesProjectListViewModel {

    private(set) var projects: [PagesProject] = []
    var isLoading = false
    var loaded = false
    var error: String?
    var isCreating = false
    var didCreate = false      // sensoryFeedback 触发器

    private let service: PagesService

    init(service: PagesService) {
        self.service = service
    }

    /// 创建 Direct Upload 空项目：成功后插到列表顶端，返回 true。
    func create(accountId: String, name: String, productionBranch: String) async -> Bool {
        guard !isCreating else { return false }
        isCreating = true
        error = nil
        defer { isCreating = false }
        do {
            let created = try await service.createProject(
                accountId: accountId, name: name, productionBranch: productionBranch
            )
            projects.insert(created, at: 0)
            didCreate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func load(accountId: String) async {
        isLoading = true
        error = nil
        do {
            projects = try await service.listProjects(accountId: accountId)
            loaded = true
        } catch is CancellationError {
            // 下拉刷新 / searchable 取消，不算加载失败
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

@Observable
@MainActor
final class PagesProjectDetailViewModel {

    var project: PagesProject
    private(set) var deployments: [PagesDeployment] = []
    var isLoadingDeployments = false
    var deploymentsLoaded = false
    var isMutating = false
    var error: String?
    var didMutate = false      // sensoryFeedback 触发器

    private let service: PagesService
    let accountId: String

    var projectName: String { project.name }

    init(project: PagesProject, accountId: String, service: PagesService) {
        self.project = project
        self.accountId = accountId
        self.service = service
    }

    func loadDeployments() async {
        isLoadingDeployments = true
        error = nil
        do {
            deployments = try await service.listDeployments(accountId: accountId, projectName: project.name)
            deploymentsLoaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingDeployments = false
    }

    func refreshProject() async {
        if let updated = try? await service.getProject(accountId: accountId, projectName: project.name) {
            project = updated
        }
    }

    func retry(_ deployment: PagesDeployment) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            _ = try await service.retryDeployment(accountId: accountId, projectName: project.name, deploymentId: deployment.id)
            await loadDeployments()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func rollback(_ deployment: PagesDeployment) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            _ = try await service.rollbackDeployment(accountId: accountId, projectName: project.name, deploymentId: deployment.id)
            await loadDeployments()
            await refreshProject()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func deleteDeployment(_ deployment: PagesDeployment) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deleteDeployment(accountId: accountId, projectName: project.name, deploymentId: deployment.id)
            deployments.removeAll { $0.id == deployment.id }
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 改构建配置 / 生产分支（PATCH 顶层合并）
    func updateBuildConfig(_ build: PagesBuildConfig, productionBranch: String?) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            project = try await service.updateProject(
                accountId: accountId, projectName: project.name,
                update: PagesProjectUpdate(buildConfig: build, productionBranch: productionBranch)
            )
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func deleteProject() async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deleteProject(accountId: accountId, projectName: project.name)
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}

// MARK: - 自定义域名

@Observable
@MainActor
final class PagesDomainsViewModel {

    /// 某个域名的 DNS 解析状态（zone 在当前账号内时才可查/可写）
    enum DNSState: Equatable {
        case resolved(String)     // 已有记录指向本项目（content）
        case conflicting(String)  // 已有解析记录但未指向本项目
        case missing              // zone 在账号内且尚无解析记录，可一键添加 CNAME
        case external             // zone 不在当前账号（去域名服务商处配置）
        case unknown              // 无 dns.read 或查询失败
    }

    private(set) var domains: [PagesDomain] = []
    private(set) var dnsStates: [String: DNSState] = [:]   // key = 域名
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false      // sensoryFeedback 触发器

    private let service: PagesService
    private let dnsService: DNSService
    let accountId: String
    let projectName: String
    /// CNAME 目标：<project>.pages.dev
    let cnameTarget: String?

    init(service: PagesService, dnsService: DNSService, accountId: String, projectName: String, subdomain: String?) {
        self.service = service
        self.dnsService = dnsService
        self.accountId = accountId
        self.projectName = projectName
        self.cnameTarget = subdomain
    }

    func load(canReadDNS: Bool) async {
        isLoading = true
        error = nil
        do {
            domains = try await service.listDomains(accountId: accountId, projectName: projectName)
            loaded = true
            await refreshDNSStates(canReadDNS: canReadDNS)
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// 逐域名查解析记录（zone 不在账号内或无权限时标记为不可查）
    private func refreshDNSStates(canReadDNS: Bool) async {
        var states: [String: DNSState] = [:]
        for domain in domains {
            guard let zoneTag = domain.zoneTag, !zoneTag.isEmpty else {
                states[domain.name] = .external
                continue
            }
            guard canReadDNS, let target = cnameTarget else {
                states[domain.name] = .unknown
                continue
            }
            do {
                let records = try await dnsService.listRecords(zoneId: zoneTag, name: domain.name)
                    .filter { ["CNAME", "A", "AAAA"].contains($0.type.uppercased()) }
                if let hit = records.first(where: { $0.content.lowercased() == target.lowercased() }) {
                    states[domain.name] = .resolved(hit.content)
                } else if let other = records.first {
                    states[domain.name] = .conflicting(other.content)
                } else {
                    states[domain.name] = .missing
                }
            } catch {
                // zone 可能属于别的账号（403）或临时失败：不阻塞域名列表
                states[domain.name] = .unknown
            }
        }
        dnsStates = states
    }

    func add(name: String, canReadDNS: Bool, canWriteDNS: Bool) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            _ = try await service.addDomain(accountId: accountId, projectName: projectName, name: name)
            await load(canReadDNS: canReadDNS)
            // 绑定后自动补解析：zone 在本账号且尚无记录时直接创建 CNAME，
            // 用户无需再手动点「添加 CNAME 记录」。失败不影响绑定结果，
            // 静默回退到列表里的手动按钮。
            if canWriteDNS, case .missing = dnsStates[name] ?? .unknown,
               let domain = domains.first(where: { $0.name == name }) {
                if await performCreateCNAME(for: domain) == false {
                    AppLog.network.notice("pages auto-CNAME failed for domain, fallback to manual button")
                    error = nil
                }
            }
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func retry(_ domain: PagesDomain, canReadDNS: Bool) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.retryDomain(accountId: accountId, projectName: projectName, domainName: domain.name)
            await load(canReadDNS: canReadDNS)
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ domain: PagesDomain) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deleteDomain(accountId: accountId, projectName: projectName, domainName: domain.name)
            domains.removeAll { $0.id == domain.id }
            dnsStates[domain.name] = nil
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 一键在域名所在 Zone 添加 CNAME → <project>.pages.dev（dns.write）
    func createCNAME(for domain: PagesDomain) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        let ok = await performCreateCNAME(for: domain)
        if ok { didMutate.toggle() }
        return ok
    }

    /// CNAME 创建核心（绑定后的自动补建与手动按钮共用；不管 isMutating 闸）
    private func performCreateCNAME(for domain: PagesDomain) async -> Bool {
        guard let zoneTag = domain.zoneTag, let target = cnameTarget else { return false }
        do {
            let record = CreateDNSRecord(
                type: "CNAME", name: domain.name, content: target,
                proxied: true, ttl: 1, priority: nil, comment: nil
            )
            _ = try await dnsService.createRecord(zoneId: zoneTag, record: record)
            dnsStates[domain.name] = .resolved(target)
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}

// MARK: - 直接上传部署

@Observable
@MainActor
final class PagesDeployViewModel {

    enum Phase: Equatable {
        case idle, hashing, uploading, creating, done, failed
    }

    /// Pages 单文件上限 25 MiB
    static let maxFileBytes = 25 * 1024 * 1024

    var phase: Phase = .idle
    var uploadedCount = 0
    var totalToUpload = 0
    var error: String?

    var isDeploying: Bool { phase == .hashing || phase == .uploading || phase == .creating }

    private let service: PagesService
    let accountId: String
    let projectName: String

    init(service: PagesService, accountId: String, projectName: String) {
        self.service = service
        self.accountId = accountId
        self.projectName = projectName
    }

    /// 执行整套直接上传部署，成功返回新部署。
    func deploy(files: [PagesDeployFile]) async -> PagesDeployment? {
        guard !files.isEmpty, !isDeploying else { return nil }
        error = nil
        uploadedCount = 0
        totalToUpload = 0
        phase = .hashing

        if let big = files.first(where: { $0.data.count > Self.maxFileBytes }) {
            error = String(localized: "文件 \(big.path) 超过 25 MB，超出 Pages 单文件上限")
            phase = .failed
            return nil
        }

        // 算哈希并建 manifest（base64 缓存复用给上传腿，避免二次编码）
        var manifest: [String: String] = [:]
        var fileByHash: [String: PagesDeployFile] = [:]
        var b64ByHash: [String: String] = [:]
        for file in files {
            let ext = (file.path as NSString).pathExtension
            let b64 = file.data.base64EncodedString()
            let hash = Blake3.hashHexPrefix(Array((b64 + ext).utf8), prefixChars: 32)
            manifest[file.path] = hash
            fileByHash[hash] = file
            b64ByHash[hash] = b64
        }

        do {
            let jwt = try await service.uploadToken(accountId: accountId, projectName: projectName)
            let allHashes = Array(Set(manifest.values))
            let missing = try await service.checkMissingAssets(jwt: jwt, hashes: allHashes)

            let uploads = missing.compactMap { hash -> (hash: String, file: PagesDeployFile, b64: String)? in
                guard let file = fileByHash[hash], let b64 = b64ByHash[hash] else { return nil }
                return (hash, file, b64)
            }
            totalToUpload = uploads.count
            phase = .uploading
            for batch in Self.batches(uploads) {
                let payloads = batch.map {
                    PagesAssetUpload(
                        key: $0.hash,
                        value: $0.b64,
                        metadata: PagesAssetMetadata(contentType: $0.file.contentType),
                        base64: true
                    )
                }
                try await service.uploadAssets(jwt: jwt, payloads: payloads)
                uploadedCount += batch.count
            }

            try await service.upsertHashes(jwt: jwt, hashes: allHashes)
            phase = .creating
            let deployment = try await service.createDeployment(accountId: accountId, projectName: projectName, manifest: manifest)
            phase = .done
            return deployment
        } catch {
            self.error = error.localizedDescription
            phase = .failed
            return nil
        }
    }

    /// 按累计原始字节（~8MB，base64 后约 11MB/批）或 50 个一批分组上传
    private static func batches(_ uploads: [(hash: String, file: PagesDeployFile, b64: String)]) -> [[(hash: String, file: PagesDeployFile, b64: String)]] {
        let maxBytes = 8 * 1024 * 1024
        let maxCount = 50
        var result: [[(hash: String, file: PagesDeployFile, b64: String)]] = []
        var current: [(hash: String, file: PagesDeployFile, b64: String)] = []
        var currentBytes = 0
        for item in uploads {
            let size = item.file.data.count
            if !current.isEmpty, currentBytes + size > maxBytes || current.count >= maxCount {
                result.append(current)
                current = []
                currentBytes = 0
            }
            current.append(item)
            currentBytes += size
        }
        if !current.isEmpty { result.append(current) }
        return result
    }

    /// 把若干 (相对路径, 数据) 归一化成部署文件：剥掉共同顶层目录（zip 整目录的常见情形），
    /// 反斜杠转正斜杠，统一加前导 /。空路径丢弃。
    static func normalize(_ raw: [(path: String, data: Data)]) -> [PagesDeployFile] {
        let cleaned = raw.compactMap { item -> (path: String, data: Data)? in
            let p = item.path.replacingOccurrences(of: "\\", with: "/")
                .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            return p.isEmpty ? nil : (p, item.data)
        }
        guard !cleaned.isEmpty else { return [] }

        // 仅当所有文件都在同一个顶层目录下时才剥离该目录
        let topDirs = Set(cleaned.map { $0.path.split(separator: "/", maxSplits: 1).first.map(String.init) ?? "" })
        let hasSubpaths = cleaned.contains { $0.path.contains("/") }
        let commonTop: String? = (topDirs.count == 1 && hasSubpaths) ? topDirs.first : nil

        return cleaned.map { item in
            var path = item.path
            if let commonTop, path.hasPrefix(commonTop + "/") {
                path = String(path.dropFirst(commonTop.count + 1))
            }
            return PagesDeployFile(path: "/" + path, data: item.data)
        }
    }
}
