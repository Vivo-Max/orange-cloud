package jiamin.chen.orangecloud.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** 复制纯文本到系统剪贴板（用平台 API，不引 Compose 已弃用的 LocalClipboardManager）。 */
fun copyToClipboard(context: Context, text: String, label: String = "orange-cloud") {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}
