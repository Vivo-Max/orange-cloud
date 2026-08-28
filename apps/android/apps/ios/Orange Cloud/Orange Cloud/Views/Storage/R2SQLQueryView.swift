//
//  R2SQLQueryView.swift
//  Orange Cloud
//
//  R2 SQL 查询控制台（叶子页，桶设置 sheet 内再起一层 sheet 打开）。
//  只读分析查询（SELECT / EXPLAIN），带计费提示与展示行数截断（对齐 D1 防线）。
//

import SwiftUI

struct R2SQLQueryView: View {

    @State private var viewModel: R2SQLViewModel
    @State private var sql = ""
    @FocusState private var editorFocused: Bool
    @Environment(\.dismiss) private var dismiss

    init(session: SessionStore, accountId: String, bucketName: String) {
        _viewModel = State(initialValue: R2SQLViewModel(
            sqlService: session.r2SQLService,
            catalogService: session.r2CatalogService,
            accountId: accountId,
            bucketName: bucketName
        ))
    }

    private var canRun: Bool {
        !sql.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !viewModel.isRunning
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    editorCard
                    if let error = viewModel.error {
                        Label(error, systemImage: "exclamationmark.triangle")
                            .font(.footnote)
                            .foregroundStyle(.red)
                            .padding(.horizontal, 4)
                    }
                    if let result = viewModel.result {
                        resultCard(result)
                    }
                }
                .padding()
            }
            .background { SkyBackground() }
            .navigationTitle("R2 SQL")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("完成") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        editorFocused = false
                        Task { await viewModel.run(sql: sql) }
                    } label: {
                        if viewModel.isRunning {
                            ProgressView()
                        } else {
                            Label("运行", systemImage: "play.fill")
                        }
                    }
                    .disabled(!canRun)
                }
            }
            .task { await viewModel.loadSchema() }
        }
    }

    // MARK: - 编辑器

    private var editorCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            TextEditor(text: $sql)
                .font(.callout.monospaced())
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($editorFocused)
                .frame(minHeight: 120)
                .scrollContentBackground(.hidden)
                .overlay(alignment: .topLeading) {
                    if sql.isEmpty {
                        Text(verbatim: "SELECT * FROM namespace.table LIMIT 100")
                            .font(.callout.monospaced())
                            .foregroundStyle(.tertiary)
                            .padding(.top, 8)
                            .padding(.leading, 5)
                            .allowsHitTesting(false)
                    }
                }

            if !viewModel.tableReferences.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(viewModel.tableReferences, id: \.self) { table in
                            Button {
                                insert(table)
                            } label: {
                                Text(table)
                                    .font(.caption2.monospaced())
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.ocOrange.opacity(0.12), in: Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                // 表引用是 SQL 标识符，保持 LTR
                .environment(\.layoutDirection, .leftToRight)
            }

            Text("只读查询（SELECT / EXPLAIN）。按扫描数据量计费：每月前 10 GB 免费，超出 $0.0025/GB，单次查询至少计 10 MB——大表请先加 WHERE / LIMIT。")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .glassIsland(cornerRadius: 20)
    }

    private func insert(_ token: String) {
        if sql.isEmpty {
            sql = "SELECT * FROM \(token) LIMIT 100"
        } else {
            sql += sql.hasSuffix(" ") || sql.hasSuffix("\n") ? token : " \(token)"
        }
    }

    // MARK: - 结果

    private func resultCard(_ result: R2SQLResult) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if result.rows.isEmpty {
                Label("执行成功，无返回行", systemImage: "checkmark.circle")
                    .font(.callout)
                    .foregroundStyle(.green)
            } else {
                Text("\(result.rows.count) 行 · \(result.columns.count) 列")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                ScrollView(.horizontal, showsIndicators: true) {
                    Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 6) {
                        GridRow {
                            ForEach(result.columns, id: \.self) { column in
                                Text(column)
                                    .font(.caption.bold())
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Divider()
                        ForEach(Array(result.rows.prefix(R2SQLViewModel.maxRows).enumerated()), id: \.offset) { _, row in
                            GridRow {
                                ForEach(Array(row.enumerated()), id: \.offset) { _, cell in
                                    Text(cell)
                                        .font(.caption.monospaced())
                                        .lineLimit(1)
                                }
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
                // 查询结果表（列名/数据值）保持 LTR 列序
                .environment(\.layoutDirection, .leftToRight)
                if result.rows.count > R2SQLViewModel.maxRows {
                    Text("仅显示前 \(R2SQLViewModel.maxRows) 行（共 \(result.rows.count) 行）")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(14)
        .glassIsland(cornerRadius: 20)
    }
}
