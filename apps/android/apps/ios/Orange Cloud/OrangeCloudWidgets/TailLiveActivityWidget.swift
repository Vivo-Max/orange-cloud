//
//  TailLiveActivityWidget.swift
//  OrangeCloudWidgets
//
//  Workers 实时日志的 Live Activity：锁屏卡片 + Dynamic Island。
//  连接进后台必断，主 App 会把内容标记为 stale；本视图据 `context.isStale`
//  把卡片渲染成「已暂停」灰态，诚实区别于绿色监听中 / 红色已断开。
//

import WidgetKit
import SwiftUI
import ActivityKit

struct TailLiveActivityWidget: Widget {

    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TailActivityAttributes.self) { context in
            // 锁屏 / 横幅
            LockScreenTailView(context: context)
                .activityBackgroundTint(Color.black.opacity(0.6))
                .activitySystemActionForegroundColor(Color.ocOrange)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    HStack(spacing: 6) {
                        Image(systemName: "bolt.fill")
                            .foregroundStyle(context.iconColor)
                        Text(context.attributes.scriptName)
                            .font(.caption.bold())
                            .lineLimit(1)
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.countLabel)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.state.lastLine.isEmpty ? String(localized: "等待事件…") : context.state.lastLine)
                        .font(.caption2.monospaced())
                        .lineLimit(2)
                        .foregroundStyle(.secondary)
                }
            } compactLeading: {
                Image(systemName: "bolt.fill")
                    .foregroundStyle(context.iconColor)
            } compactTrailing: {
                Text("\(context.state.eventCount)")
                    .font(.caption2.monospaced())
                    .foregroundStyle(context.dotColor)
            } minimal: {
                Image(systemName: "bolt.fill")
                    .foregroundStyle(context.iconColor)
            }
        }
    }
}

private struct LockScreenTailView: View {

    let context: ActivityViewContext<TailActivityAttributes>

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Label(context.attributes.scriptName, systemImage: "bolt.fill")
                    .font(.callout.bold())
                    .foregroundStyle(context.iconColor)
                Spacer()
                HStack(spacing: 4) {
                    Circle()
                        .fill(context.dotColor)
                        .frame(width: 6, height: 6)
                    Text(context.countLabel)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Text(context.state.lastLine.isEmpty ? String(localized: "正在监听实时日志…") : context.state.lastLine)
                .font(.caption.monospaced())
                .lineLimit(2)
                .foregroundStyle(context.isStale ? .secondary : .primary)
        }
        .padding()
    }
}

// 三态渲染：stale（后台挂起）灰、连接中绿、断开红
private extension ActivityViewContext where Attributes == TailActivityAttributes {
    var dotColor: Color {
        if isStale { return .gray }
        return state.isConnected ? .green : .red
    }

    var iconColor: Color {
        isStale ? .gray : .ocOrange
    }

    /// 停滞时直说「已暂停」，否则显示累计事件数
    var countLabel: String {
        isStale ? String(localized: "已暂停") : String(localized: "\(state.eventCount) 事件")
    }
}
