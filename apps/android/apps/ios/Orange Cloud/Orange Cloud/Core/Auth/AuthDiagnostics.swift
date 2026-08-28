//
//  AuthDiagnostics.swift
//  Orange Cloud
//
//  临时诊断（issue #5「经常需要重新登录」）：坐实「iCloud 钥匙串复活已轮换刷新令牌」的竞态。
//
//  原理：刷新令牌每次刷新都轮换（旧的被服务端作废）。我们每次「自己写入」令牌时，把刷新令牌的
//  不可逆指纹记到设备本地基线（绝不同步）。下次发起刷新前，对比当前钥匙串里的刷新令牌指纹与基线：
//  若不一致，说明这枚令牌不是我们最后写入的那枚——只可能是 iCloud 钥匙串把旧值同步覆盖回来了。
//  紧接着若刷新被 token 端点 400 拒绝并登出，即坐实「iCloud 复活旧令牌 → 400 → 误登出」。
//
//  采集：日志经 AppLog.auth 同时进 Console（subsystem `jiamin.chen.orange-cloud` / category `auth`）
//  与 App 内日志文件，可在「设置 → 帮助与反馈」随反馈导出。看到 "token changed externally"
//  紧跟 "rejected 400" 即坐实竞态。坐实后可删除指纹/基线逻辑（日志门面 AppLog 保留）。
//

import Foundation
import CryptoKit

nonisolated enum AuthDiagnostics {

    /// 不可逆指纹：SHA256 前 4 字节 hex（8 位）。足以区分不同令牌，不泄露令牌本体。
    static func fingerprint(_ token: String?) -> String {
        guard let token, !token.isEmpty else { return "nil" }
        let digest = SHA256.hash(data: Data(token.utf8))
        return digest.prefix(4).map { String(format: "%02x", $0) }.joined()
    }

    // MARK: - 设备本地基线（绝不参与 iCloud 同步）

    private static func baselineKey(_ sessionId: UUID) -> String {
        "diag.lastWrittenRefreshFP.\(sessionId.uuidString)"
    }

    /// 记录「我们自己最后写入的刷新令牌指纹」
    static func recordWrite(refreshToken: String?, sessionId: UUID) {
        UserDefaults.standard.set(fingerprint(refreshToken), forKey: baselineKey(sessionId))
    }

    static func lastWrittenFingerprint(_ sessionId: UUID) -> String? {
        UserDefaults.standard.string(forKey: baselineKey(sessionId))
    }

    static func clearBaseline(_ sessionId: UUID) {
        UserDefaults.standard.removeObject(forKey: baselineKey(sessionId))
    }
}
