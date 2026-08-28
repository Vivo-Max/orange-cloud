package jiamin.chen.orangecloud.ui.devplatform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Workers AI 文生图的位图解码与分享（UI 层工具，不碰网络）。
 */

/** 图片扩展名（只区分 JPEG 与其余按 PNG 处理，CF 文生图输出即这两类）。 */
private fun imageExtension(bytes: ByteArray): String {
    val isJpeg = bytes.size > 3 &&
        (bytes[0].toInt() and 0xFF) == 0xFF &&
        (bytes[1].toInt() and 0xFF) == 0xD8
    return if (isJpeg) "jpg" else "png"
}

private fun imageMime(extension: String): String = if (extension == "jpg") "image/jpeg" else "image/png"

/**
 * 降采样解码：先只读边界拿到原图尺寸，再按 2 的幂次 inSampleSize 缩到长边不超过 maxDimension，
 * 避免大图整张进内存触发 OOM。
 */
fun decodeSampledBitmap(bytes: ByteArray, maxDimension: Int = 2048): Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) return null
    var sample = 1
    while (longest / sample > maxDimension) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull()
}

/**
 * 分享生成的图片：写进应用缓存目录后经 FileProvider 授权，走系统分享面板（ACTION_SEND）。
 * 用分享而非直接写相册，是为了不申请存储权限、且让用户自行选择存相册 / 存文件 / 发给别人。
 * 落盘目录复用 file_paths.xml 已声明的缓存路径（r2 子树），不新增公共资源配置。
 * 返回 false 表示落盘失败或没有可接收的应用，由调用方给出提示。
 */
suspend fun shareGeneratedImage(context: Context, bytes: ByteArray, chooserTitle: String): Boolean {
    val extension = imageExtension(bytes)
    val uri = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "r2/ai").apply { mkdirs() }
            val file = File(dir, "orange-cloud-ai-${System.currentTimeMillis()}.$extension")
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    } ?: return false

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = imageMime(extension)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }.isSuccess
}
