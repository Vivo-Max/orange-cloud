//
//  RefreshGate.swift
//  Orange Cloud
//
//  跨进程 token 刷新串行化。主 App（AuthManager）与「文件」扩展（R2FileProviderClient）是两个
//  独立进程，却共享同一个 Cloudflare OAuth refresh token——而 Cloudflare 的 refresh token 是
//  **单次有效、轮转式**的：两个进程并发刷新会触发服务端的「复用检测」，把整条令牌链一起吊销，
//  主 App 随后再也刷不出 token、卡死在登录态（数据全空白、下拉无反应）。
//
//  这里在共享 App Group 容器里用 fcntl 文件记录锁做跨进程互斥：同一身份的刷新任一时刻只有一个
//  进程在跑（进程崩溃 / 退出时内核自动释放锁，不会留死锁）。**best-effort**：拿不到锁（超时）或
//  容器不可用时就降级为不持锁直接刷，绝不死等、绝不比现状更糟。
//
//  扩展进程里有一份等价实现（Orange Cloud File/RefreshGate.swift）——两 target 不共享源码，
//  与既有 token 读写一样按「自包含、零跨 target 依赖」复制；改一处务必同步另一处。
//
//  注：用 fcntl(F_SETLK) 而非 flock()，因为 BSD 的 flock() 函数与 `struct flock` 同名，Swift 里
//  无法干净地引用到函数；fcntl + struct flock 无歧义。
//

import Foundation

nonisolated enum RefreshGate {

    /// 与 entitlements / WidgetSnapshot.appGroupID 对齐
    private static let appGroupID = "group.jiamin.chen.Orange-Cloud"

    /// 取得「该身份刷新」的跨进程独占锁。返回非 nil 句柄表示已持锁——调用方**必须**用 `release` 释放；
    /// 返回 nil 表示降级（无共享容器 / 打不开 / 约 6s 内没抢到），调用方照常往下刷即可。
    static func acquire(sessionId: String) async -> Int32? {
        guard let dir = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupID) else {
            return nil
        }
        let lockURL = dir.appendingPathComponent("token-refresh-\(sessionId).lock")
        let fd = open(lockURL.path, O_CREAT | O_RDWR, 0o600)
        guard fd >= 0 else { return nil }
        for _ in 0..<120 {                                   // 120 × 50ms ≈ 6s 上限
            if lock(fd, type: Int16(F_WRLCK)) { return fd }
            // 显式检查取消：`try?` 会吞掉 sleep 抛出的 CancellationError，BGAppRefresh
            // 到期 cancel 后若仍空转抢锁，进程会带着待抢句柄被挂起
            if Task.isCancelled { break }
            try? await Task.sleep(nanoseconds: 50_000_000)
        }
        close(fd)                                            // 超时没抢到：关掉句柄，降级
        return nil
    }

    /// 释放 `acquire` 取得的锁（nil 为降级态，无操作）
    static func release(_ token: Int32?) {
        guard let fd = token else { return }
        _ = lock(fd, type: Int16(F_UNLCK))
        close(fd)
    }

    /// 对整个文件加 / 解非阻塞记录锁。成功返回 true（解锁恒为 true）。
    private static func lock(_ fd: Int32, type: Int16) -> Bool {
        var fl = flock()
        fl.l_start = 0
        fl.l_len = 0
        fl.l_type = type
        fl.l_whence = Int16(SEEK_SET)
        return fcntl(fd, F_SETLK, &fl) != -1
    }
}
