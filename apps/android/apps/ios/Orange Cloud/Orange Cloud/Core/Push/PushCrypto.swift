//
//  PushCrypto.swift
//  Orange Cloud
//
//  E2E 密钥生成（解密在 NSE 侧）。
//

import Foundation

nonisolated enum PushCrypto {
    /// 生成 32 字符密钥：UTF8 即 32 字节 = AES-256 key（密钥字符串字节直接当 key）。
    static func generateKey() -> String {
        let alphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789")
        var out = ""
        for _ in 0..<32 {
            out.append(alphabet[Int.random(in: 0..<alphabet.count)])
        }
        return out
    }
}
