//
//  WorkerUploadView.swift
//  Orange Cloud
//
//  新建 Worker / 更新现有 Worker（Sheet）。新建支持三种方式：
//  ① 单文件：粘贴或导入一个 .js；② 多模块：导入多个模块文件并指定入口；
//  ③ 静态资源：选 ZIP / 多文件部署为纯静态站（Workers Assets）。
//  更新现有 Worker 走单文件并保留绑定。入口已按 workers-scripts.write 门控。
//

import SwiftUI
import UniformTypeIdentifiers

struct WorkerUploadView: View {

    enum Mode: Equatable {
        case create
        case replace(scriptName: String)
    }

    enum UploadKind: String, CaseIterable, Identifiable {
        case single, modules, assets
        var id: String { rawValue }
        var label: LocalizedStringKey {
            switch self {
            case .single:  "单文件"
            case .modules: "多模块"
            case .assets:  "静态资源"
            }
        }
    }

    let mode: Mode
    let viewModel: WorkerUploadViewModel
    let onSuccess: () -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var kind: UploadKind = .single
    @State private var name = ""
    @State private var compatibilityDate = Date()

    // 单文件
    @State private var code = ""
    @State private var isModule = true
    @State private var showCodeImporter = false
    @State private var didLoadSource = false   // 原地编辑：只预填一次

    // 多模块
    @State private var modules: [WorkerUploadModule] = []
    @State private var entryName = ""
    @State private var showModuleImporter = false

    // 静态资源
    @State private var assets: [PagesDeployFile] = []
    @State private var spa = false
    @State private var showAssetImporter = false

    @State private var importError: String?

    private var isCreate: Bool { mode == .create }
    private var effectiveKind: UploadKind { isCreate ? kind : .single }

    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var nameValid: Bool { isCreate ? WorkerUploadViewModel.isValidName(trimmedName) : true }

    private var entryCandidates: [String] { modules.map(\.name).filter { WorkerModuleType.canBeEntry($0) } }

    private var canSubmit: Bool {
        guard nameValid, !viewModel.isUploading else { return false }
        if case .replace = mode, viewModel.isLoadingSource || viewModel.sourceUneditable {
            return false   // 原地编辑：源码未就绪 / 多模块不可替换时禁止部署
        }
        switch effectiveKind {
        case .single:  return !code.isEmpty
        case .modules: return !modules.isEmpty && entryCandidates.contains(entryName)
        case .assets:  return !assets.isEmpty
        }
    }

    private var title: String {
        isCreate ? String(localized: "新建 Worker") : String(localized: "更新代码")
    }

    var body: some View {
        NavigationStack {
            Form {
                if isCreate {
                    Section {
                        Picker("上传方式", selection: $kind) {
                            ForEach(UploadKind.allCases) { Text($0.label).tag($0) }
                        }
                        .pickerStyle(.segmented)
                        .disabled(viewModel.isUploading)
                    }
                    nameSection
                    compatDateSection
                }

                switch effectiveKind {
                case .single:  singleSection
                case .modules: modulesSection
                case .assets:  assetsSection
                }

                if viewModel.isUploading, effectiveKind == .assets, viewModel.totalAssets > 0 {
                    Section {
                        HStack(spacing: 10) {
                            ProgressView()
                            Text("正在上传资源 \(viewModel.uploadedAssets)/\(viewModel.totalAssets)…")
                                .font(.callout).foregroundStyle(.secondary)
                        }
                    }
                }

                if let error = importError ?? viewModel.error {
                    Section { Text(error).font(.footnote).foregroundStyle(.red) }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }.disabled(viewModel.isUploading)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await submit() }
                    } label: {
                        if viewModel.isUploading {
                            ProgressView()
                        } else {
                            Text(isCreate ? String(localized: "创建") : String(localized: "部署")).fontWeight(.semibold)
                        }
                    }
                    .disabled(!canSubmit)
                }
            }
            .interactiveDismissDisabled(viewModel.isUploading)
            .onAppear { viewModel.error = nil }
            .task { await loadSourceIfNeeded() }
            .fileImporter(isPresented: $showCodeImporter, allowedContentTypes: [.javaScript, .text, .item], allowsMultipleSelection: false) { importCode($0) }
            .fileImporter(isPresented: $showModuleImporter, allowedContentTypes: [.javaScript, .text, .item], allowsMultipleSelection: true) { importModules($0) }
            .fileImporter(isPresented: $showAssetImporter, allowedContentTypes: [.item], allowsMultipleSelection: true) { importAssets($0) }
        }
    }

    // MARK: - 公共字段（仅新建）

    private var nameSection: some View {
        Section {
            TextField("worker-name", text: $name)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .font(.callout.monospaced())
                .disabled(viewModel.isUploading)
        } header: {
            Text("名称")
        } footer: {
            Text("小写字母 / 数字开头，可含连字符与下划线。")
        }
    }

    private var compatDateSection: some View {
        Section {
            DatePicker("兼容性日期", selection: $compatibilityDate, in: ...Date(), displayedComponents: .date)
                .disabled(viewModel.isUploading)
        }
    }

    // MARK: - 单文件

    private var singleSection: some View {
        Group {
            Section {
                Picker("格式", selection: $isModule) {
                    Text("ES Module").tag(true)
                    Text("Service Worker").tag(false)
                }
                .pickerStyle(.segmented)
                // 编辑现有脚本时格式由源码决定，不允许更改（避免以错误类型回写）
                .disabled(viewModel.isUploading || !isCreate)
            } footer: {
                Text(isModule
                     ? String(localized: "ES Module：export default { fetch }（推荐）。")
                     : String(localized: "Service Worker：addEventListener(\"fetch\", …)。"))
            }
            Section {
                if !isCreate && viewModel.isLoadingSource {
                    HStack(spacing: 10) {
                        ProgressView()
                        Text("正在读取源码…").font(.callout).foregroundStyle(.secondary)
                    }
                } else if !isCreate && viewModel.sourceUneditable {
                    Label("这是多模块打包 Worker，无法在此原地编辑（会丢失其它模块）。可在「新建」里用多模块方式整体替换。", systemImage: "exclamationmark.triangle")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                } else {
                    TextEditor(text: $code)
                        .font(.callout.monospaced())
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .frame(minHeight: 220)
                        .disabled(viewModel.isUploading)
                    Button { showCodeImporter = true } label: {
                        Label("从 .js 文件导入", systemImage: "doc.badge.plus")
                    }
                    .disabled(viewModel.isUploading)
                }
            } header: {
                Text("代码")
            } footer: {
                if !isCreate && !viewModel.sourceUneditable && !viewModel.isLoadingSource {
                    Text("已载入当前线上源码，可直接修改后部署。现有变量 / 密钥 / 绑定会自动保留。")
                }
            }
        }
    }

    /// 原地编辑：进 sheet 时读取现有源码预填（仅 .replace 单文件模式，只做一次）
    private func loadSourceIfNeeded() async {
        guard case .replace(let scriptName) = mode, !didLoadSource else { return }
        didLoadSource = true
        if let content = await viewModel.fetchSource(scriptName: scriptName),
           let main = content.mainModule {
            code = main.body
            isModule = content.isModule
        }
    }

    // MARK: - 多模块

    private var modulesSection: some View {
        Group {
            Section {
                Button { showModuleImporter = true } label: {
                    Label("选择模块文件", systemImage: "doc.on.doc")
                }
                .disabled(viewModel.isUploading)
                ForEach(modules) { module in
                    HStack {
                        Text(module.name).font(.caption.monospaced()).lineLimit(1).truncationMode(.middle)
                        Spacer()
                        Text(Int64(module.data.count).ocBytes)
                            .font(.caption2).foregroundStyle(.secondary)
                    }
                }
                .onDelete { modules.remove(atOffsets: $0); fixEntry() }
            } header: {
                Text("模块")
            } footer: {
                Text("JS / .mjs 为代码模块，.wasm / 其它作为数据模块一并上传。")
            }

            if !entryCandidates.isEmpty {
                Section {
                    Picker("入口模块", selection: $entryName) {
                        ForEach(entryCandidates, id: \.self) { Text($0).tag($0) }
                    }
                    .disabled(viewModel.isUploading)
                } footer: {
                    Text("入口模块即 main_module，负责导出 fetch 等处理函数。")
                }
            }
        }
    }

    // MARK: - 静态资源

    private var assetsSection: some View {
        Group {
            Section {
                Button { showAssetImporter = true } label: {
                    Label("选择文件 / ZIP", systemImage: "folder.badge.plus")
                }
                .disabled(viewModel.isUploading)
                ForEach(assets) { file in
                    HStack {
                        Text(file.path).font(.caption.monospaced()).lineLimit(1).truncationMode(.middle)
                        Spacer()
                        Text(Int64(file.data.count).ocBytes)
                            .font(.caption2).foregroundStyle(.secondary)
                    }
                }
            } header: {
                Text("静态文件")
            } footer: {
                Text(assets.isEmpty
                     ? String(localized: "部署为纯静态站点（无 Worker 代码）。ZIP 会在设备端解包，统一顶层目录自动去掉。")
                     : String(localized: "共 \(assets.count) 个文件。"))
            }
            Section {
                Toggle("单页应用（SPA）", isOn: $spa).disabled(viewModel.isUploading)
            } footer: {
                Text("开启后，未匹配到文件的请求回退到 index.html（适合前端路由）。")
            }
        }
    }

    // MARK: - 提交

    private func submit() async {
        guard canSubmit else { return }
        viewModel.error = nil
        importError = nil
        let ok: Bool
        switch mode {
        case .replace(let scriptName):
            ok = await viewModel.replace(scriptName: scriptName, code: code, isModule: isModule)
        case .create:
            switch kind {
            case .single:
                ok = await viewModel.create(name: trimmedName, code: code, isModule: isModule, compatibilityDate: Self.format(compatibilityDate))
            case .modules:
                ok = await viewModel.createMultiModule(name: trimmedName, modules: modules, entryName: entryName, compatibilityDate: Self.format(compatibilityDate))
            case .assets:
                ok = await viewModel.createWithAssets(name: trimmedName, assets: assets, compatibilityDate: Self.format(compatibilityDate), spa: spa)
            }
        }
        if ok {
            onSuccess()
            dismiss()
        }
    }

    // MARK: - 文件导入

    private func importCode(_ result: Result<[URL], Error>) {
        guard case .success(let urls) = result, let url = urls.first else { return }
        guard let text = readText(url) else { importError = String(localized: "无法以文本读取该文件"); return }
        code = text
        if isCreate, trimmedName.isEmpty {
            name = url.deletingPathExtension().lastPathComponent.lowercased()
        }
    }

    private func importModules(_ result: Result<[URL], Error>) {
        guard case .success(let urls) = result else { return }
        importError = nil
        var added = modules
        for url in urls {
            guard let data = readData(url) else { continue }
            let n = url.lastPathComponent
            added.removeAll { $0.name == n }
            added.append(WorkerUploadModule(name: n, data: data, contentType: WorkerModuleType.contentType(forName: n)))
        }
        modules = added
        if isCreate, trimmedName.isEmpty, let first = urls.first {
            name = first.deletingPathExtension().lastPathComponent.lowercased()
        }
        fixEntry()
    }

    private func importAssets(_ result: Result<[URL], Error>) {
        guard case .success(let urls) = result else { return }
        importError = nil
        var collected: [(path: String, data: Data)] = []
        var failures: [String] = []
        for url in urls {
            guard let data = readData(url) else { failures.append(url.lastPathComponent); continue }
            if url.pathExtension.lowercased() == "zip" {
                do {
                    let entries = try ZipReader.entries(from: data)
                    collected.append(contentsOf: entries.map { (path: $0.path, data: $0.data) })
                } catch {
                    failures.append("\(url.lastPathComponent)：\(error.localizedDescription)")
                }
            } else {
                collected.append((path: url.lastPathComponent, data: data))
            }
        }
        assets = PagesDeployViewModel.normalize(collected)
        if isCreate, trimmedName.isEmpty { name = "static-site" }
        if !failures.isEmpty {
            importError = String(localized: "部分文件无法读取：") + "\n" + failures.joined(separator: "\n")
        } else if assets.isEmpty {
            importError = String(localized: "未找到可部署的文件")
        }
    }

    /// 入口模块缺失时，自动落到第一个可作入口的模块
    private func fixEntry() {
        if !entryCandidates.contains(entryName) {
            entryName = entryCandidates.first ?? ""
        }
    }

    private func readData(_ url: URL) -> Data? {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        return try? Data(contentsOf: url)
    }

    private func readText(_ url: URL) -> String? {
        guard let data = readData(url) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func format(_ date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }
}
