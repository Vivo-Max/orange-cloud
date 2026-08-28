//
//  WatchSky.swift
//  Orange Cloud Watch Watch App
//
//  Watch 的晨昏视觉小件：天色背景（复用 Shared 的 SkyPhase 相位表）+ 迷你折线。
//

import SwiftUI

/// 晨昏天空背景（亮 = 昼，暗 = 夜，随时刻走色）
struct WatchSky: View {

    @Environment(\.colorScheme) private var colorScheme
    var date: Date = .now

    var body: some View {
        let phase = SkyPhase.current(colorScheme: colorScheme, hour: Calendar.current.component(.hour, from: date))
        ZStack {
            LinearGradient(colors: phase.body, startPoint: .top, endPoint: .bottom)
            RadialGradient(colors: [phase.glow, .clear], center: UnitPoint(x: 0.5, y: -0.05), startRadius: 0, endRadius: 170)
        }
        .ignoresSafeArea()
    }
}

/// 24h 迷你折线（单色 accent）
struct WatchSparkline: View {

    var series: [Int]
    var tint: Color = .ocOrange

    var body: some View {
        GeometryReader { geo in
            let points = normalized(in: geo.size)
            if points.count > 1 {
                Path { path in
                    path.move(to: points[0])
                    for point in points.dropFirst() { path.addLine(to: point) }
                }
                .stroke(tint, style: StrokeStyle(lineWidth: 1.8, lineCap: .round, lineJoin: .round))
            }
        }
        .accessibilityHidden(true)
    }

    private func normalized(in size: CGSize) -> [CGPoint] {
        guard series.count > 1 else { return [] }
        let maxValue = series.max() ?? 1
        let minValue = series.min() ?? 0
        let span = CGFloat(max(maxValue - minValue, 1))
        let lastIndex = CGFloat(series.count - 1)
        return series.enumerated().map { index, value in
            CGPoint(
                x: CGFloat(index) / lastIndex * size.width,
                y: size.height - CGFloat(value - minValue) / span * size.height
            )
        }
    }
}

extension Int {
    /// 紧凑计数（2.4M / 312K）
    var watchCompact: String { formatted(.number.notation(.compactName)) }
}
