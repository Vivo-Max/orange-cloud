package jiamin.chen.orangecloud.core.logging

import android.util.Log
import java.io.File

/**
 * 客户端统一日志门面（对应 iOS Core/Logging/AppLog.swift）：每条日志同时进 logcat 与
 * App 内滚动日志文件（随「设置 → 帮助与反馈」作邮件附件）。按类别分流，方便排查。
 *
 * 用法：AppLog.network.info("GET /zones -> 200 (123ms)")。
 * 脱敏由调用方负责：消息里绝不含 token / 密钥值 / 完整 Authorization。
 */
object AppLog {

    enum class Category(val tag: String) {
        APP("app"), AUTH("auth"), NETWORK("network"),
        WEBSOCKET("websocket"), PURCHASE("purchase"), BACKGROUND("background")
    }

    @Volatile
    private var store: LogFileStore? = null

    /** 在 Application.onCreate 安装文件落地。未安装时仅进 logcat。 */
    fun install(store: LogFileStore) {
        this.store = store
    }

    /** 导出合并日志文件（反馈附件用）。 */
    fun exportedFile(): File? = store?.exportedFile()

    fun clear() = store?.clear() ?: Unit

    val app = Tag(Category.APP)
    val auth = Tag(Category.AUTH)
    val network = Tag(Category.NETWORK)
    val websocket = Tag(Category.WEBSOCKET)
    val purchase = Tag(Category.PURCHASE)
    val background = Tag(Category.BACKGROUND)

    class Tag(private val category: Category) {
        fun debug(message: String) = emit("debug", message)
        fun info(message: String) = emit("info", message)
        fun notice(message: String) = emit("notice", message)
        fun error(message: String) = emit("error", message)

        private fun emit(level: String, message: String) {
            when (level) {
                "error" -> Log.e(category.tag, message)
                "debug" -> Log.d(category.tag, message)
                else -> Log.i(category.tag, message)
            }
            store?.append(level, category.tag, message)
        }
    }
}
