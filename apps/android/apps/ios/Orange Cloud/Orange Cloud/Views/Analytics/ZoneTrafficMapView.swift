//
//  ZoneTrafficMapView.swift
//  Orange Cloud
//
//  全球流量地图（看板级 Pro 视觉）：把 GraphQL countryMap 数据落到世界地图上，
//  按国家/地区打气泡——圆点大小映射请求量，威胁占比高的国家转红。
//  挂在域名详情页分析区，受 ProFeature.trafficMap 闸门控制（24h 免费、地图整体 Pro）。
//
//  基线 iOS 17：MapKit SwiftUI `Map` + `Annotation` 自 iOS 17 起可用，无需 availability 守卫。
//

import SwiftUI
import MapKit

struct ZoneTrafficMapCard: View {

    let viewModel: ZoneAnalyticsViewModel

    @Environment(EntitlementStore.self) private var entitlements
    @State private var paywallPresented = false
    @State private var camera: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 25, longitude: 0),
            span: MKCoordinateSpan(latitudeDelta: 120, longitudeDelta: 200)
        )
    )

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header

            if entitlements.isPro {
                proContent
            } else {
                lockedTeaser
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
        .sheet(isPresented: $paywallPresented) {
            PaywallView(feature: .trafficMap)
        }
        .task {
            // 地理数据仅 Pro 拉取，免费层只展示锁定预告
            if entitlements.isPro { await viewModel.loadCountries() }
        }
        .onChange(of: viewModel.selectedRange) {
            if entitlements.isPro { Task { await viewModel.loadCountries() } }
        }
    }

    // MARK: - 头部

    private var header: some View {
        HStack {
            Text("全球流量分布")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)
            Spacer()
            if !entitlements.isPro {
                ProBadge()
            }
        }
    }

    // MARK: - Pro 内容

    @ViewBuilder
    private var proContent: some View {
        if viewModel.isLoadingCountries && viewModel.countries.isEmpty {
            mapPlaceholder {
                ProgressView()
            }
        } else if bubbles.isEmpty {
            mapPlaceholder {
                VStack(spacing: 6) {
                    Image(systemName: "globe.americas")
                        .font(.title2)
                        .foregroundStyle(.tertiary)
                    Text("暂无地理数据")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
        } else {
            map
            legend
        }
    }

    private var map: some View {
        Map(position: $camera, interactionModes: [.pan, .zoom]) {
            ForEach(bubbles) { bubble in
                // 标题留空避免 200+ 国家名在图上堆叠；无障碍标签挂在圆点本身
                Annotation("", coordinate: bubble.coordinate) {
                    Circle()
                        .fill(bubble.color.opacity(0.55))
                        .overlay(Circle().strokeBorder(bubble.color, lineWidth: 1.5))
                        .frame(width: bubble.diameter, height: bubble.diameter)
                        .accessibilityLabel(bubble.country.displayName)
                        .accessibilityValue(Text("\(bubble.country.requests) 次请求"))
                }
            }
        }
        .mapStyle(.standard(elevation: .flat, pointsOfInterest: .excludingAll))
        .frame(height: 240)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .strokeBorder(Color(.separator).opacity(0.4), lineWidth: 0.5)
        )
    }

    /// 地图下方 Top 5 国家/地区列表（请求量降序，威胁高亮红点）
    private var legend: some View {
        VStack(spacing: 8) {
            ForEach(viewModel.countries.prefix(5)) { country in
                HStack(spacing: 10) {
                    Circle()
                        .fill(country.isHighThreat ? Color.red : Color.ocOrange)
                        .frame(width: 8, height: 8)
                    Text(country.displayName)
                        .font(.subheadline)
                        .lineLimit(1)
                    Spacer()
                    if country.threats > 0 {
                        Label("\(country.threats.formatted(.number.notation(.compactName)))",
                              systemImage: "exclamationmark.shield")
                            .font(.caption2)
                            .foregroundStyle(.red)
                            .labelStyle(.titleAndIcon)
                    }
                    Text(country.requests.formatted(.number.notation(.compactName)))
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.top, 2)
    }

    private func mapPlaceholder<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14)
                .fill(.quaternary.opacity(0.4))
            content()
        }
        .frame(height: 240)
    }

    // MARK: - 锁定态（免费层看板预告）

    private var lockedTeaser: some View {
        Button {
            paywallPresented = true
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(
                        LinearGradient(
                            colors: [Color.ocOrange.opacity(0.18), Color.ocOrange.opacity(0.04)],
                            startPoint: .topLeading, endPoint: .bottomTrailing
                        )
                    )
                VStack(spacing: 8) {
                    Image(systemName: "globe.americas.fill")
                        .font(.system(size: 34))
                        .foregroundStyle(Color.ocOrange)
                    Text("按国家/地区查看请求与威胁的全球分布")
                        .font(.footnote)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                    Text("升级 Pro 解锁")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.ocOrangeText)
                }
                .padding()
            }
            .frame(height: 200)
        }
        .buttonStyle(.plain)
    }

    // MARK: - 气泡

    private var bubbles: [TrafficBubble] {
        let maxRequests = viewModel.countries.map(\.requests).max() ?? 1
        return viewModel.countries.compactMap { country in
            guard let geo = CountryGeo.coordinate(for: country.countryCode) else { return nil }
            return TrafficBubble(
                country: country,
                coordinate: CLLocationCoordinate2D(latitude: geo.latitude, longitude: geo.longitude),
                maxRequests: maxRequests
            )
        }
    }
}

// MARK: - 单个气泡

private struct TrafficBubble: Identifiable {

    let country: CountryTraffic
    let coordinate: CLLocationCoordinate2D
    let maxRequests: Int

    var id: String { country.countryCode }

    /// 面积感知缩放：直径 ∝ √(请求占比)，避免大国吞掉整张图
    var diameter: CGFloat {
        let ratio = maxRequests > 0 ? Double(country.requests) / Double(maxRequests) : 0
        return 9 + (40 - 9) * CGFloat(ratio.squareRoot())
    }

    var color: Color { country.isHighThreat ? .red : .ocOrange }
}

extension CountryTraffic {
    /// 威胁占比超过 1% 视为高威胁来源（地图/图例转红）
    var isHighThreat: Bool {
        requests > 0 && Double(threats) / Double(requests) >= 0.01
    }
}
