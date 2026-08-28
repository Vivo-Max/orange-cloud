//
//  HTTPProbeToolView.swift
//  Orange Cloud
//
//  HTTP 请求器（仅 https）。看状态、响应头、耗时、正文预览。
//

import SwiftUI

struct HTTPProbeToolView: View {

    @State private var vm = HTTPProbeViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                VStack(spacing: 12) {
                    Picker("方法", selection: $vm.method) {
                        ForEach(vm.methods, id: \.self) { Text($0).tag($0) }
                    }
                    .pickerStyle(.segmented)

                    TextField("https://example.com", text: $vm.urlString)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.go)
                        .onSubmit { Task { await vm.run() } }

                    Button {
                        Task { await vm.run() }
                    } label: {
                        Text("发送请求").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ocOrange)
                    .disabled(vm.isLoading)

                    Text("仅支持 https。")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(OCLayout.islandPadding)
                .glassIsland()

                if vm.isLoading {
                    ProgressView().padding()
                } else if let error = vm.error {
                    ToolNotice(systemImage: "exclamationmark.triangle", title: "请求失败", message: error, tint: .orange)
                } else if let result = vm.result {
                    ToolKVSection(title: "概要", rows: summaryRows(result))
                    if !result.headers.isEmpty {
                        ToolResultIsland(title: "响应头") {
                            ForEach(Array(result.headers.enumerated()), id: \.element.id) { index, header in
                                if index > 0 {
                                    Divider().padding(.leading, OCLayout.islandPadding)
                                }
                                ToolKV(key: LocalizedStringKey(header.name), value: header.value)
                            }
                        }
                    }
                    if !result.bodyPreview.isEmpty {
                        ToolResultIsland(title: "正文预览") {
                            Text(result.bodyPreview)
                                .font(.caption.monospaced())
                                .textSelection(.enabled)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(OCLayout.islandPadding)
                        }
                    }
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .navigationTitle("HTTP 请求")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func summaryRows(_ r: HTTPProbeResult) -> [ToolKVRow] {
        [
            ToolKVRow("状态", "\(r.statusCode) \(r.statusText)"),
            ToolKVRow("耗时", "\(r.durationMS) ms"),
            ToolKVRow("大小", Int64(r.bodyByteCount).ocBytesBinary),
            ToolKVRow("最终地址", r.finalURL),
        ]
    }
}
