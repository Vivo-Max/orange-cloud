//
//  TailSocket.swift
//  Orange Cloud
//
//  Workers tail 的 WebSocket 传输层。
//  - 独立 actor：收包与 JSON 解码不占主线程
//  - URL 是预签名的 wss://（创建 tail session 时返回），连接无需 Bearer Token
//  - 生命周期 = 一条连接：断开即弃，重连由 ViewModel 新建实例
//

import Foundation

actor TailSocket {

    private let task: URLSessionWebSocketTask
    private var started = false
    private var closed = false
    private var receiveTask: Task<Void, Never>?
    private var pingTask: Task<Void, Never>?
    private var continuation: AsyncThrowingStream<TailTraceItem, Error>.Continuation?

    init(url: URL) {
        task = URLSession.shared.webSocketTask(with: url, protocols: ["trace-v1"])
    }

    /// 连接并返回事件流。每个实例只允许调用一次。
    func events() -> AsyncThrowingStream<TailTraceItem, Error> {
        guard !started else {
            return AsyncThrowingStream { $0.finish() }
        }
        started = true

        let (stream, continuation) = AsyncThrowingStream<TailTraceItem, Error>.makeStream()
        self.continuation = continuation
        let task = self.task

        task.resume()
        AppLog.websocket.info("tail connecting")

        receiveTask = Task {
            do {
                // trace-v1 协议要求连接后先声明过滤器
                try await task.send(.string(#"{"filters":[],"debug":false}"#))

                let decoder = JSONDecoder()
                while !Task.isCancelled {
                    let message = try await task.receive()
                    let data: Data? = switch message {
                    case .string(let text): text.data(using: .utf8)
                    case .data(let d):      d
                    @unknown default:       nil
                    }
                    guard let data else { continue }
                    // 单条解码失败跳过，不中断日志流
                    if let item = try? decoder.decode(TailTraceItem.self, from: data) {
                        continuation.yield(item)
                    }
                }
                continuation.finish()
            } catch {
                if !Task.isCancelled {
                    AppLog.websocket.error("tail receive error: \(error.localizedDescription)")
                }
                continuation.finish(throwing: Task.isCancelled ? nil : error)
            }
        }

        // 心跳：防止 tail session 因 inactivity 过期
        pingTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(30))
                guard !Task.isCancelled else { break }
                task.sendPing { _ in }
            }
        }

        continuation.onTermination = { [weak self] _ in
            Task { await self?.close() }
        }

        return stream
    }

    /// 幂等关闭
    func close() {
        guard !closed else { return }
        closed = true
        AppLog.websocket.info("tail closed")
        receiveTask?.cancel()
        pingTask?.cancel()
        task.cancel(with: .goingAway, reason: nil)
        continuation?.finish()
        continuation = nil
    }
}
