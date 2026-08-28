//
//  TurnstileEditorSheet.swift
//  Orange Cloud
//
//  Turnstile 组件新建 / 编辑表单（sheet，叶子页）。
//  region 创建后不可改；offlabel / ephemeral_id 是 ENT 专属不开放；
//  clearance_level 编辑时原样回写，避免 PUT 丢配置。
//

import SwiftUI

struct TurnstileEditorSheet: View {

    let viewModel: TurnstileViewModel
    let existing: TurnstileWidget?
    var onSaved: ((TurnstileWidget) -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var mode: TurnstileMode
    @State private var domainsText: String
    @State private var botFightMode: Bool
    @State private var region = "world"

    init(viewModel: TurnstileViewModel, existing: TurnstileWidget?, onSaved: ((TurnstileWidget) -> Void)? = nil) {
        self.viewModel = viewModel
        self.existing = existing
        self.onSaved = onSaved
        _name = State(initialValue: existing?.name ?? "")
        _mode = State(initialValue: TurnstileMode(apiValue: existing?.mode ?? "managed"))
        _domainsText = State(initialValue: (existing?.domains ?? []).joined(separator: "\n"))
        _botFightMode = State(initialValue: existing?.botFightMode ?? false)
    }

    private var isEditing: Bool { existing != nil }

    /// 每行一个域名，去空白去空行去重（保序）
    private var domains: [String] {
        var seen = Set<String>()
        return domainsText
            .split(whereSeparator: \.isNewline)
            .map { $0.trimmingCharacters(in: .whitespaces).lowercased() }
            .filter { !$0.isEmpty && seen.insert($0).inserted }
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && domains.count <= 10
            && !viewModel.isSaving
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("名称") {
                    TextField("如：官网登录页", text: $name)
                }

                Section {
                    Picker("模式", selection: $mode) {
                        ForEach(TurnstileMode.allCases) { Text($0.label).tag($0) }
                    }
                } footer: {
                    Text(mode.detail)
                }

                Section {
                    TextEditor(text: $domainsText)
                        .font(.callout.monospaced())
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .frame(minHeight: 90)
                } header: {
                    Text("域名")
                } footer: {
                    if domains.count > 10 {
                        Text("最多 10 个域名（当前 \(domains.count) 个）。").foregroundStyle(.red)
                    } else {
                        Text("每行一个主机名或 IP，子域自动生效；留空表示任意主机名均可使用。")
                    }
                }

                Section {
                    Toggle("Bot Fight Mode", isOn: $botFightMode)
                } footer: {
                    Text("对疑似机器人的请求发放高算力挑战，加大自动化攻击成本。")
                }

                if !isEditing {
                    Section {
                        Picker("区域", selection: $region) {
                            Text("全球").tag("world")
                            Text("中国").tag("china")
                        }
                    } footer: {
                        Text("创建后不可更改。")
                    }
                }

                if let error = viewModel.error {
                    Section { Text(error).font(.footnote).foregroundStyle(.red) }
                }
            }
            .navigationTitle(isEditing ? String(localized: "编辑组件") : String(localized: "新建组件"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if viewModel.isSaving { ProgressView() } else { Text("保存").fontWeight(.semibold) }
                    }
                    .disabled(!canSave)
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
        }
    }

    private func save() async {
        viewModel.error = nil
        let input = TurnstileWidgetInput(
            name: name.trimmingCharacters(in: .whitespaces),
            mode: mode.rawValue,
            domains: domains,
            botFightMode: botFightMode,
            region: isEditing ? nil : region,
            clearanceLevel: existing?.clearanceLevel   // 原样回写，避免 PUT 丢配置
        )
        let saved: TurnstileWidget?
        if let existing {
            saved = await viewModel.update(sitekey: existing.sitekey, input: input)
        } else {
            saved = await viewModel.create(input: input)
        }
        if let saved {
            onSaved?(saved)
            dismiss()
        }
    }
}
