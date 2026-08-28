package jiamin.chen.orangecloud.ui.storage

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/** PRAGMA index_list 的一行：索引名 / 是否唯一 / 来源。 */
data class D1IndexInfo(
    val name: String,
    val unique: Boolean,
    /** SQLite 口径："c" = CREATE INDEX，"u" = UNIQUE 约束，"pk" = 主键。 */
    val origin: String,
    val partial: Boolean = false,
)

/** 单元格取值：缺失 / NULL → 空串；基础值 → 原文；其余 → JSON 文本。 */
private fun cellText(value: JsonElement?): String = when {
    value == null || value is JsonNull -> ""
    value is JsonPrimitive -> value.content
    else -> value.toString()
}

/**
 * RFC 4180 转义：含逗号 / 双引号 / 换行的字段用双引号包裹，内部 `"` 写成 `""`。
 * 前后空白也一并包裹，避免被表格软件吃掉。
 */
fun csvEscape(value: String): String {
    val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ||
        value != value.trim()
    return if (needsQuote) "\"" + value.replace("\"", "\"\"") + "\"" else value
}

/**
 * 把**当前已加载**的结果集转成 CSV（CRLF 行尾 + UTF-8 BOM，方便 Excel 正确识别中文）。
 * 不重新发查询，也不做任何补取。
 */
fun buildCsv(columns: List<String>, rows: List<Map<String, JsonElement>>): String = buildString {
    append('\uFEFF')
    append(columns.joinToString(",") { csvEscape(it) })
    append("\r\n")
    rows.forEach { row ->
        append(columns.joinToString(",") { csvEscape(cellText(row[it])) })
        append("\r\n")
    }
}

/** 导出文件名安全化（只留字母数字与 . _ -）。 */
fun sanitizeFileName(name: String, fallback: String): String {
    val cleaned = name.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_')
    return cleaned.ifEmpty { fallback }.take(48)
}
