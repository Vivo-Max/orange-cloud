//
//  PushCenterView.swift
//  Orange Cloud
//
//  推送中心：启用 → 拿到永久端点 URL（复制 / 二维码 / 测试）→ 收件箱。免登录。
//

import SwiftUI
import UIKit

struct PushCenterView: View {

    @State private var vm = PushCenterViewModel()
    @State private var copied = false
    @Environment(AuthManager.self) private var auth

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                statusIsland
                if let endpoint = vm.registrar.endpointURL {
                    endpointIsland(endpoint)
                    e2eIsland
                    if auth.isLoggedIn {
                        cfAlertingIsland(endpoint)
                    }
                }
                inboxSection
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .navigationTitle("推送中心")
        .navigationBarTitleDisplayMode(.inline)
        .task { vm.refresh() }
        .refreshable { vm.refresh() }
    }

    // MARK: - 状态 / 启用

    @ViewBuilder private var statusIsland: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "bell.badge.fill")
                    .font(.title2)
                    .foregroundStyle(Color.ocOrange)
                VStack(alignment: .leading, spacing: 2) {
                    Text("端点推送")
                        .font(.headline)
                    Text("给自己的端点 curl 一下，就推到这台设备。免登录。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            switch vm.registrar.status {
            case .registered:
                Label("已启用", systemImage: "checkmark.circle.fill")
                    .font(.subheadline)
                    .foregroundStyle(.green)
            case .registering:
                HStack(spacing: 8) {
                    ProgressView()
                    Text("注册中…").foregroundStyle(.secondary)
                }
            case .denied:
                VStack(alignment: .leading, spacing: 8) {
                    Label("通知权限被拒，横幅不会弹出", systemImage: "exclamationmark.triangle")
                        .font(.caption)
                        .foregroundStyle(.orange)
                    enableButton("仍然启用端点")
                }
            case .failed:
                VStack(alignment: .leading, spacing: 8) {
                    if let error = vm.error {
                        Text(error).font(.caption).foregroundStyle(.red)
                    }
                    enableButton("重试")
                }
            case .idle:
                enableButton("启用推送")
            }
        }
        .padding(OCLayout.islandPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }

    private func enableButton(_ title: LocalizedStringKey) -> some View {
        Button {
            Task { await vm.enable() }
        } label: {
            Text(title).frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .tint(.ocOrange)
    }

    // MARK: - 端点

    @ViewBuilder private func endpointIsland(_ endpoint: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("你的端点")
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(endpoint)
                .font(.callout.monospaced())
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)

            if let qr = QRCode.image(endpoint) {
                Image(uiImage: qr)
                    .interpolation(.none)
                    .resizable()
                    .frame(width: 160, height: 160)
                    .frame(maxWidth: .infinity)
            }

            HStack(spacing: 10) {
                Button {
                    UIPasteboard.general.string = endpoint
                    copied = true
                } label: {
                    Label(copied ? "已复制" : "复制", systemImage: copied ? "checkmark" : "doc.on.doc")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Button {
                    Task { await vm.sendTest() }
                } label: {
                    HStack(spacing: 6) {
                        if vm.testSending { ProgressView() }
                        if let result = vm.testResult { Text(result) } else { Text("发送测试") }
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(.ocOrange)
                .disabled(vm.testSending)
            }

            Text("示例：curl \(endpoint)/标题/内容")
                .font(.caption2.monospaced())
                .foregroundStyle(.tertiary)
                .textSelection(.enabled)
        }
        .padding(OCLayout.islandPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }

    // MARK: - CF 告警推送（仅登录后）

    @ViewBuilder private func cfAlertingIsland(_ endpoint: String) -> some View {
        NavigationLink {
            CFAlertingView(endpointURL: endpoint)
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: "exclamationmark.bubble")
                        .foregroundStyle(Color.ocOrange)
                    Text("CF 告警推送")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.primary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
                Text("把 Cloudflare 告警（DDoS、健康检查、证书到期、Workers 错误率、部署失败…）推到这台设备。需账号下有 Pro 及以上套餐的域名。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.leading)
            }
            .padding(OCLayout.islandPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassIsland()
        }
        .buttonStyle(.plain)
    }

    // MARK: - 端到端加密

    @ViewBuilder private var e2eIsland: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "lock.shield")
                    .foregroundStyle(Color.ocOrange)
                Text("端到端加密")
                    .font(.subheadline.weight(.medium))
            }
            if let key = vm.e2eKey {
                Text(key)
                    .font(.callout.monospaced())
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(8)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
                Text("把这串密钥填进你的脚本，用 AES-CBC 加密内容后发 ciphertext + iv；中继看不到明文。")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                HStack(spacing: 10) {
                    Button {
                        UIPasteboard.general.string = key
                    } label: {
                        Label("复制密钥", systemImage: "doc.on.doc").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    Button(role: .destructive) {
                        vm.clearE2EKey()
                    } label: {
                        Label("移除", systemImage: "trash").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            } else {
                Text("可选：生成一个 32 位密钥，端到端加密推送内容。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button {
                    vm.generateE2EKey()
                } label: {
                    Label("生成加密密钥", systemImage: "key").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(.ocOrange)
            }
        }
        .padding(OCLayout.islandPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }

    // MARK: - 收件箱

    @ViewBuilder private var inboxSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("收件箱")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                if !vm.inbox.isEmpty {
                    Button("清空") { vm.clearInbox() }
                        .font(.caption)
                        .tint(.ocOrange)
                }
            }
            .padding(.horizontal, 4)

            if vm.inbox.isEmpty {
                ToolNotice(
                    systemImage: "tray",
                    title: "暂无消息",
                    message: String(localized: "收到的推送会出现在这里，包括 App 在后台时到达的。")
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(vm.inbox.enumerated()), id: \.element.id) { index, msg in
                        if index > 0 {
                            Divider().padding(.leading, OCLayout.islandPadding)
                        }
                        inboxRow(msg)
                    }
                }
                .glassIsland()
            }
        }
    }

    private func inboxRow(_ msg: PushMessage) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                if let title = msg.title {
                    Text(title).font(.subheadline.weight(.medium))
                }
                Spacer()
                Text(msg.date, style: .time)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            if let subtitle = msg.subtitle {
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
            Text(msg.body)
                .font(.callout)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let url = msg.url, let link = URL(string: url) {
                Link(destination: link) {
                    Text(url).font(.caption).foregroundStyle(Color.ocOrangeText).lineLimit(1)
                }
            }
        }
        .padding(.horizontal, OCLayout.islandPadding)
        .padding(.vertical, 9)
    }
}
