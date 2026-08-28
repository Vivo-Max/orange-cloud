//
//  ZoneAvatar.swift
//  Orange Cloud
//
//  域名字母头像：哈希色相渐变圆形 + 白色首字母（设计稿 ZoneAvatar）。
//

import SwiftUI

struct ZoneAvatar: View {

    let domain: String
    var size: CGFloat = 36

    private var initial: String {
        let label = domain
            .replacingOccurrences(of: "www.", with: "")
            .replacingOccurrences(of: "https://", with: "")
        return String(label.first ?? "?").uppercased()
    }

    private var base: Color { AvatarPalette.color(for: domain) }

    var body: some View {
        Text(initial)
            .font(.system(size: size * 0.44, weight: .bold, design: .rounded))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(
                LinearGradient(
                    colors: [base, base.mixed(with: .black, by: 0.28)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ),
                in: Circle()
            )
            .overlay(
                Circle()
                    .strokeBorder(.white.opacity(0.32), lineWidth: 0.5)
                    .blendMode(.plusLighter)
            )
            .shadow(color: .black.opacity(0.18), radius: 1.5, y: 1)
            // 首字母头像是装饰，域名文字总在旁边，对读屏隐藏
            .accessibilityHidden(true)
    }
}

#Preview {
    HStack(spacing: 12) {
        ZoneAvatar(domain: "jenny.codes")
        ZoneAvatar(domain: "northwind.app", size: 52)
        ZoneAvatar(domain: "lumen.store", size: 30)
    }
    .padding()
}
