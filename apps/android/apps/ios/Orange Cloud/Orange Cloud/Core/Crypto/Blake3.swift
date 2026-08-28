//
//  Blake3.swift
//  Orange Cloud
//
//  自包含的 BLAKE3 哈希实现（无第三方依赖）。Cloudflare Pages「直接上传」部署
//  对每个文件算的资源键是 blake3(base64(内容) + 扩展名) 取 hex 前 32 位——
//  CryptoKit 不提供 BLAKE3，故在此移植参考实现。算法照 BLAKE3 官方参考实现
//  （IV / 消息置换 / 7 轮 G 混合 / 区块树 + 根标志），已对官方测试向量校验。
//
//  仅做哈希（非 keyed / 非 derive_key），输出长度固定按调用方需要截取。
//

import Foundation

nonisolated enum Blake3 {

    // MARK: - 常量

    fileprivate static let iv: [UInt32] = [
        0x6A09_E667, 0xBB67_AE85, 0x3C6E_F372, 0xA54F_F53A,
        0x510E_527F, 0x9B05_688C, 0x1F83_D9AB, 0x5BE0_CD19,
    ]

    fileprivate static let msgPermutation: [Int] = [2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8]

    fileprivate static let chunkStart: UInt32 = 1 << 0
    fileprivate static let chunkEnd:   UInt32 = 1 << 1
    fileprivate static let parent:     UInt32 = 1 << 2
    fileprivate static let root:       UInt32 = 1 << 3

    fileprivate static let blockLen = 64
    fileprivate static let chunkLen = 1024

    // MARK: - 对外 API

    /// 完整 32 字节 BLAKE3 摘要
    static func hash(_ input: [UInt8]) -> [UInt8] {
        var hasher = Hasher()
        hasher.update(input)
        return hasher.finalize(outputLen: 32)
    }

    /// hex 编码摘要，取前 `prefixChars` 个字符（Pages 资源键用前 32）
    static func hashHexPrefix(_ input: [UInt8], prefixChars: Int) -> String {
        let digest = hash(input)
        var hex = ""
        hex.reserveCapacity(digest.count * 2)
        for byte in digest {
            hex += String(format: "%02x", byte)
        }
        return String(hex.prefix(prefixChars))
    }

    // MARK: - 压缩函数

    @inline(__always)
    private static func rotr(_ x: UInt32, _ n: UInt32) -> UInt32 {
        (x >> n) | (x << (32 - n))
    }

    @inline(__always)
    private static func g(_ s: inout [UInt32], _ a: Int, _ b: Int, _ c: Int, _ d: Int, _ mx: UInt32, _ my: UInt32) {
        s[a] = s[a] &+ s[b] &+ mx
        s[d] = rotr(s[d] ^ s[a], 16)
        s[c] = s[c] &+ s[d]
        s[b] = rotr(s[b] ^ s[c], 12)
        s[a] = s[a] &+ s[b] &+ my
        s[d] = rotr(s[d] ^ s[a], 8)
        s[c] = s[c] &+ s[d]
        s[b] = rotr(s[b] ^ s[c], 7)
    }

    private static func roundFn(_ s: inout [UInt32], _ m: [UInt32]) {
        // 列
        g(&s, 0, 4, 8, 12, m[0], m[1])
        g(&s, 1, 5, 9, 13, m[2], m[3])
        g(&s, 2, 6, 10, 14, m[4], m[5])
        g(&s, 3, 7, 11, 15, m[6], m[7])
        // 对角
        g(&s, 0, 5, 10, 15, m[8], m[9])
        g(&s, 1, 6, 11, 12, m[10], m[11])
        g(&s, 2, 7, 8, 13, m[12], m[13])
        g(&s, 3, 4, 9, 14, m[14], m[15])
    }

    private static func permute(_ m: inout [UInt32]) {
        var p = [UInt32](repeating: 0, count: 16)
        for i in 0..<16 { p[i] = m[msgPermutation[i]] }
        m = p
    }

    /// 返回 16 个字（前 8 为链值，全部 16 用于根输出）
    fileprivate static func compress(
        _ cv: [UInt32],
        _ blockWords: [UInt32],
        _ counter: UInt64,
        _ blockLen: UInt32,
        _ flags: UInt32
    ) -> [UInt32] {
        var state: [UInt32] = [
            cv[0], cv[1], cv[2], cv[3], cv[4], cv[5], cv[6], cv[7],
            iv[0], iv[1], iv[2], iv[3],
            UInt32(truncatingIfNeeded: counter),
            UInt32(truncatingIfNeeded: counter >> 32),
            blockLen, flags,
        ]
        var m = blockWords
        for r in 0..<7 {
            roundFn(&state, m)
            if r < 6 { permute(&m) }   // 轮间置换，最后一轮后不置换
        }
        for i in 0..<8 {
            state[i] ^= state[i + 8]
            state[i + 8] ^= cv[i]
        }
        return state
    }

    /// 64 字节小端 → 16 个 UInt32
    fileprivate static func words(from block: [UInt8]) -> [UInt32] {
        var w = [UInt32](repeating: 0, count: 16)
        for i in 0..<16 {
            let o = i * 4
            w[i] = UInt32(block[o])
                | (UInt32(block[o + 1]) << 8)
                | (UInt32(block[o + 2]) << 16)
                | (UInt32(block[o + 3]) << 24)
        }
        return w
    }

    // MARK: - 节点输出（chunk 末块 / 父节点）

    fileprivate struct Output {
        let inputCV: [UInt32]
        let blockWords: [UInt32]
        let counter: UInt64
        let blockLen: UInt32
        let flags: UInt32

        func chainingValue() -> [UInt32] {
            Array(Blake3.compress(inputCV, blockWords, counter, blockLen, flags)[0..<8])
        }

        func rootOutputBytes(_ outLen: Int) -> [UInt8] {
            var out = [UInt8]()
            out.reserveCapacity(outLen)
            var outputBlockCounter: UInt64 = 0
            while out.count < outLen {
                let ws = Blake3.compress(inputCV, blockWords, outputBlockCounter, blockLen, flags | Blake3.root)
                for w in ws {
                    for b in 0..<4 {
                        if out.count >= outLen { break }
                        out.append(UInt8(truncatingIfNeeded: w >> (8 * UInt32(b))))
                    }
                    if out.count >= outLen { break }
                }
                outputBlockCounter += 1
            }
            return out
        }
    }

    fileprivate static func parentOutput(_ left: [UInt32], _ right: [UInt32], _ key: [UInt32], _ flags: UInt32) -> Output {
        var bw = left
        bw.append(contentsOf: right)
        return Output(inputCV: key, blockWords: bw, counter: 0, blockLen: UInt32(blockLen), flags: parent | flags)
    }

    fileprivate static func parentCV(_ left: [UInt32], _ right: [UInt32], _ key: [UInt32], _ flags: UInt32) -> [UInt32] {
        parentOutput(left, right, key, flags).chainingValue()
    }

    // MARK: - 增量哈希器

    struct Hasher {
        private let key: [UInt32]
        private let flags: UInt32 = 0

        // 当前 chunk 状态
        private var chunkCV: [UInt32]
        private var chunkCounter: UInt64
        private var block: [UInt8]
        private var blockLen: Int
        private var blocksCompressed: Int

        private var cvStack: [[UInt32]] = []

        init() {
            key = Blake3.iv
            chunkCV = Blake3.iv
            chunkCounter = 0
            block = [UInt8](repeating: 0, count: Blake3.blockLen)
            blockLen = 0
            blocksCompressed = 0
        }

        private var startFlag: UInt32 { blocksCompressed == 0 ? Blake3.chunkStart : 0 }
        private var chunkConsumed: Int { blocksCompressed * Blake3.blockLen + blockLen }

        mutating func update(_ input: [UInt8]) {
            var i = 0
            while i < input.count {
                // 当前 chunk 已满 → 收口出 CV、合并进栈、开新 chunk
                if chunkConsumed == Blake3.chunkLen {
                    let cv = chunkOutput().chainingValue()
                    let totalChunks = chunkCounter + 1
                    addChunkCV(cv, totalChunks)
                    chunkCV = key
                    chunkCounter = totalChunks
                    block = [UInt8](repeating: 0, count: Blake3.blockLen)
                    blockLen = 0
                    blocksCompressed = 0
                }
                let want = Blake3.chunkLen - chunkConsumed
                let take = min(want, input.count - i)
                feedChunk(input, i, i + take)
                i += take
            }
        }

        /// 把 input[start..<end]（≤ 一个 chunk 剩余空间）喂进当前 chunk
        private mutating func feedChunk(_ input: [UInt8], _ start: Int, _ end: Int) {
            var i = start
            while i < end {
                if blockLen == Blake3.blockLen {
                    let bw = Blake3.words(from: block)
                    chunkCV = Array(Blake3.compress(chunkCV, bw, chunkCounter, UInt32(Blake3.blockLen), flags | startFlag)[0..<8])
                    blocksCompressed += 1
                    block = [UInt8](repeating: 0, count: Blake3.blockLen)
                    blockLen = 0
                }
                let want = Blake3.blockLen - blockLen
                let take = min(want, end - i)
                for k in 0..<take { block[blockLen + k] = input[i + k] }
                blockLen += take
                i += take
            }
        }

        private func chunkOutput() -> Output {
            Output(
                inputCV: chunkCV,
                blockWords: Blake3.words(from: block),
                counter: chunkCounter,
                blockLen: UInt32(blockLen),
                flags: flags | startFlag | Blake3.chunkEnd
            )
        }

        private mutating func addChunkCV(_ newCV: [UInt32], _ totalChunks: UInt64) {
            var cv = newCV
            var tc = totalChunks
            while tc & 1 == 0 {
                cv = Blake3.parentCV(cvStack.removeLast(), cv, key, flags)
                tc >>= 1
            }
            cvStack.append(cv)
        }

        func finalize(outputLen: Int) -> [UInt8] {
            var output = chunkOutput()
            var remaining = cvStack.count
            while remaining > 0 {
                remaining -= 1
                output = Blake3.parentOutput(cvStack[remaining], output.chainingValue(), key, flags)
            }
            return output.rootOutputBytes(outputLen)
        }
    }
}
