//
//  ZipReader.swift
//  Orange Cloud
//
//  极简、零依赖的 ZIP 解包（仅读）：解析中央目录 + 用 Apple Compression 框架做 DEFLATE 解压。
//  Pages「直接上传」没有「上传 zip」端点——zip 必须在设备端解包后逐文件上传，此处负责解包。
//  支持 store(0) 与 deflate(8)；不支持 Zip64 / 加密（超出手机端部署小型静态站的需要，遇到即报错）。
//

import Foundation
import Compression

nonisolated enum ZipReader {

    struct Entry: Sendable {
        let path: String
        let data: Data
    }

    enum ZipError: LocalizedError {
        case notZip
        case corrupted
        case unsupportedMethod(Int)
        case zip64Unsupported
        case inflateFailed(String)

        var errorDescription: String? {
            switch self {
            case .notZip:                    String(localized: "不是有效的 ZIP 文件")
            case .corrupted:                 String(localized: "ZIP 文件已损坏或格式不受支持")
            case .unsupportedMethod(let m):  String(localized: "不支持的压缩方式（\(m)）")
            case .zip64Unsupported:          String(localized: "暂不支持 Zip64（超大压缩包）")
            case .inflateFailed(let name):   String(localized: "解压「\(name)」失败")
            }
        }
    }

    /// 解出全部文件条目（跳过目录项与 __MACOSX/.DS_Store 之类的垃圾）
    static func entries(from data: Data) throws -> [Entry] {
        let bytes = [UInt8](data)
        guard let eocd = findEOCD(bytes) else { throw ZipError.notZip }

        let entryCount = readU16(bytes, eocd + 10)
        let cdOffset = Int(readU32(bytes, eocd + 16))
        // Zip64 标记是 cdOffset == 0xFFFFFFFF（不能用 entryCount==0xFFFF 判断——
        // 恰好 65535 个条目的普通 zip 是合法的）
        if cdOffset == 0xFFFF_FFFF { throw ZipError.zip64Unsupported }

        var p = cdOffset
        var entries: [Entry] = []
        for _ in 0..<entryCount {
            guard p + 46 <= bytes.count, readU32(bytes, p) == 0x0201_4b50 else { throw ZipError.corrupted }
            let method      = readU16(bytes, p + 10)
            let compSize    = Int(readU32(bytes, p + 20))
            let uncompSize  = Int(readU32(bytes, p + 24))
            let nameLen     = Int(readU16(bytes, p + 28))
            let extraLen    = Int(readU16(bytes, p + 30))
            let commentLen  = Int(readU16(bytes, p + 32))
            let localOffset = Int(readU32(bytes, p + 42))
            let nameStart   = p + 46
            guard nameStart + nameLen <= bytes.count else { throw ZipError.corrupted }
            let name = String(decoding: bytes[nameStart..<nameStart + nameLen], as: UTF8.self)
            p = nameStart + nameLen + extraLen + commentLen

            if name.hasSuffix("/") { continue }                                   // 目录项
            if name.hasPrefix("__MACOSX/") { continue }                           // macOS 资源叉
            if (name as NSString).lastPathComponent == ".DS_Store" { continue }
            if compSize == 0xFFFF_FFFF || uncompSize == 0xFFFF_FFFF || localOffset == 0xFFFF_FFFF {
                throw ZipError.zip64Unsupported
            }

            // 本地头算数据起点（本地头的 name/extra 长度可能与中央目录不同）
            guard localOffset + 30 <= bytes.count, readU32(bytes, localOffset) == 0x0403_4b50 else { throw ZipError.corrupted }
            let localNameLen  = Int(readU16(bytes, localOffset + 26))
            let localExtraLen = Int(readU16(bytes, localOffset + 28))
            let dataStart = localOffset + 30 + localNameLen + localExtraLen
            guard dataStart + compSize <= bytes.count else { throw ZipError.corrupted }
            let comp = Array(bytes[dataStart..<dataStart + compSize])

            let content: Data
            switch method {
            case 0: content = Data(comp)                                          // store
            case 8: content = try inflate(comp, expectedSize: uncompSize, name: name)  // deflate
            default: throw ZipError.unsupportedMethod(method)
            }
            entries.append(Entry(path: name, data: content))
        }
        return entries
    }

    /// 原始 DEFLATE 解压（ZIP 用无 zlib 包头的裸 deflate，对应 Apple 的 COMPRESSION_ZLIB）
    private static func inflate(_ input: [UInt8], expectedSize: Int, name: String) throws -> Data {
        if expectedSize == 0 { return Data() }
        var dst = [UInt8](repeating: 0, count: expectedSize)
        let written = input.withUnsafeBufferPointer { src in
            dst.withUnsafeMutableBufferPointer { out in
                compression_decode_buffer(out.baseAddress!, expectedSize, src.baseAddress!, input.count, nil, COMPRESSION_ZLIB)
            }
        }
        guard written == expectedSize else { throw ZipError.inflateFailed(name) }
        return Data(dst)
    }

    /// 从尾部回扫 End of Central Directory 签名（EOCD 最小 22 字节，注释最长 64KB）
    private static func findEOCD(_ bytes: [UInt8]) -> Int? {
        let minLen = 22
        guard bytes.count >= minLen else { return nil }
        let lowerBound = max(0, bytes.count - minLen - 0xFFFF)
        var i = bytes.count - minLen
        while i >= lowerBound {
            // 校验注释长度字段与实际尾部对齐，避免命中存档注释里伪造的 EOCD 签名
            if readU32(bytes, i) == 0x0605_4b50,
               i + minLen + readU16(bytes, i + 20) == bytes.count {
                return i
            }
            i -= 1
        }
        return nil
    }

    private static func readU16(_ b: [UInt8], _ o: Int) -> Int {
        Int(b[o]) | (Int(b[o + 1]) << 8)
    }

    private static func readU32(_ b: [UInt8], _ o: Int) -> UInt32 {
        UInt32(b[o]) | (UInt32(b[o + 1]) << 8) | (UInt32(b[o + 2]) << 16) | (UInt32(b[o + 3]) << 24)
    }
}
