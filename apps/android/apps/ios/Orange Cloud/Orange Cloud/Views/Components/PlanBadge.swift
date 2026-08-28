//
//  PlanBadge.swift
//  Orange Cloud
//
//  套餐徽章：Free 灰 / Pro 蓝 / Business 橙 / Enterprise 金（设计稿 PlanBadge）。
//

import SwiftUI

struct PlanBadge: View {

    let planName: String

    private var shortName: String {
        // "Free Website" / "Pro Plan" 等取首词
        planName.components(separatedBy: " ").first ?? planName
    }

    private var tone: Color {
        switch shortName.lowercased() {
        case "pro":        .blue
        case "business":   .ocOrange
        case "enterprise": .ocGold
        default:           .gray
        }
    }

    var body: some View {
        Text(shortName)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(tone)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(tone.opacity(0.14), in: Capsule())
    }
}

#Preview {
    HStack {
        PlanBadge(planName: "Free Website")
        PlanBadge(planName: "Pro")
        PlanBadge(planName: "Business")
        PlanBadge(planName: "Enterprise")
    }
    .padding()
}
