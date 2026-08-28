//
//  WidgetDaybreak.swift
//  OrangeCloudWidgets
//
//  「晨昏」语言的 Widget 端实现。
//  - WidgetSky：天空做 containerBackground，相位取 timeline entry 的时刻
//  - HorizonScene：地平线弧 + 天体（天窗小尺寸的签名元素；传入比值即变成额度仪表）
//  - RidgeScene：24h 数据长成地平线下的山脊，太阳走到当前时刻，山脊在脚下点灯
//
//  横轴口径：widget 场景统一用 24h 表盘（0:00 → 24:00），山脊各小时样本
//  落在自己的钟点位置上，太阳脚下那座峰就是「此刻」。
//

import SwiftUI

// MARK: - 时刻 → 表盘位置

nonisolated enum WidgetDial {

    /// 当前时刻在 24h 表盘上的进度（夹在 0.02–0.98，天体不贴边）
    static func clockProgress(_ date: Date) -> Double {
        let calendar = Calendar.current
        let minutes = Double(calendar.component(.hour, from: date) * 60 + calendar.component(.minute, from: date))
        return min(max(minutes / 1440, 0.02), 0.98)
    }

    /// 二次贝塞尔曲线上的点
    static func bezier(_ t: CGFloat, _ start: CGPoint, _ control: CGPoint, _ end: CGPoint) -> CGPoint {
        let mt = 1 - t
        return CGPoint(
            x: mt * mt * start.x + 2 * mt * t * control.x + t * t * end.x,
            y: mt * mt * start.y + 2 * mt * t * control.y + t * t * end.y
        )
    }

    /// 折线 → 中点二次曲线平滑路径
    static func smoothPath(_ points: [CGPoint]) -> Path {
        var path = Path()
        guard let first = points.first else { return path }
        path.move(to: first)
        guard points.count > 2 else {
            points.dropFirst().forEach { path.addLine(to: $0) }
            return path
        }
        for index in 1..<(points.count - 1) {
            let mid = CGPoint(
                x: (points[index].x + points[index + 1].x) / 2,
                y: (points[index].y + points[index + 1].y) / 2
            )
            path.addQuadCurve(to: mid, control: points[index])
        }
        path.addLine(to: points[points.count - 1])
        return path
    }
}

// MARK: - 天空画布

/// Widget 的天空背景：相位由 entry 时刻 + 外观模式决定（亮 = 昼，暗 = 夜）
struct WidgetSky: View {

    @Environment(\.colorScheme) private var colorScheme
    let date: Date

    var body: some View {
        let phase = SkyPhase.current(colorScheme: colorScheme, hour: Calendar.current.component(.hour, from: date))
        ZStack {
            LinearGradient(colors: phase.body, startPoint: .top, endPoint: .bottom)
            RadialGradient(
                colors: [phase.glow, .clear],
                center: UnitPoint(x: 0.5, y: -0.2),
                startRadius: 0,
                endRadius: 320
            )
        }
    }
}

// MARK: - 地平线弧（天窗 / 额度仪表）

/// 地平线弧 + 天体。
/// - `gaugeRatio == nil`：时间模式，天体按时刻走位（昼为橙日，夜为白月）
/// - `gaugeRatio != nil`：额度仪表，天体走到用量百分比处，已走过的弧段实线点亮
struct HorizonScene: View {

    @Environment(\.colorScheme) private var colorScheme
    var date: Date
    var gaugeRatio: Double? = nil
    var gaugeTint: Color = .ocOrange

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let height = geo.size.height
            let start = CGPoint(x: -4, y: height - 3)
            let end = CGPoint(x: width + 4, y: height - 3)
            let control = CGPoint(x: width / 2, y: height * 0.08)
            let t = CGFloat(gaugeRatio.map { min(max($0, 0.02), 0.98) } ?? WidgetDial.clockProgress(date))
            let dot = WidgetDial.bezier(t, start, control, end)
            let isGauge = gaugeRatio != nil
            let bodyColor: Color = isGauge ? gaugeTint : (colorScheme == .dark ? Color(red: 0.93, green: 0.93, blue: 0.98) : .ocOrange)
            let trackColor: Color = isGauge
                ? gaugeTint.opacity(0.30)
                : (colorScheme == .dark ? Color.white.opacity(0.16) : Color.ocOrange.opacity(0.35))
            let arc = Path { path in
                path.move(to: start)
                path.addQuadCurve(to: end, control: control)
            }

            ZStack {
                arc.stroke(trackColor, style: StrokeStyle(lineWidth: 1, dash: [3, 4]))
                if isGauge {
                    arc.trimmedPath(from: 0, to: t)
                        .stroke(gaugeTint, style: StrokeStyle(lineWidth: 1.5, lineCap: .round))
                }
                Circle()
                    .fill(bodyColor)
                    .frame(width: 7, height: 7)
                    .shadow(color: bodyColor.opacity(0.6), radius: 5)
                    .position(dot)
            }
        }
        .accessibilityHidden(true)
    }
}

// MARK: - 山脊场景

/// 24h 数据地形：series 末位为当前小时，各样本落在 24h 表盘的钟点位置上。
/// 山脊轮廓线是城市灯火（品牌橙），太阳/月亮在弧上走到当前时刻，脚下的数据点亮灯。
struct RidgeScene: View {

    @Environment(\.colorScheme) private var colorScheme
    var series: [Int]
    var date: Date

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            let points = ridgePoints(in: size)
            if points.count > 1 {
                let crest = WidgetDial.smoothPath(points)
                let t = CGFloat(WidgetDial.clockProgress(date))
                let arcStart = CGPoint(x: -4, y: size.height * 0.52)
                let arcEnd = CGPoint(x: size.width + 4, y: size.height * 0.52)
                let arcControl = CGPoint(x: size.width / 2, y: size.height * 0.10)
                let sun = WidgetDial.bezier(t, arcStart, arcControl, arcEnd)
                let lamp = nearestPoint(to: sun.x, in: points)
                let sunColor: Color = colorScheme == .dark ? Color(red: 0.93, green: 0.93, blue: 0.98) : .ocOrange

                ZStack {
                    Path { path in
                        path.move(to: arcStart)
                        path.addQuadCurve(to: arcEnd, control: arcControl)
                    }
                    .stroke(
                        colorScheme == .dark ? Color.white.opacity(0.16) : Color.ocOrange.opacity(0.35),
                        style: StrokeStyle(lineWidth: 1, dash: [3, 4])
                    )
                    Circle()
                        .fill(sunColor)
                        .frame(width: 7, height: 7)
                        .shadow(color: sunColor.opacity(0.6), radius: 5)
                        .position(sun)

                    fillPath(crest: crest, points: points, size: size)
                        .fill(colorScheme == .dark ? Color.ocOrange.opacity(0.10) : Color(red: 0.34, green: 0.24, blue: 0.15).opacity(0.10))
                    crest.stroke(
                        Color.ocOrange.opacity(colorScheme == .dark ? 0.85 : 0.80),
                        style: StrokeStyle(lineWidth: 1.6, lineCap: .round)
                    )

                    Circle()
                        .fill(Color.ocOrange)
                        .frame(width: 4.5, height: 4.5)
                        .shadow(color: Color.ocOrange.opacity(0.7), radius: 4)
                        .position(lamp)
                }
            }
        }
        .accessibilityHidden(true)
    }

    /// 样本 → 钟点横轴坐标（含左右出血点，让山体延伸出卡片边缘）
    private func ridgePoints(in size: CGSize) -> [CGPoint] {
        guard series.count > 1 else { return [] }
        let hour = Calendar.current.component(.hour, from: date)
        let maxValue = series.max() ?? 1
        let minValue = series.min() ?? 0
        let span = CGFloat(max(maxValue - minValue, 1))
        let yTop = size.height * 0.36
        let yBottom = size.height * 0.92

        var points: [CGPoint] = series.enumerated().map { index, value in
            let sampleHour = ((hour - (series.count - 1 - index)) % 24 + 24) % 24
            let x = (CGFloat(sampleHour) + 0.5) / 24 * size.width
            let y = yBottom - CGFloat(value - minValue) / span * (yBottom - yTop)
            return CGPoint(x: x, y: y)
        }
        points.sort { $0.x < $1.x }
        if let first = points.first, let last = points.last {
            points.insert(CGPoint(x: -8, y: first.y), at: 0)
            points.append(CGPoint(x: size.width + 8, y: last.y))
        }
        return points
    }

    private func fillPath(crest: Path, points: [CGPoint], size: CGSize) -> Path {
        var path = crest
        guard let last = points.last, let first = points.first else { return path }
        path.addLine(to: CGPoint(x: last.x, y: size.height + 2))
        path.addLine(to: CGPoint(x: first.x, y: size.height + 2))
        path.closeSubpath()
        return path
    }

    private func nearestPoint(to x: CGFloat, in points: [CGPoint]) -> CGPoint {
        points.min { abs($0.x - x) < abs($1.x - x) } ?? .zero
    }
}

// MARK: - 公共小件

/// 数据缺失提示（保留天空，提示文字居中）
struct WidgetEmptyHint: View {

    var text: String

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: "cloud")
                .font(.title3)
                .foregroundStyle(.secondary)
            Text(text)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
