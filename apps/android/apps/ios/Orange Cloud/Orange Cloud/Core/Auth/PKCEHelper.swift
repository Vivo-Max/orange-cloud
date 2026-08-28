//
//  PKCEHelper.swift
//  Orange Cloud
//
//  PKCE (RFC 7636)：code_verifier 随机生成，code_challenge = BASE64URL(SHA256(verifier))
//

import Foundation
import CryptoKit

nonisolated enum PKCEHelper {

    /// 生成 43–128 字符的 code_verifier（32 字节随机数 → 43 字符 base64url）
    static func generateCodeVerifier() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return Data(bytes).base64URLEncodedString()
    }

    /// code_challenge = BASE64URL(SHA256(code_verifier))，method 固定 S256
    static func generateCodeChallenge(from verifier: String) -> String {
        let digest = SHA256.hash(data: Data(verifier.utf8))
        return Data(digest).base64URLEncodedString()
    }
}

extension Data {
    /// base64url 编码（无 padding，RFC 4648 §5）
    nonisolated func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
