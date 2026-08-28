//
//  ProxiedBadge.swift
//  Orange Cloud
//

import SwiftUI

/// Cloudflare 代理状态徽标：橙色云朵 = 已代理，灰色 = 仅 DNS
struct ProxiedBadge: View {
    let proxied: Bool

    var body: some View {
        Image(systemName: proxied ? "cloud.fill" : "cloud")
            .foregroundStyle(proxied ? Color.ocOrange : Color.secondary)
            .contentTransition(.symbolEffect(.replace))
            .accessibilityLabel(proxied ? String(localized: "已代理") : String(localized: "仅 DNS"))
    }
}
