//
//  PagesBuildConfigEditorView.swift
//  Orange Cloud
//
//  Pages 构建配置编辑：构建命令 / 输出目录 / 根目录 / 生产分支（PATCH 顶层合并）。
//

import SwiftUI

struct PagesBuildConfigEditorView: View {

    let viewModel: PagesProjectDetailViewModel

    @Environment(\.dismiss) private var dismiss

    @State private var buildCommand: String
    @State private var destinationDir: String
    @State private var rootDir: String
    @State private var productionBranch: String

    init(viewModel: PagesProjectDetailViewModel) {
        self.viewModel = viewModel
        let build = viewModel.project.buildConfig
        _buildCommand     = State(initialValue: build?.buildCommand ?? "")
        _destinationDir   = State(initialValue: build?.destinationDir ?? "")
        _rootDir          = State(initialValue: build?.rootDir ?? "")
        _productionBranch = State(initialValue: viewModel.project.productionBranch ?? "")
    }

    var body: some View {
        Form {
            Section {
                TextField("如 npm run build", text: $buildCommand)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("构建命令")
            }

            Section {
                TextField("如 dist", text: $destinationDir)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("输出目录")
            } footer: {
                Text("构建产物所在目录（destination dir）。")
            }

            Section {
                TextField("如 /", text: $rootDir)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("根目录")
            } footer: {
                Text("Monorepo 时构建的子目录（root dir），留空为仓库根。")
            }

            Section {
                TextField("如 main", text: $productionBranch)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("生产分支")
            }

            if let error = viewModel.error {
                Section {
                    Text(error).font(.footnote).foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("构建配置")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button {
                    Task { await save() }
                } label: {
                    if viewModel.isMutating {
                        ProgressView()
                    } else {
                        Text("保存").fontWeight(.semibold)
                    }
                }
                .disabled(viewModel.isMutating)
            }
        }
        .interactiveDismissDisabled(viewModel.isMutating)
        .onDisappear { viewModel.error = nil }
    }

    private func save() async {
        let build = PagesBuildConfig(
            buildCommand:   buildCommand.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
            destinationDir: destinationDir.trimmingCharacters(in: .whitespaces).nilIfEmpty,
            rootDir:        rootDir.trimmingCharacters(in: .whitespaces).nilIfEmpty
        )
        let branch = productionBranch.trimmingCharacters(in: .whitespaces).nilIfEmpty
        if await viewModel.updateBuildConfig(build, productionBranch: branch) {
            dismiss()
        }
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
