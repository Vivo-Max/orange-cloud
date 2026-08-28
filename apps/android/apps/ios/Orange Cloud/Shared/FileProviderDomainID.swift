//
//  FileProviderDomainID.swift
//  Orange Cloud — Shared（主 App + FileProvider extension 双 target 编译）
//
//  NSFileProviderDomain 的 identifier 编码方案：把「登录身份 / 账号 / 桶名」三段塞进
//  domain identifier，extension 拿到 domain 即可还原出凭证来源（按 sessionId 从共享
//  Keychain 读 token）与 R2 目标（accountId + bucketName）。无需额外 App Group 配置表。
//
//  分隔符用 "|"：R2 桶名仅允许小写字母/数字/连字符，accountId 是十六进制，sessionId 是
//  UUID，三者都不含 "|"，不会冲突。
//

import Foundation

nonisolated enum FileProviderDomainID {

    private static let separator: Character = "|"

    /// 组装 domain identifier 字符串
    static func make(sessionId: UUID, accountId: String, bucketName: String) -> String {
        "\(sessionId.uuidString)\(separator)\(accountId)\(separator)\(bucketName)"
    }

    /// 解析 domain identifier；格式不符返回 nil
    static func parse(_ identifier: String) -> (sessionId: UUID, accountId: String, bucketName: String)? {
        let parts = identifier.split(separator: separator, omittingEmptySubsequences: false).map(String.init)
        guard parts.count == 3,
              let sessionId = UUID(uuidString: parts[0]),
              !parts[1].isEmpty, !parts[2].isEmpty else { return nil }
        return (sessionId, parts[1], parts[2])
    }
}
