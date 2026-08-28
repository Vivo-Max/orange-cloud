//
//  DNSLookupToolView.swift
//  Orange Cloud
//
//  DNS 查询（DoH over 1.1.1.1，无需 CF 账号）。
//

import SwiftUI

struct DNSLookupToolView: View {

    @State private var vm = DNSLookupViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                VStack(spacing: 12) {
                    TextField("域名，如 example.com", text: $vm.name)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.search)
                        .onSubmit { Task { await vm.run() } }

                    HStack {
                        Text("记录类型")
                            .foregroundStyle(.secondary)
                        Spacer()
                        Picker("记录类型", selection: $vm.type) {
                            ForEach(DNSQueryType.allCases) { Text($0.rawValue).tag($0) }
                        }
                        .pickerStyle(.menu)
                        .tint(.ocOrange)
                    }

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
                } else if vm.hasRun && vm.results.isEmpty {
                    ToolNotice(systemImage: "questionmark.circle", title: "无记录", message: String(localized: "该名称下没有此类型的记录。"))
                } else if !vm.results.isEmpty {
                    ToolResultIsland(title: "记录") {
                        ForEach(Array(vm.results.enumerated()), id: \.element.id) { index, rec in
                            if index > 0 {
                                Divider().padding(.leading, OCLayout.islandPadding)
                            }
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(rec.typeName)
                                        .font(.caption.bold())
                                        .foregroundStyle(Color.ocOrangeText)
                                    Spacer()
                                    Text("TTL \(rec.ttl)")
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                                Text(rec.value)
                                    .font(.callout.monospaced())
                                    .textSelection(.enabled)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding(.horizontal, OCLayout.islandPadding)
                            .padding(.vertical, 9)
                        }
                    }
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .navigationTitle("DNS 查询")
        .navigationBarTitleDisplayMode(.inline)
    }
}
