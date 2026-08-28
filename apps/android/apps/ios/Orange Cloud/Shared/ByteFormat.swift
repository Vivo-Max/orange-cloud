//
//  ByteFormat.swift
//  Orange Cloud（主 App / Widget / 手表共用）
//
//  字节数展示：数字随本地化，单位固定用国际通用符号（B/KB/MB/GB/TB/PB）。
//  系统 .byteCount / ByteCountFormatter 会把单位本地化（法语 Mo、阿语 ميغابايت），
//  存储/流量单位行业惯例不翻译，故手工拼接。
//

import Foundation

nonisolated extension Int64 {

    /// 十进制（1000 进位），对应系统 .decimal / .file 的量级口径
    var ocBytes: String { Self.ocFormat(self, base: 1000) }

    /// 二进制（1024 进位），对应系统 .binary
    var ocBytesBinary: String { Self.ocFormat(self, base: 1024) }

    private static func ocFormat(_ bytes: Int64, base: Double) -> String {
        guard bytes > 0 else { return "0 B" }
        let units = ["B", "KB", "MB", "GB", "TB", "PB"]
        var value = Double(bytes)
        var index = 0
        while value >= base, index < units.count - 1 {
            value /= base
            index += 1
        }
        // B 不带小数；其余按量级保留 0–2 位（与系统展示精度相当）
        let fraction = index == 0 ? 0 : (value >= 100 ? 0 : (value >= 10 ? 1 : 2))
        return value.formatted(.number.precision(.fractionLength(0...fraction))) + " " + units[index]
    }
}
