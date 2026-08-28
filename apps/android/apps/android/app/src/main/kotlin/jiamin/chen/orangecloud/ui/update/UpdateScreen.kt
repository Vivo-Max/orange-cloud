package jiamin.chen.orangecloud.ui.update

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.BuildConfig
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.update.ApkInstaller
import jiamin.chen.orangecloud.core.update.UpdateChecker
import jiamin.chen.orangecloud.core.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checker: UpdateChecker,
    private val installer: ApkInstaller,
) : ViewModel() {
    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available: StateFlow<UpdateInfo?> = _available.asStateFlow()

    private val _installing = MutableStateFlow(false)
    val installing: StateFlow<Boolean> = _installing.asStateFlow()

    init {
        // 仅 sideload 的 direct 包需要自助更新；其余风味有商店更新 / 自编译，不轮询。
        if (BuildConfig.IS_DIRECT) {
            viewModelScope.launch { _available.value = checker.fetchLatest() }
        }
    }

    /** 本次关闭（非强制；下次启动再提醒）。 */
    fun dismiss() {
        if (!_installing.value) _available.value = null
    }

    /** 忽略此版本（持久化；更高版本仍会提示）。 */
    fun skip() {
        val info = _available.value ?: return
        viewModelScope.launch {
            checker.skip(info.versionCode)
            _available.value = null
        }
    }

    /**
     * 应用内安装：下载 + PackageInstaller。成功则交给系统安装界面（非强制更新顺手关掉弹窗）；
     * 失败回退到浏览器下载（[onFallback]）。
     */
    fun install(onFallback: (String) -> Unit) {
        val info = _available.value ?: return
        if (_installing.value) return
        viewModelScope.launch {
            _installing.value = true
            val ok = installer.downloadAndInstall(info.url)
            _installing.value = false
            when {
                !ok -> onFallback(info.url)
                !info.forced -> _available.value = null
                // 强制更新：保留弹窗，用户若取消系统安装界面仍被拦在更新页。
            }
        }
    }
}

/**
 * 发现新版本时的提示弹窗。
 * - 普通更新：「立即更新」+「忽略此版本」，点窗外 / 返回键 = 本次关闭。
 * - 强制更新（[UpdateInfo.forced]）：仅「立即更新」，不可取消。
 * - 下载中：转圈 + 「下载中…」，按钮禁用、不可取消。
 */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    installing: Boolean,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelable = !info.forced && !installing
    AlertDialog(
        onDismissRequest = { if (cancelable) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable,
        ),
        title = { Text(stringResource(R.string.update_title, info.versionName)) },
        text = {
            if (installing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.update_downloading),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            } else {
                Text(info.note ?: stringResource(R.string.update_message))
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate, enabled = !installing) {
                Text(stringResource(R.string.update_now))
            }
        },
        dismissButton = {
            if (!info.forced && !installing) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
                TextButton(onClick = onSkip) { Text(stringResource(R.string.update_skip)) }
            }
        },
    )
}
