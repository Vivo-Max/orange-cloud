package jiamin.chen.orangecloud.ui.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

// MARK: - 预览上限（超限不下载整个对象，直接提示改用「下载并打开」）

/** 文本类对象应用内预览上限：10 MB。 */
const val R2_TEXT_PREVIEW_MAX_BYTES: Long = 10L * 1024 * 1024

/** 图片对象应用内预览上限：20 MB。 */
const val R2_IMAGE_PREVIEW_MAX_BYTES: Long = 20L * 1024 * 1024

/** 文本预览渲染字符上限，超出截断并提示。 */
const val R2_TEXT_PREVIEW_MAX_CHARS: Int = 80_000

/** 图片解码后的最长边上限（用 inSampleSize 降采样逼近），避免大图 OOM。 */
private const val R2_IMAGE_MAX_DIMENSION: Int = 2048

/** 搜索高亮的字符上限：超过只给匹配计数，不再构造 AnnotatedString。 */
private const val R2_HIGHLIGHT_MAX_CHARS: Int = 20_000

// MARK: - 预览内容模型

sealed interface R2Preview {
    /** 已降采样的位图。 */
    data class ImageContent(val bitmap: Bitmap) : R2Preview

    /** 文本正文；isJson = 已格式化的 JSON；truncated = 已截断。 */
    data class TextContent(val text: String, val isJson: Boolean, val truncated: Boolean) : R2Preview

    /** 对象过大，未下载。 */
    data class TooLarge(val size: Long) : R2Preview

    /** 类型不支持或解码失败。 */
    data object Unsupported : R2Preview

    /** 下载失败。 */
    data object Failed : R2Preview
}

data class R2PreviewState(
    val key: String? = null,
    val isLoading: Boolean = false,
    val content: R2Preview? = null,
)

// MARK: - 类型判定（contentType 优先，扩展名兜底）

private val R2_IMAGE_EXTS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif", "avif")

private val R2_TEXT_EXTS = setOf(
    "txt", "json", "jsonl", "ndjson", "xml", "html", "htm", "css", "js", "mjs", "cjs", "ts", "tsx", "jsx",
    "md", "markdown", "yml", "yaml", "toml", "ini", "conf", "cfg", "csv", "tsv", "log", "sql", "sh", "bash",
    "py", "rb", "go", "rs", "java", "kt", "kts", "swift", "c", "h", "cpp", "hpp", "properties", "env", "svg",
    "gitignore", "editorconfig",
)

private val R2_TEXT_MIME_EXACT = setOf(
    "application/json", "application/xml", "application/javascript", "application/x-javascript",
    "application/ecmascript", "application/x-ndjson", "application/sql", "application/x-sh",
    "application/x-yaml", "application/yaml", "application/toml", "image/svg+xml",
)

/** 未知二进制的通配 MIME：此时以扩展名为准。 */
private val R2_OPAQUE_MIME = setOf("application/octet-stream", "binary/octet-stream", "")

/** 归一 contentType：去掉 charset 等参数并小写。 */
private fun normalizedMime(contentType: String?): String =
    contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()

/** 对象末段扩展名（小写，无点）。 */
fun r2Extension(key: String): String =
    key.substringAfterLast('/').substringAfterLast('.', "").lowercase()

/** 是否按图片预览。SVG 归文本。 */
fun isImageKey(key: String, contentType: String?): Boolean {
    val mime = normalizedMime(contentType)
    if (mime !in R2_OPAQUE_MIME) {
        return mime.startsWith("image/") && mime != "image/svg+xml"
    }
    return r2Extension(key) in R2_IMAGE_EXTS
}

/** 是否按文本预览。 */
fun isTextKey(key: String, contentType: String?): Boolean {
    val mime = normalizedMime(contentType)
    if (mime !in R2_OPAQUE_MIME) {
        if (mime.startsWith("text/")) return true
        if (mime in R2_TEXT_MIME_EXACT) return true
        if (mime.endsWith("+json") || mime.endsWith("+xml")) return true
        return false
    }
    return r2Extension(key) in R2_TEXT_EXTS
}

/** 是否值得尝试 JSON 格式化（失败会自动退回纯文本）。 */
fun isJsonKey(key: String, contentType: String?): Boolean {
    val mime = normalizedMime(contentType)
    if (mime.contains("json")) return true
    return r2Extension(key) in setOf("json", "jsonl", "ndjson")
}

// MARK: - 解码

private val prettyJson = Json { prettyPrint = true }

/**
 * 先读 bounds 求 inSampleSize 再真正解码，最长边压到 [R2_IMAGE_MAX_DIMENSION] 以内，
 * 避免整张原图进堆导致 OOM。解码失败返回 null。
 */
fun decodeSampledBitmap(bytes: ByteArray, maxDimension: Int = R2_IMAGE_MAX_DIMENSION): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull()
}

/** 前 4 KB 内出现 NUL 字节即判为二进制，不当文本渲染。 */
private fun looksBinary(bytes: ByteArray): Boolean {
    val end = minOf(bytes.size, 4096)
    for (i in 0 until end) if (bytes[i] == 0.toByte()) return true
    return false
}

/**
 * 文本预览：UTF-8 解码 → （可选）JSON 美化 → 超长截断。
 * JSON 解析失败静默退回纯文本，不报错。
 */
fun decodeTextPreview(bytes: ByteArray, preferJson: Boolean): R2Preview {
    if (looksBinary(bytes)) return R2Preview.Unsupported
    val raw = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: return R2Preview.Unsupported
    var text = raw
    var isJson = false
    if (preferJson && raw.length <= R2_TEXT_PREVIEW_MAX_CHARS) {
        val pretty = runCatching {
            prettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(raw))
        }.getOrNull()
        if (pretty != null) {
            text = pretty
            isJson = true
        }
    }
    val truncated = text.length > R2_TEXT_PREVIEW_MAX_CHARS
    if (truncated) text = text.take(R2_TEXT_PREVIEW_MAX_CHARS)
    return R2Preview.TextContent(text, isJson, truncated)
}

/** 忽略大小写统计出现次数（不重叠）。 */
fun countMatches(text: String, query: String): Int {
    if (query.isEmpty()) return 0
    var index = text.indexOf(query, 0, ignoreCase = true)
    var count = 0
    while (index >= 0) {
        count++
        index = text.indexOf(query, index + query.length, ignoreCase = true)
    }
    return count
}

/** 命中处加背景色高亮；文本过长时直接返回原文，避免构造开销。 */
private fun highlighted(text: String, query: String, color: Color): AnnotatedString {
    if (query.isEmpty() || text.length > R2_HIGHLIGHT_MAX_CHARS) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        while (true) {
            val hit = text.indexOf(query, cursor, ignoreCase = true)
            if (hit < 0) break
            append(text, cursor, hit)
            pushStyle(SpanStyle(background = color))
            append(text, hit, hit + query.length)
            pop()
            cursor = hit + query.length
        }
        append(text, cursor, text.length)
    }
}

// MARK: - 预览 UI

/** 对象详情里的内联预览区块（图片 / JSON / 文本 / 超限 / 不支持）。 */
@Composable
fun R2PreviewPane(preview: R2PreviewState) {
    when {
        preview.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp, color = OcOrange)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.r2_preview_loading),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> when (val content = preview.content) {
            null -> Unit

            is R2Preview.ImageContent -> Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    bitmap = content.bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.r2_preview_title),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 320.dp).padding(8.dp),
                )
            }

            is R2Preview.TextContent -> TextPreview(content)

            is R2Preview.TooLarge -> PreviewNotice(
                stringResource(R.string.r2_preview_too_large, formatBytes(content.size)),
            )

            R2Preview.Unsupported -> PreviewNotice(stringResource(R.string.r2_preview_unsupported))

            R2Preview.Failed -> PreviewNotice(stringResource(R.string.r2_preview_failed))
        }
    }
}

@Composable
private fun PreviewNotice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TextPreview(content: R2Preview.TextContent) {
    var query by remember(content) { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val matches = remember(content, trimmedQuery) {
        if (trimmedQuery.isEmpty()) 0 else countMatches(content.text, trimmedQuery)
    }
    val highlightColor = OcOrange.copy(alpha = 0.35f)
    val display = remember(content, trimmedQuery, highlightColor) {
        highlighted(content.text, trimmedQuery, highlightColor)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.r2_preview_search)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        if (trimmedQuery.isNotEmpty()) {
            Text(
                if (matches > 0) {
                    stringResource(R.string.r2_preview_matches, matches)
                } else {
                    stringResource(R.string.r2_preview_no_match)
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (content.isJson) {
            Text(
                stringResource(R.string.r2_preview_json_formatted),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OcOrange,
            )
        }
        if (content.truncated) {
            Text(
                stringResource(R.string.r2_preview_truncated, R2_TEXT_PREVIEW_MAX_CHARS),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    display,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
