package jiamin.chen.orangecloud.ui.storage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.R2CatalogRepository
import jiamin.chen.orangecloud.data.repository.R2SqlRepository
import jiamin.chen.orangecloud.data.repository.R2SqlResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 展示行数上限（查询不截断，展示截断——扫描量在服务端已发生，对齐 D1 防线）。 */
private const val MAX_ROWS = 500

data class R2SqlUiState(
    val tableRefs: List<String> = emptyList(),
    val result: R2SqlResult? = null,
    val isRunning: Boolean = false,
    /** 401/403：独立主机对 OAuth 的接受度未官方明说，给定向指引 */
    val authRejected: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class R2SqlViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val sqlRepository: R2SqlRepository,
    private val catalogRepository: R2CatalogRepository,
) : ViewModel() {

    private val bucket: String = checkNotNull(savedStateHandle["bucket"])

    private val _uiState = MutableStateFlow(R2SqlUiState())
    val uiState: StateFlow<R2SqlUiState> = _uiState.asStateFlow()

    init {
        // 拉命名空间与各自的表，作为点按插入的引用芯片（失败静默，不挡查询输入）
        viewModelScope.launch {
            runCatching {
                val accountId = accountId()
                val refs = mutableListOf<String>()
                for (namespace in catalogRepository.namespaces(accountId, bucket)) {
                    catalogRepository.tables(accountId, bucket, namespace.sqlName).forEach { refs += it.sqlName }
                }
                _uiState.update { it.copy(tableRefs = refs) }
            }
        }
    }

    fun run(sql: String) {
        val trimmed = sql.trim()
        if (trimmed.isEmpty() || _uiState.value.isRunning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, error = null, authRejected = false) }
            try {
                val result = sqlRepository.query(accountId(), bucket, trimmed)
                _uiState.update { it.copy(result = result) }
            } catch (e: ApiError.Unauthorized) {
                _uiState.update { it.copy(authRejected = true) }
            } catch (e: ApiError.Http) {
                if (e.status == 401 || e.status == 403) _uiState.update { it.copy(authRejected = true) }
                else _uiState.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun accountId(): String = accountStore.selectedAccountId.value ?: error("no account")
}

@Composable
fun R2SqlQueryScreen(
    onBack: () -> Unit,
    viewModel: R2SqlViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    var sql by remember { mutableStateOf("") }

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = "R2 SQL",
                onSky = onSky,
                isLoading = state.isRunning,
                onRefresh = { viewModel.run(sql) },
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = sql,
                    onValueChange = { sql = it },
                    placeholder = { Text("SELECT * FROM namespace.table LIMIT 100", fontFamily = FontFamily.Monospace) },
                    minLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.tableRefs.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.tableRefs.forEach { table ->
                            AssistChip(
                                onClick = {
                                    sql = if (sql.isBlank()) "SELECT * FROM $table LIMIT 100" else "$sql $table"
                                },
                                label = { Text(table, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.r2sql_billing_hint),
                    fontSize = 12.sp,
                    color = onSky.copy(alpha = 0.75f),
                )

                Button(
                    onClick = { viewModel.run(sql) },
                    enabled = sql.isNotBlank() && !state.isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.width(18.dp))
                    } else {
                        Text(stringResource(R.string.r2sql_run), fontWeight = FontWeight.SemiBold)
                    }
                }

                if (state.authRejected) {
                    Text(stringResource(R.string.r2sql_auth_rejected), fontSize = 13.sp, color = Color(0xFFE5484D))
                }
                state.error?.let {
                    Text(it, fontSize = 13.sp, color = Color(0xFFE5484D))
                }

                state.result?.let { result ->
                    if (result.rows.isEmpty()) {
                        Text(stringResource(R.string.r2sql_no_rows), fontSize = 14.sp, color = onSky)
                    } else {
                        ResultTable(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultTable(result: R2SqlResult) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.r2sql_result_summary, result.rows.size, result.columns.size),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 双轴滚动的简易网格：外层页面纵滚，这里横滚（对齐 D1 防线，仅展示前 MAX_ROWS 行）
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        result.columns.forEach { column ->
                            Text(
                                column,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(140.dp),
                                maxLines = 1,
                            )
                        }
                    }
                    result.rows.take(MAX_ROWS).forEach { row ->
                        Row {
                            row.forEach { cell ->
                                Text(
                                    cell,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(140.dp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            if (result.rows.size > MAX_ROWS) {
                Text(
                    stringResource(R.string.r2sql_truncated, MAX_ROWS, result.rows.size),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
