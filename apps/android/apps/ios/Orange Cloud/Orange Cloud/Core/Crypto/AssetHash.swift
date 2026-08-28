//
//  AssetHash.swift
//  Orange Cloud
//
//  Workers 静态资源（Workers Assets）manifest 哈希。与 Pages 同构（base64(内容)+扩展名），
//  仅把哈希函数从 BLAKE3 换成 SHA-256（CryptoKit 原生，无需 vendor）。取 hex 前 32 位。
//
//  ⚠️ 该输入口径（base64+扩展名）来自 wrangler 与 Pages 共享的实现推断，未经真机端到端核对。
//  若首次真机部署 assets-upload-session / upload 返回哈希不匹配，备选口径是
//  sha256(原始字节)——只需改本函数一处即可，调用方无需变动。
//

import Foundation
import CryptoKit

nonisolated enum AssetHash {
    static func workerAsset(data: Data, ext: String) -> String {
        let input = Data((data.base64EncodedString() + ext).utf8)
        let digest = SHA256.hash(data: input)
        let hex = digest.map { String(format: "%02x", $0) }.joined()
        return String(hex.prefix(32))
    }
}
