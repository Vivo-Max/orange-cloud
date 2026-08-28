//
//  GeoIPToolView.swift
//  Orange Cloud
//
//  IP 归属地（ipwho.is，无需 CF 账号）。留空查本机出口 IP。
//

import SwiftUI

struct GeoIPToolView: View {

    @State private var vm = GeoIPViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: OCLayout.islandGap) {
                VStack(spacing: 12) {
                    TextField("IP（留空查本机）", text: $vm.ip)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.numbersAndPunctuation)
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
                } else if let result = vm.result {
                    ToolKVSection(title: "归属地", rows: rows(result))
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .background { SkyBackground() }
        .navigationTitle("IP 归属地")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if !vm.hasRun { await vm.run() }
        }
    }

    private func rows(_ r: GeoIPResult) -> [ToolKVRow] {
        var rows: [ToolKVRow] = []
        if let ip = r.ip { rows.append(ToolKVRow("IP", ip)) }
        let place = [r.city, r.region, r.country].compactMap { $0 }.joined(separator: ", ")
        if !place.isEmpty { rows.append(ToolKVRow("位置", place, mono: false)) }
        if let cc = r.countryCode { rows.append(ToolKVRow("国家代码", cc)) }
        if let org = r.connection?.org ?? r.connection?.isp { rows.append(ToolKVRow("运营商", org, mono: false)) }
        if let asn = r.connection?.asn { rows.append(ToolKVRow("ASN", "AS\(asn)")) }
        if let tz = r.timezone?.id { rows.append(ToolKVRow("时区", tz)) }
        if let lat = r.latitude, let lon = r.longitude {
            rows.append(ToolKVRow("经纬度", String(format: "%.4f, %.4f", lat, lon)))
        }
        return rows
    }
}
