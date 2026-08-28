//
//  WhoisToolView.swift
//  Orange Cloud
//
//  WHOIS（走 RDAP，无需 CF 账号）。
//

import SwiftUI

struct WhoisToolView: View {

    @State private var vm = WhoisViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                VStack(spacing: 12) {
                    TextField("域名，如 example.com", text: $vm.domain)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.search)
                        .onSubmit { Task { await vm.run() } }
                    Button {
                        Task { await vm.run() }
                    } label: {
                        Text("查询").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ocOrange)
                    .disabled(vm.isLoading)
                }
                .padding(OCLayout.islandPadding)
                .glassIsland()

                if vm.isLoading {
                    ProgressView().padding()
                } else if let error = vm.error {
                    ToolNotice(systemImage: "exclamationmark.triangle", title: "查询失败", message: error, tint: .orange)
                } else if let info = vm.info {
                    ToolKVSection(title: "注册信息", rows: rows(info))

                    if !info.nameservers.isEmpty {
                        ToolResultIsland(title: "名称服务器") {
                            ForEach(Array(info.nameservers.enumerated()), id: \.offset) { index, ns in
                                if index > 0 {
                                    Divider().padding(.leading, OCLayout.islandPadding)
                                }
                                Text(ns.lowercased())
                                    .font(.callout.monospaced())
                                    .textSelection(.enabled)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(.horizontal, OCLayout.islandPadding)
                                    .padding(.vertical, 8)
                            }
                        }
                    }
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .navigationTitle("WHOIS")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func rows(_ info: WhoisInfo) -> [ToolKVRow] {
        var rows: [ToolKVRow] = [ToolKVRow("域名", info.domain)]
        if let registrar = info.registrar { rows.append(ToolKVRow("注册商", registrar, mono: false)) }
        rows.append(ToolKVRow("注册时间", ToolFormat.date(info.created)))
        rows.append(ToolKVRow("更新时间", ToolFormat.date(info.updated)))
        rows.append(ToolKVRow("到期时间", ToolFormat.date(info.expires)))
        if !info.statuses.isEmpty {
            rows.append(ToolKVRow("状态", info.statuses.joined(separator: ", "), mono: false))
        }
        return rows
    }
}
