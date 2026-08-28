//
//  CIDRCalculator.swift
//  Orange Cloud
//
//  纯本地 CIDR 计算（IPv4 + IPv6），无网络。按字节掩码，避免 128 位大数运算。
//

import Foundation

nonisolated enum CIDRCalculator {

    /// 解析 "a.b.c.d/n" 或 "2001:db8::/n"，非法返回 nil。
    static func parse(_ input: String) -> CIDRResult? {
        let parts = input.split(separator: "/", maxSplits: 1, omittingEmptySubsequences: false)
        guard parts.count == 2, let prefix = Int(parts[1]) else { return nil }
        let addr = String(parts[0])
        return addr.contains(":") ? parseV6(addr, prefix: prefix) : parseV4(addr, prefix: prefix)
    }

    // MARK: IPv4

    private static func parseV4(_ addr: String, prefix: Int) -> CIDRResult? {
        guard prefix >= 0, prefix <= 32 else { return nil }
        var raw = in_addr()
        guard addr.withCString({ inet_pton(AF_INET, $0, &raw) }) == 1 else { return nil }
        let ip = UInt32(bigEndian: raw.s_addr)
        let mask: UInt32 = prefix == 0 ? 0 : ~UInt32(0) << (32 - prefix)
        let network = ip & mask
        let broadcast = network | ~mask
        let total = prefix == 0 ? (UInt64(UInt32.max) + 1) : (UInt64(1) << (32 - prefix))

        let first: UInt32
        let last: UInt32
        let usable: String?
        if prefix <= 30 {
            first = network &+ 1
            last = broadcast &- 1
            usable = String(total - 2)
        } else {
            first = network
            last = broadcast
            usable = prefix == 32 ? "1" : "2"
        }

        return CIDRResult(
            family: "IPv4",
            networkAddress: v4String(network),
            prefixLength: prefix,
            netmask: v4String(mask),
            firstAddress: v4String(first),
            lastAddress: v4String(last),
            totalCount: String(total),
            usableHosts: usable
        )
    }

    private static func v4String(_ value: UInt32) -> String {
        "\((value >> 24) & 0xff).\((value >> 16) & 0xff).\((value >> 8) & 0xff).\(value & 0xff)"
    }

    // MARK: IPv6（按 16 字节逐字节掩码）

    private static func parseV6(_ addr: String, prefix: Int) -> CIDRResult? {
        guard prefix >= 0, prefix <= 128 else { return nil }
        var raw = in6_addr()
        guard addr.withCString({ inet_pton(AF_INET6, $0, &raw) }) == 1 else { return nil }
        let bytes = withUnsafeBytes(of: &raw) { Array($0) }
        guard bytes.count == 16 else { return nil }

        var network = bytes
        var last = bytes
        for i in 0..<16 {
            let bitStart = i * 8
            if bitStart >= prefix {
                network[i] = 0
                last[i] = 0xff
            } else if bitStart + 8 <= prefix {
                last[i] = network[i]
            } else {
                let bitsInByte = prefix - bitStart
                let maskByte = UInt8(truncatingIfNeeded: 0xff << (8 - bitsInByte))
                network[i] = bytes[i] & maskByte
                last[i] = network[i] | ~maskByte
            }
        }

        let hostBits = 128 - prefix
        let total = hostBits >= 64 ? "2^\(hostBits)" : String(UInt64(1) << UInt64(hostBits))

        return CIDRResult(
            family: "IPv6",
            networkAddress: v6String(network) ?? addr,
            prefixLength: prefix,
            netmask: nil,
            firstAddress: v6String(network) ?? "—",
            lastAddress: v6String(last) ?? "—",
            totalCount: total,
            usableHosts: nil
        )
    }

    private static func v6String(_ bytes: [UInt8]) -> String? {
        var raw = in6_addr()
        withUnsafeMutableBytes(of: &raw) { dst in
            for i in 0..<min(16, bytes.count) { dst[i] = bytes[i] }
        }
        var buf = [CChar](repeating: 0, count: Int(INET6_ADDRSTRLEN))
        let result = inet_ntop(AF_INET6, &raw, &buf, socklen_t(INET6_ADDRSTRLEN))
        return result != nil ? String(cString: buf) : nil
    }
}
