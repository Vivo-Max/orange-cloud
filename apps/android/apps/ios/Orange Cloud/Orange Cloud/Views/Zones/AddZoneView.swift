//
//  AddZoneView.swift
//  Orange Cloud
//
//  添加域名（新建 Zone）的 Sheet：
//  - 表单页：输入根域名 → POST /zones（full setup）
//  - 结果页：展示 Cloudflare 分配的名称服务器 + 在注册商处更换 NS 的步骤
//  参考 https://developers.cloudflare.com/fundamentals/manage-domains/add-site/
//

import SwiftUI
import SwiftData
import UIKit

struct AddZoneView: View {

    let accountId:   String
    let accountName: String

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    @State private var viewModel: AddZoneViewModel
    @State private var domain = ""
    @FocusState private var fieldFocused: Bool

    /// 用 String 常量（而非字面量）传入，走 StringProtocol 重载，不进本地化目录
    private let domainPlaceholder = "example.com"

    init(accountId: String, accountName: String, zoneService: ZoneService) {
        self.accountId = accountId
        self.accountName = accountName
        _viewModel = State(initialValue: AddZoneViewModel(zoneService: zoneService))
    }

    // MARK: - 域名规范化与校验

    /// 去掉协议头 / 路径 / 首尾点与空白，统一小写——用户常会整段粘贴 URL
    private var normalizedDomain: String {
        var s = domain.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if let scheme = s.range(of: "://") { s = String(s[scheme.upperBound...]) }
        if let slash = s.firstIndex(of: "/") { s = String(s[..<slash]) }
        return s.trimmingCharacters(in: CharacterSet(charactersIn: "."))
    }

    /// 宽松校验（最终以服务端为准）：至少两段、无空段、无空格、TLD ≥ 2 字符。
    /// 故意不限定 ASCII，避免误拒国际化域名（中文.com 等）。
    private var isValidDomain: Bool {
        let s = normalizedDomain
        guard s.count >= 3, !s.contains(" ") else { return false }
        let labels = s.split(separator: ".", omittingEmptySubsequences: false)
        guard labels.count >= 2, labels.allSatisfy({ !$0.isEmpty }) else { return false }
        return (labels.last?.count ?? 0) >= 2
    }

    private var canSubmit: Bool { isValidDomain && !viewModel.isSaving }

    var body: some View {
        NavigationStack {
            Group {
                if let zone = viewModel.createdZone {
                    AddZoneResultView(zone: zone) { dismiss() }
                } else {
                    form
                }
            }
            .navigationTitle(viewModel.createdZone == nil
                             ? String(localized: "添加域名")
                             : String(localized: "已添加"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if viewModel.createdZone == nil {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("取消") { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button {
                            Task { await submit() }
                        } label: {
                            if viewModel.isSaving {
                                ProgressView()
                            } else {
                                Text("添加").fontWeight(.semibold)
                            }
                        }
                        .disabled(!canSubmit)
                    }
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
        }
    }

    // MARK: - 表单

    private var form: some View {
        Form {
            Section {
                TextField(domainPlaceholder, text: $domain)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .font(.callout.monospaced())
                    .focused($fieldFocused)
                    .submitLabel(.go)
                    .onSubmit { if canSubmit { Task { await submit() } } }
            } header: {
                Text("域名")
            } footer: {
                Text("输入你已注册的根域名（如 example.com），无需带 www 或 https://。")
            }

            Section("添加到账号") {
                HStack(spacing: 10) {
                    Image(systemName: "person.crop.circle")
                        .foregroundStyle(Color.ocOrangeText)
                    Text(accountName)
                        .lineLimit(1)
                }
            }

            Section {
                Label {
                    Text("添加后，Cloudflare 会为该域名分配两个名称服务器。你需要登录域名注册商，把 NS 改成它们，域名才会激活。")
                } icon: {
                    Image(systemName: "info.circle")
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
            }

            if let error = viewModel.error {
                Section {
                    Text(error)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            }
        }
        .onAppear { fieldFocused = true }
    }

    private func submit() async {
        fieldFocused = false
        await viewModel.create(name: normalizedDomain, accountId: accountId, context: modelContext)
    }
}

// MARK: - 结果页：名称服务器 + 后续步骤

private struct AddZoneResultView: View {

    let zone: Zone
    let onDone: () -> Void

    @State private var copied = false

    private var nameServers: [String] { zone.nameServers ?? [] }

    private var steps: [String] {
        [
            String(localized: "登录你购买该域名的注册商（Registrar）。"),
            String(localized: "找到「名称服务器（Nameservers）」设置。"),
            String(localized: "删除原有名称服务器，替换为上方两个 Cloudflare 地址。"),
            String(localized: "保存更改。激活通常需几分钟到几小时，最长可达 24 小时。"),
        ]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                header

                if nameServers.isEmpty {
                    // full setup 通常会即时返回 NS；万一缺失，引导去 Dashboard 查看
                    sectionCard(String(localized: "名称服务器")) {
                        Text("Cloudflare 尚未返回名称服务器，请稍后在域名详情或 Cloudflare Dashboard 查看。")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                } else {
                    nameServerCard
                    stepsCard
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .safeAreaInset(edge: .bottom) {
            Button {
                onDone()
            } label: {
                Text("完成")
                    .fontWeight(.bold)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.ocOrangePressed)
            .controlSize(.large)
            .padding(OCLayout.pagePadding)
            .background(.ultraThinMaterial)
        }
    }

    // MARK: 子视图

    private var header: some View {
        VStack(spacing: 10) {
            ZoneAvatar(domain: zone.name, size: 52)
            Text(zone.name)
                .font(.system(.title2, weight: .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            HStack(spacing: 5) {
                StatusDot(status: zone.status, size: 7)
                    .accessibilityHidden(true)
                Text("待激活")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .glassIsland()
    }

    private var nameServerCard: some View {
        sectionCard(String(localized: "名称服务器")) {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(nameServers, id: \.self) { server in
                    Text(server)
                        .font(.callout.monospaced())
                        .textSelection(.enabled)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Button {
                    UIPasteboard.general.string = nameServers.joined(separator: "\n")
                    copied = true
                } label: {
                    Label(copied ? String(localized: "已复制") : String(localized: "复制名称服务器"),
                          systemImage: copied ? "checkmark" : "doc.on.doc")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color.ocOrangeText)
                }
                .contentTransition(.symbolEffect(.replace))
            }
        }
        .sensoryFeedback(.success, trigger: copied)
    }

    private var stepsCard: some View {
        sectionCard(String(localized: "接下来")) {
            VStack(alignment: .leading, spacing: 14) {
                ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                    HStack(alignment: .top, spacing: 12) {
                        Text("\(index + 1)")
                            .font(.caption.bold().monospaced())
                            .foregroundStyle(.white)
                            .frame(width: 22, height: 22)
                            .background(Color.ocOrangePressed, in: Circle())
                        Text(step)
                            .font(.footnote)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
    }

    // MARK: 分组卡（同 ZoneDetailView 的视觉）

    private func sectionCard(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
                .padding(.horizontal, 4)
            VStack(spacing: 0) {
                content()
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassIsland(cornerRadius: OCLayout.chipRadius)
        }
    }
}
