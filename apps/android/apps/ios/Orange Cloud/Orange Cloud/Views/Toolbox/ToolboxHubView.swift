//
//  ToolboxHubView.swift
//  Orange Cloud
//
//  免登录工具箱 hub：无需 CF 账号的开发者随身工具。
//  从登录页次级按钮 / 设置入口 / 通知点按以全屏 cover 呈现（挂在 ContentView 根层）。
//  推送中心将作为旗舰卡片在后端就绪后接入（docs/12、docs/13）。
//

import SwiftUI

struct ToolboxHubView: View {

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    toolRow("bell.badge.fill", .ocOrange, "推送中心", "给端点推送 · 免登录") {
                        PushCenterView()
                    }
                } header: {
                    Text("推送")
                }
                .glassRow()

                Section {
                    toolRow("magnifyingglass", .blue, "DNS 查询", "A / AAAA / CNAME / MX / TXT…") {
                        DNSLookupToolView()
                    }
                    toolRow("network", .indigo, "HTTP 请求", "状态 / 响应头 / 耗时") {
                        HTTPProbeToolView()
                    }
                    toolRow("person.text.rectangle", .teal, "WHOIS", "注册信息（RDAP）") {
                        WhoisToolView()
                    }
                    toolRow("globe.americas", .orange, "IP 归属地", "GeoIP 定位") {
                        GeoIPToolView()
                    }
                } header: {
                    Text("网络查询")
                }
                .glassRow()

                Section {
                    toolRow("lock.shield", .green, "SSL 证书检查", "证书链 / 到期 / SAN") {
                        CertInspectToolView()
                    }
                    toolRow("point.3.connected.trianglepath.dotted", .purple, "CF 数据中心", "/cdn-cgi/trace · colo") {
                        CFTraceToolView()
                    }
                    toolRow("function", .pink, "CIDR 计算器", "子网 / 范围 / 主机数") {
                        CIDRToolView()
                    }
                } header: {
                    Text("诊断与计算")
                } footer: {
                    Text("以上工具均无需 Cloudflare 账号。")
                }
                .glassRow()
            }
            .daybreakList()
            .navigationTitle("开发者工具箱")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") { dismiss() }
                }
            }
        }
        .tint(.ocOrange)
    }

    @ViewBuilder
    private func toolRow<Destination: View>(
        _ icon: String,
        _ color: Color,
        _ title: LocalizedStringKey,
        _ subtitle: LocalizedStringKey,
        @ViewBuilder destination: () -> Destination
    ) -> some View {
        NavigationLink {
            destination()
        } label: {
            HStack(spacing: 12) {
                TintIcon(systemImage: icon, color: color)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .foregroundStyle(.primary)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.vertical, 2)
        }
    }
}
