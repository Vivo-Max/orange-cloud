//
//  CIDRToolView.swift
//  Orange Cloud
//
//  CIDR 计算器（纯本地，无网络）。
//

import SwiftUI

struct CIDRToolView: View {

    @State private var input = ""
    @State private var result: CIDRResult?
    @State private var invalid = false

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                VStack(spacing: 12) {
                    TextField("192.168.1.0/24 或 2001:db8::/48", text: $input)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .submitLabel(.go)
                        .onSubmit(compute)
                    Button(action: compute) {
                        Text("计算").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ocOrange)
                }
                .padding(OCLayout.islandPadding)
                .glassIsland()

                if invalid {
                    ToolNotice(
                        systemImage: "exclamationmark.triangle",
                        title: "无法解析",
                        message: String(localized: "请输入形如 10.0.0.0/8 或 2001:db8::/48 的 CIDR。"),
                        tint: .orange
                    )
                } else if let result {
                    ToolKVSection(title: "结果", rows: rows(result))
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .navigationTitle("CIDR 计算器")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func compute() {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        let parsed = CIDRCalculator.parse(trimmed)
        result = parsed
        invalid = parsed == nil && !trimmed.isEmpty
    }

    private func rows(_ r: CIDRResult) -> [ToolKVRow] {
        var rows: [ToolKVRow] = [
            ToolKVRow("地址族", r.family, mono: false),
            ToolKVRow("网络地址", r.networkAddress),
            ToolKVRow("前缀长度", "/\(r.prefixLength)"),
        ]
        if let mask = r.netmask { rows.append(ToolKVRow("子网掩码", mask)) }
        rows.append(ToolKVRow("起始地址", r.firstAddress))
        rows.append(ToolKVRow("结束地址", r.lastAddress))
        rows.append(ToolKVRow("地址总数", r.totalCount))
        if let usable = r.usableHosts { rows.append(ToolKVRow("可用主机", usable)) }
        return rows
    }
}
