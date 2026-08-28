//
//  CFAlertingView.swift
//  Orange Cloud
//
//  CF 告警管理（登录后）：选账号 → 全量告警类型逐项开关 → 自动接线到推送端点。
//

import SwiftUI

struct CFAlertingView: View {

    let endpointURL: String

    @Environment(AuthManager.self) private var auth
    @State private var vm: CFAlertingViewModel?

    var body: some View {
        Group {
            if !auth.hasScope("notifications.read") {
                noScope
            } else if let vm {
                content(vm)
            } else {
                ProgressView().frame(maxWidth: .infinity).padding(.top, 40)
            }
        }
        .background { SkyBackground() }
        .navigationTitle("CF 告警推送")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if vm == nil { vm = CFAlertingViewModel(auth: auth, endpointURL: endpointURL) }
            await vm?.load()
        }
    }

    private var noScope: some View {
        ScrollView {
            ToolNotice(
                systemImage: "lock",
                title: "需要「通知」权限",
                message: String(localized: "管理 Cloudflare 告警需要 Notifications 权限。请到 设置 → 你的账号 重新登录以授予。"),
                tint: .orange
            )
            .padding(OCLayout.pagePadding)
        }
    }

    @ViewBuilder private func content(_ vm: CFAlertingViewModel) -> some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                if vm.accounts.count > 1 { accountPicker(vm) }
                webhookStatus(vm)

                if vm.isLoading && vm.groups.isEmpty {
                    ProgressView().padding(.top, 20)
                } else if let error = vm.error, vm.groups.isEmpty {
                    ToolNotice(systemImage: "exclamationmark.triangle", title: "加载失败", message: error, tint: .orange)
                } else {
                    if let error = vm.error {
                        ToolNotice(systemImage: "exclamationmark.triangle", title: "出错了", message: error, tint: .orange)
                    }
                    ForEach(vm.groups, id: \.category) { group in
                        alertGroup(vm, category: group.category, alerts: group.alerts)
                    }
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .refreshable { await vm.load() }
    }

    private func accountPicker(_ vm: CFAlertingViewModel) -> some View {
        Menu {
            ForEach(vm.accounts, id: \.id) { account in
                Button(account.name) { vm.selectAccount(account.id) }
            }
        } label: {
            HStack {
                TintIcon(systemImage: "person.crop.circle", color: .ocOrange)
                Text("账号").foregroundStyle(.secondary)
                Spacer()
                Text(vm.accounts.first(where: { $0.id == vm.selectedAccountId })?.name ?? String(localized: "选择"))
                    .foregroundStyle(.primary)
                Image(systemName: "chevron.up.chevron.down").font(.caption).foregroundStyle(.tertiary)
            }
            .padding(OCLayout.islandPadding)
            .frame(maxWidth: .infinity)
            .glassIsland()
        }
        .tint(.ocOrange)
    }

    private func webhookStatus(_ vm: CFAlertingViewModel) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: vm.pushWebhookId != nil ? "checkmark.seal.fill" : "link")
                    .foregroundStyle(vm.pushWebhookId != nil ? .green : Color.ocOrange)
                if vm.pushWebhookId != nil {
                    Text("已接到推送端点").font(.subheadline)
                } else {
                    Text("开启任一告警即自动接线").font(.subheadline)
                }
            }
            Text(vm.cfWebhookURL)
                .font(.caption.monospaced())
                .foregroundStyle(.secondary)
                .textSelection(.enabled)
                .lineLimit(1)
                .truncationMode(.middle)
        }
        .padding(OCLayout.islandPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }

    private func alertGroup(_ vm: CFAlertingViewModel, category: String, alerts: [CFAvailableAlert]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(category)
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)
            VStack(spacing: 0) {
                ForEach(Array(alerts.enumerated()), id: \.element.id) { index, alert in
                    if index > 0 { Divider().padding(.leading, OCLayout.islandPadding) }
                    alertRow(vm, alert)
                }
            }
            .glassIsland()
        }
    }

    private func alertRow(_ vm: CFAlertingViewModel, _ alert: CFAvailableAlert) -> some View {
        let on = vm.policy(for: alert.type) != nil
        return HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(alert.label).font(.callout)
                if let description = alert.description, !description.isEmpty {
                    Text(description).font(.caption2).foregroundStyle(.secondary).lineLimit(2)
                }
            }
            Spacer(minLength: 8)
            if vm.busyAlertType == alert.type {
                ProgressView()
            } else {
                Button {
                    Task { await vm.toggle(alertType: alert.type, displayName: alert.label) }
                } label: {
                    Image(systemName: on ? "checkmark.circle.fill" : "circle")
                        .font(.title3)
                        .foregroundStyle(on ? Color.ocOrange : Color.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, OCLayout.islandPadding)
        .padding(.vertical, 9)
    }
}
