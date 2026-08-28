//
//  CertInspectToolView.swift
//  Orange Cloud
//
//  SSL 证书检查（无需 CF 账号）。
//

import SwiftUI

struct CertInspectToolView: View {

    @State private var vm = CertInspectViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                VStack(spacing: 12) {
                    TextField("域名，如 example.com", text: $vm.host)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.go)
                        .onSubmit { Task { await vm.run() } }
                    Button {
                        Task { await vm.run() }
                    } label: {
                        Text("检查").frame(maxWidth: .infinity)
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
                    ToolNotice(systemImage: "exclamationmark.triangle", title: "检查失败", message: error, tint: .orange)
                } else if let info = vm.info {
                    ToolKVSection(title: "证书", rows: rows(info))

                    if !info.sanDNSNames.isEmpty {
                        ToolResultIsland(title: "备用名称（SAN）") {
                            ForEach(Array(info.sanDNSNames.enumerated()), id: \.offset) { index, name in
                                if index > 0 {
                                    Divider().padding(.leading, OCLayout.islandPadding)
                                }
                                Text(name)
                                    .font(.callout.monospaced())
                                    .textSelection(.enabled)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(.horizontal, OCLayout.islandPadding)
                                    .padding(.vertical, 8)
                            }
                        }
                    }

                    if !info.chainSubjects.isEmpty {
                        ToolResultIsland(title: "证书链") {
                            ForEach(Array(info.chainSubjects.enumerated()), id: \.offset) { index, subject in
                                if index > 0 {
                                    Divider().padding(.leading, OCLayout.islandPadding)
                                }
                                HStack(spacing: 8) {
                                    Text("\(index)")
                                        .font(.caption.monospaced())
                                        .foregroundStyle(.secondary)
                                    Text(subject)
                                        .font(.callout)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                }
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
        .navigationTitle("SSL 证书检查")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func rows(_ info: CertInfo) -> [ToolKVRow] {
        var rows: [ToolKVRow] = [
            ToolKVRow("主题 CN", info.subjectCN, mono: false),
            ToolKVRow("签发者", info.issuerCN, mono: false),
            ToolKVRow("生效", ToolFormat.date(info.notBefore)),
            ToolKVRow("到期", ToolFormat.date(info.notAfter)),
        ]
        if let days = info.daysRemaining {
            rows.append(ToolKVRow("剩余天数", info.isExpired ? String(localized: "已过期") : String(localized: "\(days) 天")))
        }
        if let bits = info.publicKeyBits { rows.append(ToolKVRow("公钥位数", String(localized: "\(bits) 位"))) }
        if let serial = info.serialHex { rows.append(ToolKVRow("序列号", serial)) }
        return rows
    }
}
