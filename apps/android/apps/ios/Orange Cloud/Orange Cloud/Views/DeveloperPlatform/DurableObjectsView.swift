//
//  DurableObjectsView.swift
//  Orange Cloud
//
//  Durable Objects 命名空间（命名空间本身只读：由 Worker 迁移声明，API 不支持增删改）。
//  点命名空间可下钻浏览其对象实例（只读，游标分页）。account 级，workers-scripts.read。
//

import SwiftUI

struct DurableObjectsView: View {

    let session: SessionStore
    @State private var vm: DurableObjectsViewModel?
    @State private var detailTarget: DurableObjectNamespace?

    var body: some View {
        Group {
            if let vm { content(vm) } else { ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity) }
        }
        .background { SkyBackground() }
        .navigationTitle("Durable Objects")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $detailTarget) { ns in
            DurableObjectInstancesSheet(session: session, namespace: ns)
        }
        .task {
            await session.ensureAccounts()
            guard vm == nil else { return }
            let model = DurableObjectsViewModel(service: session.durableObjectService, accountId: session.selectedAccount?.id)
            vm = model
            await model.load()
        }
    }

    @ViewBuilder
    private func content(_ vm: DurableObjectsViewModel) -> some View {
        if vm.isLoading && !vm.loaded {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.namespaces.isEmpty {
            ContentUnavailableView {
                Label("没有 Durable Objects", systemImage: "cube.transparent")
            } description: {
                Text(vm.error ?? String(localized: "该账号下还没有 Durable Object 命名空间。命名空间由 Worker 迁移声明。"))
            }
        } else {
            List {
                Section {
                    ForEach(vm.namespaces) { ns in
                        Button { detailTarget = ns } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    HStack(spacing: 6) {
                                        Text(ns.name ?? ns.className ?? ns.id)
                                            .font(.callout.weight(.semibold)).lineLimit(1).foregroundStyle(.primary)
                                        if ns.useSqlite == true {
                                            Text("SQLite").font(.caption2.weight(.semibold))
                                                .foregroundStyle(Color.ocOrangeText)
                                                .padding(.horizontal, 6).padding(.vertical, 2)
                                                .background(Color.ocOrange.opacity(0.14), in: Capsule())
                                        }
                                    }
                                    if let cls = ns.className {
                                        Text(verbatim: "class \(cls)").font(.caption.monospaced()).foregroundStyle(.secondary).lineLimit(1)
                                    }
                                    if let script = ns.script {
                                        Text(script).font(.caption2.monospaced()).foregroundStyle(.tertiary).lineLimit(1)
                                    }
                                }
                                Spacer()
                                Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
                            }
                            .padding(.vertical, 2)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                } footer: {
                    Text("点按浏览对象实例 · 命名空间的增删改请在 Worker 迁移中进行。")
                }
                .glassRow()
            }
            .daybreakList()
            .refreshable { await vm.load() }
        }
    }
}

// MARK: - 对象实例浏览 sheet（只读，游标分页）

private struct DurableObjectInstancesSheet: View {
    let session: SessionStore
    let namespace: DurableObjectNamespace

    @Environment(\.dismiss) private var dismiss
    @State private var vm: DurableObjectInstancesViewModel?

    var body: some View {
        NavigationStack {
            Group {
                if let vm { content(vm) } else { ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity) }
            }
            .background { SkyBackground() }
            .navigationTitle(namespace.name ?? namespace.className ?? namespace.id)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("完成") { dismiss() } }
            }
            .task {
                guard vm == nil else { return }
                let model = DurableObjectInstancesViewModel(
                    service: session.durableObjectService,
                    accountId: session.selectedAccount?.id,
                    namespaceId: namespace.id
                )
                vm = model
                await model.load()
                await model.loadMemory()
            }
        }
    }

    @ViewBuilder
    private func content(_ vm: DurableObjectInstancesViewModel) -> some View {
        if vm.isLoading && !vm.loaded {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.instances.isEmpty {
            ContentUnavailableView {
                Label("没有对象", systemImage: "cube")
            } description: {
                Text(vm.error ?? String(localized: "该命名空间下还没有对象实例。对象在首次被访问时自动创建。"))
            }
        } else {
            List {
                // 内存按 isolate 计，不是按单个对象——口径要写清楚，否则会被误读
                if let memory = vm.memory {
                    Section {
                        LabeledContent("P50", value: Int64(memory.p50).ocBytes)
                        LabeledContent("P90", value: Int64(memory.p90).ocBytes)
                        LabeledContent("P99", value: Int64(memory.p99).ocBytes)
                        LabeledContent("P999", value: Int64(memory.p999).ocBytes)
                    } header: {
                        Text("内存用量（24 小时）")
                    } footer: {
                        Text("V8 isolate 的内存，不是单个对象的。一个 isolate 可能同时承载同类的多个对象与外围 Worker 代码，每个 isolate 上限 128 MB。")
                    }
                    .glassRow()
                }

                Section {
                    ForEach(vm.instances) { obj in
                        HStack {
                            Text(obj.id).font(.caption.monospaced()).lineLimit(1).truncationMode(.middle)
                            Spacer()
                            if obj.hasStoredData == true {
                                Text("有数据").font(.caption2.weight(.semibold))
                                    .foregroundStyle(Color.ocOrangeText)
                                    .padding(.horizontal, 6).padding(.vertical, 2)
                                    .background(Color.ocOrange.opacity(0.14), in: Capsule())
                            }
                        }
                        .padding(.vertical, 2)
                    }
                    if vm.hasMore {
                        Button {
                            Task { await vm.loadMore() }
                        } label: {
                            HStack {
                                Spacer()
                                if vm.isLoading { ProgressView() } else { Text("加载更多") }
                                Spacer()
                            }
                        }
                        .disabled(vm.isLoading)
                    }
                } header: {
                    Text("\(vm.instances.count) 个对象")
                } footer: {
                    Text("对象实例只读：展示对象 ID 与是否已写入存储。")
                }
                .glassRow()
            }
            .daybreakList()
        }
    }
}
