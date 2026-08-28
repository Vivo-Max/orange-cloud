package jiamin.chen.orangecloud.core.logging

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * AppLog 的文件落地（对应 iOS LogFileStore）：日志写进 cacheDir/logs/app.log，
 * 超上限滚动一代（app.1.log）。导出时合并上一代 + 当前（旧在前），约束体积便于作邮件附件。
 * 全部读写 @Synchronized，对外线程安全。
 *
 * 脱敏铁律（调用方负责）：绝不把 token / Cookie / Authorization / KV·密钥的值写进消息。
 */
class LogFileStore(cacheDir: File) {

    private val dir = File(cacheDir, "logs").apply { runCatching { mkdirs() } }
    private val current = File(dir, "app.log")
    private val previous = File(dir, "app.1.log")
    private val exported = File(dir, "OrangeCloud-logs.txt")
    private val maxBytes = 256L * 1024   // 单文件上限，两代约 512KB
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    @Synchronized
    fun append(level: String, category: String, message: String) {
        try {
            if (current.exists() && current.length() > maxBytes) rotate()
            current.appendText("${LocalDateTime.now().format(fmt)} [$level] [$category] $message\n")
        } catch (_: Exception) {
            // 日志写入失败绝不影响主流程
        }
    }

    private fun rotate() {
        runCatching {
            if (previous.exists()) previous.delete()
            current.renameTo(previous)
        }
    }

    /** 合并上一代 + 当前写到导出文件返回；无内容时返回 null。供反馈附件用。 */
    @Synchronized
    fun exportedFile(): File? {
        val text = buildString {
            if (previous.exists()) runCatching { append(previous.readText()) }
            if (current.exists()) runCatching { append(current.readText()) }
        }
        if (text.isBlank()) return null
        return runCatching { exported.writeText(text); exported }.getOrNull()
    }

    @Synchronized
    fun clear() {
        runCatching { current.delete(); previous.delete(); exported.delete() }
    }
}
