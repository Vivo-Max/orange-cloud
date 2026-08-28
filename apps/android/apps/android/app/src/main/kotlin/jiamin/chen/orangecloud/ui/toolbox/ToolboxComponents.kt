package jiamin.chen.orangecloud.ui.toolbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.data.repository.RdapUnsupportedException

/** 工具的错误类型（VM 持有，UI 映射到 strings.xml）。 */
enum class ToolErrorKind { INPUT, NETWORK, UNSUPPORTED, NOT_FOUND, GENERIC }

/** 异常 → 工具错误类型（VM 不直接碰 strings.xml）。 */
fun Throwable.toToolErrorKind(): ToolErrorKind = when (this) {
    is RdapUnsupportedException -> ToolErrorKind.UNSUPPORTED
    is ApiError.Network -> ToolErrorKind.NETWORK
    is ApiError.Http -> if (status == 404) ToolErrorKind.NOT_FOUND else ToolErrorKind.NETWORK
    is IllegalArgumentException -> ToolErrorKind.INPUT
    else -> ToolErrorKind.GENERIC
}

@Composable
fun toolErrorText(kind: ToolErrorKind): String = stringResource(
    when (kind) {
        ToolErrorKind.INPUT -> R.string.tool_err_input
        ToolErrorKind.NETWORK -> R.string.tool_err_network
        ToolErrorKind.UNSUPPORTED -> R.string.tool_err_unsupported
        ToolErrorKind.NOT_FOUND -> R.string.tool_err_not_found
        ToolErrorKind.GENERIC -> R.string.error_generic
    },
)

/** 工具页统一头（返回 + 标题），不带刷新（工具靠输入栏驱动）。 */
@Composable
fun ToolHeader(title: String, onSky: Color, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = onSky)
        }
        Text(title, color = onSky, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

/** 输入栏 + 运行按钮（IME「前往」也触发运行）。 */
@Composable
fun ToolInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isLoading: Boolean,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Uri,
    enabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, maxLines = 1) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (enabled) onRun() }),
        )
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onRun,
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(56.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.tool_run), modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** 结果卡片（圆角玻璃容器）。 */
@Composable
fun ToolResultCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp), content = content)
    }
}

/** 「标签 : 值」行，值可长按之外提供复制按钮。 */
@Composable
fun ToolFieldRow(label: String, value: String, mono: Boolean = false, copyable: Boolean = true) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (copyable && value.isNotBlank()) {
            IconButton(onClick = { copyToClipboard(context, value) }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.tool_copy),
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 居中提示文案（空态 / 错误）。 */
@Composable
fun ToolHint(text: String, onSky: Color) {
    Text(
        text,
        color = onSky.copy(alpha = 0.8f),
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
    )
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Orange Cloud", text))
    Toast.makeText(context, context.getString(R.string.tool_copied), Toast.LENGTH_SHORT).show()
}
