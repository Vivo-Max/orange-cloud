//
//  TintIcon.swift
//  Orange Cloud
//
//  iOS 26 风格彩色圆底图标：12% 透明色圆形 + 同色 SF Symbol（设计稿 TintIcon）。
//

import SwiftUI

struct TintIcon: View {

    let systemImage: String
    let color: Color
    var size: CGFloat = 30

    var body: some View {
        Image(systemName: systemImage)
            .font(.system(size: size * 0.5, weight: .medium))
            .foregroundStyle(color)
            .frame(width: size, height: size)
            .background(color.opacity(0.12), in: Circle())
            // 始终作为行内装饰图标出现（旁边有文字标签），对读屏隐藏避免冗余
            .accessibilityHidden(true)
    }
}

#Preview {
    HStack(spacing: 12) {
        TintIcon(systemImage: "network", color: .ocOrange)
        TintIcon(systemImage: "checkmark.shield", color: .green)
        TintIcon(systemImage: "chart.bar", color: .blue)
        TintIcon(systemImage: "gauge", color: .indigo)
    }
    .padding()
}
