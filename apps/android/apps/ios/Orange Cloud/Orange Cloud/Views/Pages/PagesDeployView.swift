//
//  PagesDeployView.swift
//  Orange Cloud
//
//  Pages「直接上传」部署表单（Sheet）：两种来源——
//  ① 粘贴代码：输入文件名 + 正文（默认 index.html），适合快速发一个静态页；
//  ② 选择文件：文件选择器选 zip / html / 多个静态资源，zip 在设备端解包后逐文件上传。
//  入口（PagesProjectDetailView）已按 page.write 门控。
//

import SwiftUI
import UniformTypeIdentifiers

struct PagesDeployView: View {

    let viewModel: PagesDeployViewModel
    let onSuccess: () -> Void

    @Environment(\.dismiss) private var dismiss

    private enum Source: String, CaseIterable, Identifiable {
        case paste, files
        var id: String { rawValue }
        var label: LocalizedStringKey { self == .paste ? "粘贴代码" : "选择文件" }
    }

    @State private var source: Source = .paste
    @State private var filename = "index.html"
    @State private var code = ""
    @State private var pickedFiles: [PagesDeployFile] = []
    @State private var showImporter = false
    @State private var pickError: String?

    private var trimmedFilename: String {
        filename.trimmingCharacters(in: CharacterSet(charactersIn: "/ \n\t"))
    }

    /// 当前要部署的文件集合
    private var filesToDeploy: [PagesDeployFile] {
        switch source {
        case .paste:
            guard !trimmedFilename.isEmpty, !code.isEmpty else { return [] }
            return [PagesDeployFile(path: "/" + trimmedFilename, data: Data(code.utf8))]
        case .files:
            return pickedFiles
        }
    }

    private var canDeploy: Bool { !filesToDeploy.isEmpty && !viewModel.isDeploying }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("来源", selection: $source) {
                        ForEach(Source.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .disabled(viewModel.isDeploying)
                }

                switch source {
                case .paste: pasteSection
                case .files: filesSection
                }

                if viewModel.isDeploying || viewModel.phase == .done {
                    progressSection
                }

                if let error = pickError ?? viewModel.error {
                    Section { Text(error).font(.footnote).foregroundStyle(.red) }
                }
            }
            .navigationTitle("部署")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }.disabled(viewModel.isDeploying)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await deploy() }
                    } label: {
                        if viewModel.isDeploying { ProgressView() } else { Text("部署").fontWeight(.semibold) }
                    }
                    .disabled(!canDeploy)
                }
            }
            .interactiveDismissDisabled(viewModel.isDeploying)
            .onAppear {
                // 复用同一 VM 时，重开表单清掉上次的结束/失败态
                if !viewModel.isDeploying {
                    viewModel.phase = .idle
                    viewModel.error = nil
                }
            }
            .fileImporter(
                isPresented: $showImporter,
                allowedContentTypes: [.item],
                allowsMultipleSelection: true
            ) { result in
                handleImport(result)
            }
        }
    }

    // MARK: - 粘贴代码

    private var pasteSection: some View {
        Group {
            Section {
                TextField("文件名", text: $filename)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .font(.callout.monospaced())
                    .disabled(viewModel.isDeploying)
            } header: {
                Text("文件名")
            } footer: {
                Text("通常用 index.html 作为站点首页；也可用子路径，如 about/index.html。")
            }

            Section {
                TextEditor(text: $code)
                    .font(.callout.monospaced())
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .frame(minHeight: 220)
                    .disabled(viewModel.isDeploying)
            } header: {
                Text("内容")
            }
        }
    }

    // MARK: - 选择文件

    private var filesSection: some View {
        Section {
            Button {
                showImporter = true
            } label: {
                Label("选择文件 / ZIP", systemImage: "folder.badge.plus")
            }
            .disabled(viewModel.isDeploying)

            if !pickedFiles.isEmpty {
                ForEach(pickedFiles) { file in
                    HStack {
                        Text(file.path).font(.caption.monospaced()).lineLimit(1).truncationMode(.middle)
                        Spacer()
                        Text(Int64(file.data.count).ocBytes)
                            .font(.caption2).foregroundStyle(.secondary)
                    }
                }
            }
        } header: {
            Text("文件")
        } footer: {
            Text(pickedFiles.isEmpty
                 ? String(localized: "可选单个 ZIP（设备端自动解包）或多个静态文件。ZIP 内若有统一顶层目录会自动去掉。")
                 : String(localized: "共 \(pickedFiles.count) 个文件，将作为一次新部署上传。"))
        }
    }

    // MARK: - 进度

    private var progressSection: some View {
        Section {
            HStack(spacing: 10) {
                if viewModel.phase == .done {
                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                } else {
                    ProgressView()
                }
                Text(phaseLabel)
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var phaseLabel: String {
        switch viewModel.phase {
        case .idle:      ""
        case .hashing:   String(localized: "正在计算文件指纹…")
        case .uploading: String(localized: "正在上传资源 \(viewModel.uploadedCount)/\(viewModel.totalToUpload)…")
        case .creating:  String(localized: "正在创建部署…")
        case .done:      String(localized: "部署已创建")
        case .failed:    String(localized: "部署失败")
        }
    }

    // MARK: - 动作

    private func deploy() async {
        guard canDeploy else { return }
        pickError = nil
        if await viewModel.deploy(files: filesToDeploy) != nil {
            onSuccess()
            dismiss()
        }
    }

    private func handleImport(_ result: Result<[URL], Error>) {
        pickError = nil
        viewModel.error = nil
        switch result {
        case .failure(let error):
            pickError = error.localizedDescription
        case .success(let urls):
            var collected: [(path: String, data: Data)] = []
            var failures: [String] = []
            for url in urls {
                let scoped = url.startAccessingSecurityScopedResource()
                defer { if scoped { url.stopAccessingSecurityScopedResource() } }
                guard let data = try? Data(contentsOf: url) else {
                    failures.append(url.lastPathComponent)
                    continue
                }
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
            pickedFiles = PagesDeployViewModel.normalize(collected)
            if !failures.isEmpty {
                pickError = String(localized: "部分文件无法读取：") + "\n" + failures.joined(separator: "\n")
            } else if pickedFiles.isEmpty {
                pickError = String(localized: "未找到可部署的文件")
            }
        }
    }
}
