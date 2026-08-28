//
//  CFTraceToolView.swift
//  Orange Cloud
//
//  CF 数据中心 trace（/cdn-cgi/trace）。默认探测本机出口的 colo。
//

import SwiftUI

struct CFTraceToolView: View {

    @State private var vm = CFTraceViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                VStack(spacing: 12) {
                    TextField("主机，如 1.1.1.1 或 example.com", text: $vm.host)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.go)
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
                } else if let result = vm.result {
                    let summary = summaryRows(result)
                    if !summary.isEmpty {
                        ToolKVSection(title: "概要", rows: summary)
                    }
                    ToolResultIsland(title: "全部字段") {
                        ForEach(Array(result.fields.enumerated()), id: \.element.id) { index, field in
                            if index > 0 {
                                Divider().padding(.leading, OCLayout.islandPadding)
                            }
                            ToolKV(key: LocalizedStringKey(field.key), value: field.value)
                        }
                    }
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .navigationTitle("CF 数据中心")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if vm.result == nil && vm.error == nil { await vm.run() }
        }
    }

    private func summaryRows(_ r: CFTraceResult) -> [ToolKVRow] {
        var rows: [ToolKVRow] = []
        if let colo = r.colo { rows.append(ToolKVRow("Colo", colo)) }
        if let loc = r.loc { rows.append(ToolKVRow("国家/地区", loc)) }
        if let ip = r.ip { rows.append(ToolKVRow("你的 IP", ip)) }
        if let tls = r.tls { rows.append(ToolKVRow("TLS", tls)) }
        return rows
    }
}
