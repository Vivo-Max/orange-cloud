//
//  WorkerDeploymentsView.swift
//  Orange Cloud
//
//  Worker 部署历史：列出历次部署，删除历史部署（首项为活跃部署，不可删除）。
//  写操作按 workers-scripts.write 门控。
//

import SwiftUI

struct WorkerDeploymentsView: View {

    @Environment(AuthManager.self) private var auth
    @State private var viewModel: WorkerDeploymentsViewModel
    @State private var toDelete: WorkerDeployment?
    @State private var deleteDenied = false

    init(accountId: String, scriptName: String, session: SessionStore) {
        _viewModel = State(initialValue: WorkerDeploymentsViewModel(
            service: session.workerService, accountId: accountId, scriptName: scriptName
        ))
    }

    private var canWrite: Bool { auth.hasScope("workers-scripts.write") }

    var body: some View {
        Group {
            if !viewModel.loaded && viewModel.isLoading {
                SkeletonList(rows: 6, trailing: true)
            } else if viewModel.deployments.isEmpty {
                ContentUnavailableView("暂无部署", systemImage: "clock.arrow.circlepath")
            } else {
                List {
                    Section {
                        ForEach(Array(viewModel.deployments.enumerated()), id: \.element.id) { index, dep in
                            row(dep, isActive: index == 0)
                                .swipeActions(edge: .trailing) {
                                    if index != 0 {
                                        Button(role: .destructive) {
                                            if canWrite { toDelete = dep } else { deleteDenied = true }
                                        } label: {
                                            Label("删除", systemImage: "trash")
                                        }
                                    }
                                }
                        }
                    } footer: {
                        Text("首项为当前活跃部署，不可删除。向左滑动删除历史部署。")
                            .font(.caption)
                    }
                    .glassRow()
                }
                .scrollContentBackground(.hidden)
                .refreshable { await viewModel.load() }
            }
        }
        .background { SkyBackground() }
        .navigationTitle("部署历史")
        .navigationBarTitleDisplayMode(.inline)
        .task { if !viewModel.loaded { await viewModel.load() } }
        .sensoryFeedback(.success, trigger: viewModel.didDelete)
        .alert("删除此部署？", isPresented: Binding(get: { toDelete != nil }, set: { if !$0 { toDelete = nil } })) {
            Button("取消", role: .cancel) { toDelete = nil }
            Button("删除", role: .destructive) {
                if let dep = toDelete {
                    Task { _ = await viewModel.delete(dep); toDelete = nil }
                }
            }
        } message: {
            Text("删除后不可恢复。")
        }
        .alert("权限不足", isPresented: $deleteDenied) {
            Button("好", role: .cancel) {}
        } message: {
            Text("当前授权未包含 Workers 写权限（workers-scripts.write）。\n请在设置中退出登录后重新授权以启用此功能。")
        }
        .overlay(alignment: .bottom) {
            if let error = viewModel.error {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .padding(10)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 10))
                    .padding()
            }
        }
    }

    @ViewBuilder
    private func row(_ dep: WorkerDeployment, isActive: Bool) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                if isActive {
                    Text("活跃")
                        .font(.caption2.weight(.bold))
                        .padding(.horizontal, 6).padding(.vertical, 2)
                        .background(Color.ocOrange.opacity(0.15))
                        .foregroundStyle(Color.ocOrange)
                        .clipShape(Capsule())
                }
                Text(dep.message ?? dep.source ?? String(dep.id.prefix(8)))
                    .font(.callout)
                    .lineLimit(1)
                Spacer()
                if let date = dep.createdDate {
                    Text(date, format: .relative(presentation: .named))
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
            HStack(spacing: 8) {
                Text(dep.id.prefix(8))
                    .font(.caption2.monospaced())
                    .foregroundStyle(.secondary)
                if let email = dep.authorEmail {
                    Text(email)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
        }
        .padding(.vertical, 2)
    }
}
