package jiamin.chen.orangecloud.core.crypto

/**
 * 自包含 BLAKE3 哈希（无第三方依赖）。Cloudflare Pages「直接上传」对每个文件算的资源键 =
 * blake3(base64(内容) + 扩展名) 取 hex 前 32 位。逐行移植自 iOS Core/Crypto/Blake3.swift
 * （官方参考实现：IV / 消息置换 / 7 轮 G 混合 / 区块树 + 根标志），已对官方测试向量校验。
 * 仅哈希（非 keyed / 非 derive_key）。Kotlin UInt 算术按二进制补码自动回绕，等价 Swift `&+`。
 */
object Blake3 {

    private val IV = uintArrayOf(
        0x6A09E667u, 0xBB67AE85u, 0x3C6EF372u, 0xA54FF53Au,
        0x510E527Fu, 0x9B05688Cu, 0x1F83D9ABu, 0x5BE0CD19u,
    )
    private val MSG_PERMUTATION = intArrayOf(2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8)

    private val CHUNK_START: UInt = 1u
    private val CHUNK_END: UInt = 2u
    private val PARENT: UInt = 4u
    private val ROOT: UInt = 8u
    private const val BLOCK_LEN = 64
    private const val CHUNK_LEN = 1024

    fun hash(input: ByteArray): ByteArray {
        val h = Hasher()
        h.update(input)
        return h.finalize(32)
    }

    fun hashHexPrefix(input: ByteArray, prefixChars: Int): String {
        val digest = hash(input)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) sb.append("%02x".format(b.toInt() and 0xFF))
        return sb.substring(0, prefixChars)
    }

    private fun rotr(x: UInt, n: Int): UInt = (x shr n) or (x shl (32 - n))

    private fun g(s: UIntArray, a: Int, b: Int, c: Int, d: Int, mx: UInt, my: UInt) {
        s[a] = s[a] + s[b] + mx
        s[d] = rotr(s[d] xor s[a], 16)
        s[c] = s[c] + s[d]
        s[b] = rotr(s[b] xor s[c], 12)
        s[a] = s[a] + s[b] + my
        s[d] = rotr(s[d] xor s[a], 8)
        s[c] = s[c] + s[d]
        s[b] = rotr(s[b] xor s[c], 7)
    }

    private fun roundFn(s: UIntArray, m: UIntArray) {
        g(s, 0, 4, 8, 12, m[0], m[1])
        g(s, 1, 5, 9, 13, m[2], m[3])
        g(s, 2, 6, 10, 14, m[4], m[5])
        g(s, 3, 7, 11, 15, m[6], m[7])
        g(s, 0, 5, 10, 15, m[8], m[9])
        g(s, 1, 6, 11, 12, m[10], m[11])
        g(s, 2, 7, 8, 13, m[12], m[13])
        g(s, 3, 4, 9, 14, m[14], m[15])
    }

    private fun permute(m: UIntArray): UIntArray {
        val p = UIntArray(16)
        for (i in 0 until 16) p[i] = m[MSG_PERMUTATION[i]]
        return p
    }

    private fun compress(cv: UIntArray, blockWords: UIntArray, counter: ULong, blockLen: UInt, flags: UInt): UIntArray {
        val state = uintArrayOf(
            cv[0], cv[1], cv[2], cv[3], cv[4], cv[5], cv[6], cv[7],
            IV[0], IV[1], IV[2], IV[3],
            counter.toUInt(),
            (counter shr 32).toUInt(),
            blockLen, flags,
        )
        var m = blockWords
        for (r in 0 until 7) {
            roundFn(state, m)
            if (r < 6) m = permute(m)
        }
        for (i in 0 until 8) {
            state[i] = state[i] xor state[i + 8]
            state[i + 8] = state[i + 8] xor cv[i]
        }
        return state
    }

    private fun words(block: ByteArray): UIntArray {
        val w = UIntArray(16)
        for (i in 0 until 16) {
            val o = i * 4
            w[i] = (block[o].toInt() and 0xFF).toUInt() or
                ((block[o + 1].toInt() and 0xFF).toUInt() shl 8) or
                ((block[o + 2].toInt() and 0xFF).toUInt() shl 16) or
                ((block[o + 3].toInt() and 0xFF).toUInt() shl 24)
        }
        return w
    }

    private class Output(
        val inputCV: UIntArray,
        val blockWords: UIntArray,
        val counter: ULong,
        val blockLen: UInt,
        val flags: UInt,
    ) {
        fun chainingValue(): UIntArray = compress(inputCV, blockWords, counter, blockLen, flags).copyOfRange(0, 8)

        fun rootOutputBytes(outLen: Int): ByteArray {
            val out = ArrayList<Byte>(outLen)
            var outputBlockCounter = 0uL
            while (out.size < outLen) {
                val ws = compress(inputCV, blockWords, outputBlockCounter, blockLen, flags or ROOT)
                loop@ for (w in ws) {
                    for (b in 0 until 4) {
                        if (out.size >= outLen) break@loop
                        out.add(((w shr (8 * b)).toInt() and 0xFF).toByte())
                    }
                    if (out.size >= outLen) break
                }
                outputBlockCounter += 1uL
            }
            return out.toByteArray()
        }
    }

    private fun parentOutput(left: UIntArray, right: UIntArray, key: UIntArray, flags: UInt): Output {
        val bw = UIntArray(16)
        left.copyInto(bw, 0); right.copyInto(bw, 8)
        return Output(key, bw, 0uL, BLOCK_LEN.toUInt(), PARENT or flags)
    }

    private fun parentCV(left: UIntArray, right: UIntArray, key: UIntArray, flags: UInt): UIntArray =
        parentOutput(left, right, key, flags).chainingValue()

    private class Hasher {
        private val key = IV.copyOf()
        private val flags: UInt = 0u
        private var chunkCV = IV.copyOf()
        private var chunkCounter = 0uL
        private var block = ByteArray(BLOCK_LEN)
        private var blockLen = 0
        private var blocksCompressed = 0
        private val cvStack = ArrayList<UIntArray>()

        private val startFlag: UInt get() = if (blocksCompressed == 0) CHUNK_START else 0u
        private val chunkConsumed: Int get() = blocksCompressed * BLOCK_LEN + blockLen

        fun update(input: ByteArray) {
            var i = 0
            while (i < input.size) {
                if (chunkConsumed == CHUNK_LEN) {
                    val cv = chunkOutput().chainingValue()
                    val totalChunks = chunkCounter + 1uL
                    addChunkCV(cv, totalChunks)
                    chunkCV = key.copyOf()
                    chunkCounter = totalChunks
                    block = ByteArray(BLOCK_LEN)
                    blockLen = 0
                    blocksCompressed = 0
                }
                val want = CHUNK_LEN - chunkConsumed
                val take = minOf(want, input.size - i)
                feedChunk(input, i, i + take)
                i += take
            }
        }

        private fun feedChunk(input: ByteArray, start: Int, end: Int) {
            var i = start
            while (i < end) {
                if (blockLen == BLOCK_LEN) {
                    val bw = words(block)
                    chunkCV = compress(chunkCV, bw, chunkCounter, BLOCK_LEN.toUInt(), flags or startFlag).copyOfRange(0, 8)
                    blocksCompressed += 1
                    block = ByteArray(BLOCK_LEN)
                    blockLen = 0
                }
                val want = BLOCK_LEN - blockLen
                val take = minOf(want, end - i)
                System.arraycopy(input, i, block, blockLen, take)
                blockLen += take
                i += take
            }
        }

        private fun chunkOutput(): Output =
            Output(chunkCV, words(block), chunkCounter, blockLen.toUInt(), flags or startFlag or CHUNK_END)

        private fun addChunkCV(newCV: UIntArray, totalChunks: ULong) {
            var cv = newCV
            var tc = totalChunks
            while (tc and 1uL == 0uL) {
                cv = parentCV(cvStack.removeAt(cvStack.size - 1), cv, key, flags)
                tc = tc shr 1
            }
            cvStack.add(cv)
        }

        fun finalize(outputLen: Int): ByteArray {
            var output = chunkOutput()
            var remaining = cvStack.size
            while (remaining > 0) {
                remaining -= 1
                output = parentOutput(cvStack[remaining], output.chainingValue(), key, flags)
            }
            return output.rootOutputBytes(outputLen)
        }
    }
}
