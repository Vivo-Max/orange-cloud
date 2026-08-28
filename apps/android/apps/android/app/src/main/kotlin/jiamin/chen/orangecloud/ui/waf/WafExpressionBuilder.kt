package jiamin.chen.orangecloud.ui.waf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.height
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.theme.OcOrange

/** 可建模的 WAF 字段（Wirefilter）。numeric 决定值不加引号。 */
private enum class WafField(val expr: String, val labelRes: Int, val numeric: Boolean = false) {
    IP_SRC("ip.src", R.string.wafb_field_ip),
    COUNTRY("ip.geoip.country", R.string.wafb_field_country),
    PATH("http.request.uri.path", R.string.wafb_field_path),
    HOST("http.host", R.string.wafb_field_host),
    METHOD("http.request.method", R.string.wafb_field_method),
    USER_AGENT("http.user_agent", R.string.wafb_field_ua),
    URI_FULL("http.request.full_uri", R.string.wafb_field_uri),
    THREAT("cf.threat_score", R.string.wafb_field_threat, numeric = true),
}

private enum class WafOp(val expr: String, val labelRes: Int) {
    EQ("eq", R.string.wafb_op_eq),
    NE("ne", R.string.wafb_op_ne),
    CONTAINS("contains", R.string.wafb_op_contains),
    GT("gt", R.string.wafb_op_gt),
    LT("lt", R.string.wafb_op_lt),
}

private class WafCondition(field: WafField, op: WafOp, value: String) {
    var field by mutableStateOf(field)
    var op by mutableStateOf(op)
    var value by mutableStateOf(value)

    fun toExpression(): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        val rhs = if (field.numeric) v.filter { it.isDigit() }.ifEmpty { return null } else "\"${v.replace("\"", "\\\"")}\""
        return "${field.expr} ${op.expr} $rhs"
    }
}

private fun buildExpression(conditions: List<WafCondition>, useAnd: Boolean): String {
    val parts = conditions.mapNotNull { it.toExpression() }
    if (parts.isEmpty()) return ""
    if (parts.size == 1) return parts.first()
    val joiner = if (useAnd) " and " else " or "
    return parts.joinToString(joiner) { "($it)" }
}

/** WAF 可视化表达式构建器内容（嵌入 ModalBottomSheet）。onApply 回传拼好的表达式。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WafExpressionBuilder(
    onApply: (String) -> Unit,
    aiViewModel: WafAiViewModel = hiltViewModel(),
) {
    val conditions = remember { mutableStateListOf(WafCondition(WafField.PATH, WafOp.CONTAINS, "")) }
    var useAnd by remember { mutableStateOf(true) }
    val preview = buildExpression(conditions, useAnd)
    val aiState by aiViewModel.state.collectAsStateWithLifecycle()

    // AI 生成成功 → 直接应用并关闭。
    LaunchedEffect(aiState.result) {
        aiState.result?.let { onApply(it); aiViewModel.consumeResult() }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.wafb_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

        if (aiState.available) {
            AiGenerateRow(isGenerating = aiState.isGenerating, error = aiState.error) { aiViewModel.generate(it) }
            Text(stringResource(R.string.wafb_or_manual), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        conditions.forEachIndexed { index, cond ->
            ConditionEditor(cond, showRemove = conditions.size > 1) { conditions.removeAt(index) }
        }

        TextButton(onClick = { conditions.add(WafCondition(WafField.IP_SRC, WafOp.EQ, "")) }) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = OcOrange)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.wafb_add_condition), color = OcOrange)
        }

        if (conditions.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = useAnd, onClick = { useAnd = true }, label = { Text(stringResource(R.string.wafb_and)) })
                FilterChip(selected = !useAnd, onClick = { useAnd = false }, label = { Text(stringResource(R.string.wafb_or)) })
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                preview.ifBlank { stringResource(R.string.wafb_preview_empty) },
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (preview.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp),
            )
        }

        Button(
            onClick = { onApply(preview) },
            enabled = preview.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.wafb_apply))
        }
    }
}

@Composable
private fun AiGenerateRow(isGenerating: Boolean, error: String?, onGenerate: (String) -> Unit) {
    var desc by remember { mutableStateOf("") }
    Surface(color = OcOrange.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.wafb_ai_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OcOrange)
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                placeholder = { Text(stringResource(R.string.wafb_ai_hint)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onGenerate(desc) },
                enabled = desc.isNotBlank() && !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.wafb_ai_generate))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionEditor(cond: WafCondition, showRemove: Boolean, onRemove: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldDropdown(cond, Modifier.weight(1f))
                if (showRemove) {
                    IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.wafb_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            OpDropdown(cond)
            OutlinedTextField(
                value = cond.value,
                onValueChange = { cond.value = it },
                label = { Text(stringResource(R.string.wafb_value)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldDropdown(cond: WafCondition, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = stringResource(cond.field.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.wafb_field)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WafField.entries.forEach { f ->
                DropdownMenuItem(text = { Text(stringResource(f.labelRes)) }, onClick = { cond.field = f; expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpDropdown(cond: WafCondition) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(cond.op.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.wafb_operator)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WafOp.entries.forEach { o ->
                DropdownMenuItem(text = { Text(stringResource(o.labelRes)) }, onClick = { cond.op = o; expanded = false })
            }
        }
    }
}
