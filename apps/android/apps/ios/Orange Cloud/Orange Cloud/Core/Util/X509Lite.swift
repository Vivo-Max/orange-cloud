//
//  X509Lite.swift
//  Orange Cloud
//
//  极简 X.509 DER 解析：只取 SSL 检查需要的字段（序列号 / 生效到期 / 主题与签发 CN / SAN）。
//  iOS 无公开 API 直接读这些字段（SecCertificateCopyValues 仅 macOS），故自解 TBSCertificate。
//  解析失败优雅降级（返回 nil 字段），不抛错。
//

import Foundation

nonisolated enum X509Lite {

    struct Node {
        let tag: UInt8
        let length: Int
        let valueStart: Int
        var valueEnd: Int { valueStart + length }
    }

    struct Parsed {
        var serialHex: String?
        var notBefore: Date?
        var notAfter: Date?
        var subjectCN: String?
        var issuerCN: String?
        var sanDNS: [String]
    }

    static func parse(der bytes: [UInt8]) -> Parsed? {
        guard !bytes.isEmpty,
              let cert = tlv(bytes, 0), cert.tag == 0x30,
              let tbs = tlv(bytes, cert.valueStart), tbs.tag == 0x30 else { return nil }

        var children: [Node] = []
        var cursor = tbs.valueStart
        while cursor < tbs.valueEnd, let node = tlv(bytes, cursor) {
            children.append(node)
            cursor = node.valueEnd
        }

        // version [0] 可选（context tag 0xA0），存在则后续字段整体后移一位
        var idx = 0
        if let first = children.first, first.tag == 0xA0 { idx = 1 }

        var result = Parsed(serialHex: nil, notBefore: nil, notAfter: nil, subjectCN: nil, issuerCN: nil, sanDNS: [])

        if children.indices.contains(idx), children[idx].tag == 0x02 {
            result.serialHex = hex(bytes, children[idx])
        }
        let issuerIdx = idx + 2
        let validityIdx = idx + 3
        let subjectIdx = idx + 4
        if children.indices.contains(issuerIdx) { result.issuerCN = commonName(bytes, children[issuerIdx]) }
        if children.indices.contains(validityIdx) {
            let pair = validity(bytes, children[validityIdx])
            result.notBefore = pair.0
            result.notAfter = pair.1
        }
        if children.indices.contains(subjectIdx) { result.subjectCN = commonName(bytes, children[subjectIdx]) }
        if let ext = children.first(where: { $0.tag == 0xA3 }) { result.sanDNS = san(bytes, ext) }
        return result
    }

    // MARK: - TLV

    private static func tlv(_ b: [UInt8], _ start: Int) -> Node? {
        guard start >= 0, start < b.count else { return nil }
        let tag = b[start]
        var i = start + 1
        guard i < b.count else { return nil }
        let firstLen = b[i]; i += 1
        var length = 0
        if firstLen & 0x80 == 0 {
            length = Int(firstLen)
        } else {
            let count = Int(firstLen & 0x7f)
            guard count > 0, count <= 4, i + count <= b.count else { return nil }
            for _ in 0..<count { length = (length << 8) | Int(b[i]); i += 1 }
        }
        guard length >= 0, i + length <= b.count else { return nil }
        return Node(tag: tag, length: length, valueStart: i)
    }

    // MARK: - Name → CN

    private static func commonName(_ b: [UInt8], _ name: Node) -> String? {
        var cursor = name.valueStart
        while cursor < name.valueEnd, let set = tlv(b, cursor) {
            var c2 = set.valueStart
            while c2 < set.valueEnd, let atv = tlv(b, c2), atv.tag == 0x30 {
                if let oid = tlv(b, atv.valueStart), oid.tag == 0x06,
                   oidEquals(b, oid, [0x55, 0x04, 0x03]),
                   let val = tlv(b, oid.valueEnd) {
                    return string(b, val)
                }
                c2 = atv.valueEnd
            }
            cursor = set.valueEnd
        }
        return nil
    }

    // MARK: - Validity

    private static func validity(_ b: [UInt8], _ node: Node) -> (Date?, Date?) {
        var dates: [Date?] = []
        var cursor = node.valueStart
        while cursor < node.valueEnd, dates.count < 2, let t = tlv(b, cursor) {
            dates.append(time(b, t))
            cursor = t.valueEnd
        }
        return (dates.first ?? nil, dates.count > 1 ? dates[1] : nil)
    }

    private static func time(_ b: [UInt8], _ node: Node) -> Date? {
        guard let s = string(b, node) else { return nil }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = node.tag == 0x17 ? "yyMMddHHmmss'Z'" : "yyyyMMddHHmmss'Z'"
        return f.date(from: s)
    }

    // MARK: - SAN（subjectAltName，OID 2.5.29.17）

    private static func san(_ b: [UInt8], _ ext: Node) -> [String] {
        guard let seq = tlv(b, ext.valueStart), seq.tag == 0x30 else { return [] }
        var names: [String] = []
        var cursor = seq.valueStart
        while cursor < seq.valueEnd, let one = tlv(b, cursor), one.tag == 0x30 {
            if let oid = tlv(b, one.valueStart), oid.tag == 0x06, oidEquals(b, oid, [0x55, 0x1D, 0x11]) {
                var c = oid.valueEnd
                var octet: Node?
                while c < one.valueEnd, let n = tlv(b, c) {
                    if n.tag == 0x04 { octet = n; break }
                    c = n.valueEnd
                }
                if let octet, let gnSeq = tlv(b, octet.valueStart), gnSeq.tag == 0x30 {
                    var gc = gnSeq.valueStart
                    while gc < gnSeq.valueEnd, let gn = tlv(b, gc) {
                        if gn.tag == 0x82, let s = string(b, gn) { names.append(s) }  // dNSName [2] IA5String
                        gc = gn.valueEnd
                    }
                }
                break
            }
            cursor = one.valueEnd
        }
        return names
    }

    // MARK: - Helpers

    private static func oidEquals(_ b: [UInt8], _ node: Node, _ expected: [UInt8]) -> Bool {
        guard node.length == expected.count else { return false }
        for (i, e) in expected.enumerated() where b[node.valueStart + i] != e { return false }
        return true
    }

    private static func string(_ b: [UInt8], _ node: Node) -> String? {
        let slice = Array(b[node.valueStart..<node.valueEnd])
        return String(bytes: slice, encoding: .utf8) ?? String(bytes: slice, encoding: .ascii)
    }

    private static func hex(_ b: [UInt8], _ node: Node) -> String {
        b[node.valueStart..<node.valueEnd].map { String(format: "%02X", $0) }.joined(separator: " ")
    }
}
