//
//  RefreshGate.swift
//  Orange Cloud File
//
//  跨进程 token 刷新串行化（扩展进程侧）。与主 App 的 Core/Auth/RefreshGate.swift 是等价实现——
//  两 target 不共享源码，按本扩展「自包含、零跨 target 依赖」的既有约定复制一份；改一处务必同步另一处。
//
//  动机：主 App 与本扩展共享同一个 Cloudflare OAuth refresh token，而该令牌单次有效、轮转式。
//  两进程并发刷新会触发服务端复用检测吊销整条令牌链，主 App 随后卡死登录态。用共享 App Group 容器
//  里的 fcntl 文件记录锁让同一身份的刷新任一时刻只有一个进程在跑。best-effort：拿不到锁就降级直刷。
//

import Foundation

enum RefreshGate {

    /// 与 entitlements 的 application-groups 对齐
    private static let appGroupID = "group.jiamin.chen.Orange-Cloud"

    /// 取得「该身份刷新」的跨进程独占锁。非 nil = 已持锁（调用方必须 release）；nil = 降级（照常刷）。
    static func acquire(sessionId: String) async -> Int32? {
        guard let dir = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupID) else {
            return nil
        }
        let lockURL = dir.appendingPathComponent("token-refresh-\(sessionId).lock")
        let fd = open(lockURL.path, O_CREAT | O_RDWR, 0o600)
        guard fd >= 0 else { return nil }
        for _ in 0..<120 {                                   // 120 × 50ms ≈ 6s 上限
            if lock(fd, type: Int16(F_WRLCK)) { return fd }
            try? await Task.sleep(nanoseconds: 50_000_000)
        }
        close(fd)
        return nil
    }

    static func release(_ token: Int32?) {
        guard let fd = token else { return }
        _ = lock(fd, type: Int16(F_UNLCK))
        close(fd)
    }

    /// 对整个文件加 / 解非阻塞记录锁（fcntl + struct flock，避开 flock() 函数与同名结构体的歧义）
    private static func lock(_ fd: Int32, type: Int16) -> Bool {
        var fl = flock()
        fl.l_start = 0
        fl.l_len = 0
        fl.l_type = type
        fl.l_whence = Int16(SEEK_SET)
        return fcntl(fd, F_SETLK, &fl) != -1
    }
}
