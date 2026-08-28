package jiamin.chen.orangecloud.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.system.ResourceSort
import java.time.Instant

/** 页头排序按钮（Workers / Pages 等资源列表通用），当前选中项打勾。 */
@Composable
fun SortMenuButton(
    sort: ResourceSort,
    onSky: Color,
    onSelect: (ResourceSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = stringResource(R.string.sort_label), tint = onSky)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                ResourceSort.NAME to R.string.sort_default,
                ResourceSort.CREATED to R.string.sort_created,
                ResourceSort.MODIFIED to R.string.sort_modified,
            ).forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = { onSelect(option); expanded = false },
                    trailingIcon = if (sort == option) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** 按可选 ISO8601 日期串排（解析失败沉底）。名称序保持列表原有顺序。 */
fun <T> ResourceSort.sorted(items: List<T>, created: (T) -> String?, modified: (T) -> String?): List<T> = when (this) {
    ResourceSort.NAME -> items
    ResourceSort.CREATED -> items.sortedByDescending { parseCfDate(created(it)) }
    ResourceSort.MODIFIED -> items.sortedByDescending { parseCfDate(modified(it)) }
}

/** CF 返回 6 位小数的 ISO8601（如 2026-03-22T20:05:13.916883Z），Instant.parse 可直接吃。 */
private fun parseCfDate(value: String?): Long =
    value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L
