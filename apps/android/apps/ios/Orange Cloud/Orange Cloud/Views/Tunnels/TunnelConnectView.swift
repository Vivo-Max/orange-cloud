//
//  TunnelConnectView.swift
//  Orange Cloud
//
//  隧道连接信息：拉取连接令牌（argotunnel.write 才可读），给出 cloudflared 安装命令并支持复制。
//  从详情页推入（普通返回）或新建流程推入（带"完成"按钮关闭整张 Sheet）。
//

import SwiftUI
import UIKit

struct TunnelConnectView: View {

    let tunnel: Tunnel
    let accountId: String
    let session: SessionStore
    var onDone: (() -> Void)?

    @State private var viewModel: TunnelDetailViewModel
    @State private var revealed = false
    @State private var copied = false

    init(tunnel: Tunnel, accountId: String, session: SessionStore, onDone: (() -> Void)? = nil) {
        self.tunnel = tunnel
        self.accountId = accountId
        self.session = session
        self.onDone = onDone
        _viewModel = State(initialValue: TunnelDetailViewModel(
            tunnel: tunnel, accountId: accountId, session: session, canWriteDNS: false
        ))
    }

    private var installCommand: String {
        "cloudflared service install \(viewModel.token ?? "")"
    }

    var body: some View {
        List {
            Section {
                Text("在目标机器上安装 cloudflared，并以管理员身份运行下面的命令，隧道即可连接。")
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
            .glassRow()

            Section {
                if let token = viewModel.token {
                    Text(revealed ? installCommand : maskedCommand(token: token))
                        .font(.caption.monospaced())
                        .textSelection(.enabled)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button {
                        revealed.toggle()
                    } label: {
                        Label(revealed ? "隐藏令牌" : "显示令牌",
                              systemImage: revealed ? "eye.slash" : "eye")
                            .font(.subheadline.weight(.medium))
                    }

                    Button {
                        UIPasteboard.general.string = installCommand
                        copied = true
                    } label: {
                        Label(copied ? "已复制命令" : "复制命令",
                              systemImage: copied ? "checkmark" : "doc.on.doc")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Color.ocOrangeText)
                    }
                    .contentTransition(.symbolEffect(.replace))
                } else if viewModel.isLoadingToken {
                    HStack(spacing: 8) {
                        ProgressView()
                        Text("获取令牌…").foregroundStyle(.secondary)
                    }
                } else {
                    Text(viewModel.error ?? String(localized: "无法获取令牌"))
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            } header: {
                Text("运行命令")
            } footer: {
                Text("令牌等同于隧道凭据，请妥善保管，不要公开分享。")
            }
            .glassRow()
        }
        .daybreakList()
        .navigationTitle("连接隧道")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.success, trigger: copied)
        .task { await viewModel.loadToken() }
        .toolbar {
            if let onDone {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") { onDone() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    /// 未揭示时只显示令牌前缀，避免凭据完整暴露在屏幕上。
    private func maskedCommand(token: String) -> String {
        "cloudflared service install \(token.prefix(8))…••••"
    }
}
