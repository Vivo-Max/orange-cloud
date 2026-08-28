//
//  DeveloperHubView.swift
//  Orange Cloud
//
//  「开发者平台」Tab：把 Workers 与各开发者平台资源收进一个分类入口，
//  对齐 Cloudflare 的产品分组（计算 / 数据与消息 / AI）。Workers 不再单独占一个 Tab。
//

import SwiftUI
import SwiftData

struct DeveloperHubView: View {

    let session: SessionStore

    var body: some View {
        // NavigationStack 常驻，账号切换只重建栈内内容（.id 在栈内、挂在 hubList 上）。
        // **不要把 .id(账号) 挪回栈外或 MainTabView**：selectedAccount 可能在本 Tab 可见时
        // 才翻转，重建可见 NavigationStack 在 iOS 17.0.x 导航栏硬断言必崩（1.8.2(24) 复发根因）。
        NavigationStack {
            hubList
                .id(session.selectedAccount?.id)
        }
    }

    private var hubList: some View {
            List {
                // List 内由系统提供 NavigationLink chevron，勿再传 showsChevron（否则双箭头）。
                Section("计算") {
                    // Workers 免费（按 scope 门控，不走 Pro）。
                    // 用值式导航：Workers 列表点击行还要继续 push 到详情，必须走单一宿主栈 +
                    // 栈根 navdest（见下方 .navigationDestination），eager 形态的目的页内部 push 会失灵/错乱。
                    PermissionGatedValueLink(
                        label: "Workers", systemImage: "bolt.fill",
                        requiredScope: "workers-scripts.read",
                        value: DevHubRoute.workers
                    )
                    // Pages 链路（列表 → 项目详情 → 域名/部署/构建配置）同 Workers：
                    // 列表与详情都要继续 push，入口必须值式（判据见 PermissionGatedValueLink 注释）
                    ProGatedValueLink(
                        label: "Cloudflare Pages", systemImage: "doc.richtext",
                        requiredScope: "page.read", feature: .pages,
                        value: DevHubRoute.pages
                    )
                }
                .glassRow()

                // 以下入口的目的页均为叶子（内部只开 sheet、不再 push），List 行内 eager
                // 形态实测安全，保留；若哪个目的页日后加了内层 push，必须改值式。
                Section("数据与消息") {
                    ProGatedNavigationLink(
                        label: "Queues", systemImage: "tray.2",
                        requiredScope: "queues.read", feature: .queues
                    ) { QueuesView(session: session) }
                    ProGatedNavigationLink(
                        label: "Durable Objects", systemImage: "cube.transparent",
                        requiredScope: "workers-scripts.read", feature: .durableObjects
                    ) { DurableObjectsView(session: session) }
                    ProGatedNavigationLink(
                        label: "Hyperdrive", systemImage: "bolt.horizontal.circle",
                        requiredScope: "query-cache.read", feature: .hyperdrive
                    ) { HyperdriveView(session: session) }
                }
                .glassRow()

                Section("AI") {
                    ProGatedNavigationLink(
                        label: "Workers AI", systemImage: "brain",
                        requiredScope: "ai.read", feature: .workersAI
                    ) { WorkersAIView(session: session) }
                    ProGatedNavigationLink(
                        label: "AI Gateway", systemImage: "brain.head.profile",
                        requiredScope: "aig.read", feature: .aiGateway
                    ) { AIGatewayView(session: session) }
                }
                .glassRow()
            }
            .daybreakList()
            .background { SkyBackground() }
            .navigationTitle("开发者平台")
            .navigationBarTitleDisplayMode(.inline)
            // Workers 列表与其详情都挂在宿主栈根：列表入口走 DevHubRoute、行点击走 CachedWorkerScript，
            // 单一栈、不嵌套，逐级 push 正常（详见 WorkerListView 注释）。
            .navigationDestination(for: DevHubRoute.self) { route in
                switch route {
                case .workers: WorkerListView(session: session)
                case .pages: PagesProjectListView(session: session)
                }
            }
            .navigationDestination(for: CachedWorkerScript.self) { script in
                WorkerDetailView(script: script, session: session)
            }
            .navigationDestination(for: PagesProjectRoute.self) { route in
                PagesProjectDetailView(project: route.project, session: session)
            }
    }
}

/// 开发者平台里「目的页自身还要继续 push」的入口路由（走宿主栈根 navdest）
enum DevHubRoute: Hashable {
    case workers
    case pages
}
