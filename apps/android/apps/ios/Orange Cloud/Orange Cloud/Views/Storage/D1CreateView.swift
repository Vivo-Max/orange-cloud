//
//  D1CreateView.swift
//  Orange Cloud
//
//  D1 数据库创建表单（Sheet）：名称 + 可选主要位置。
//  入口（StorageView 的 + 按钮）已按 d1.write 门控，此处只管表单提交。
//

import SwiftUI

struct D1CreateView: View {

    let viewModel: D1DatabaseListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var location: D1Location = .automatic
    @FocusState private var nameFocused: Bool

    private var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var canCreate: Bool {
        !trimmedName.isEmpty && !accountId.isEmpty && !viewModel.isCreating
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("数据库名称", text: $name)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .focused($nameFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await create() } }
                } header: {
                    Text("名称")
                } footer: {
                    Text("为数据库起一个便于识别的名字。")
                }

                Section {
                    Picker("主要位置", selection: $location) {
                        ForEach(D1Location.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                } footer: {
                    Text("主要位置决定数据库主副本所在区域，选择「自动」由 Cloudflare 就近分配。")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("创建数据库")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await create() }
                    } label: {
                        if viewModel.isCreating {
                            ProgressView()
                        } else {
                            Text("创建").fontWeight(.semibold)
                        }
                    }
                    .disabled(!canCreate)
                }
            }
            .onAppear { nameFocused = true }
            .interactiveDismissDisabled(viewModel.isCreating)
        }
    }

    private func create() async {
        guard canCreate else { return }
        nameFocused = false
        if await viewModel.create(accountId: accountId, name: trimmedName, locationHint: location.hint) {
            dismiss()
        }
    }
}

// MARK: - 主要位置选项（primary_location_hint）

/// D1 主副本放置提示。.automatic 不传 hint，由 Cloudflare 按首个查询来源就近分配。
private enum D1Location: String, CaseIterable, Identifiable {
    case automatic, wnam, enam, weur, eeur, apac, oc

    var id: String { rawValue }

    var hint: String? { self == .automatic ? nil : rawValue }

    var label: LocalizedStringKey {
        switch self {
        case .automatic: "自动"
        case .wnam:      "北美西部"
        case .enam:      "北美东部"
        case .weur:      "欧洲西部"
        case .eeur:      "欧洲东部"
        case .apac:      "亚太地区"
        case .oc:        "大洋洲"
        }
    }
}
