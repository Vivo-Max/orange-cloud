package jiamin.chen.orangecloud.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.BuildConfig
import jiamin.chen.orangecloud.core.purchase.BillingGateway
import jiamin.chen.orangecloud.core.purchase.EntitlementStore
import jiamin.chen.orangecloud.core.purchase.RedeemOutcome
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProGateViewModel @Inject constructor(
    entitlementStore: EntitlementStore,
    private val billingGateway: jiamin.chen.orangecloud.core.purchase.BillingGateway,
) : ViewModel() {
    val isPro: StateFlow<Boolean> = entitlementStore.isPro
    fun purchase(activity: android.app.Activity, planId: String) = billingGateway.launchPurchase(activity, planId)

    private val _redeeming = MutableStateFlow(false)
    val redeeming: StateFlow<Boolean> = _redeeming.asStateFlow()
    private val _redeemOutcome = MutableStateFlow<RedeemOutcome?>(null)
    val redeemOutcome: StateFlow<RedeemOutcome?> = _redeemOutcome.asStateFlow()

    /** direct 风味：兑换激活码；成功后 EntitlementStore 翻 true，闸门反应式放行。 */
    fun redeem(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            _redeeming.value = true
            _redeemOutcome.value = billingGateway.redeem(code.trim())
            _redeeming.value = false
        }
    }

    private val _deactivating = MutableStateFlow(false)
    val deactivating: StateFlow<Boolean> = _deactivating.asStateFlow()

    /** direct 风味：反激活本设备，释放名额；成功后 EntitlementStore 翻 false，本屏反应式回到购买态。 */
    fun deactivate() {
        viewModelScope.launch {
            _deactivating.value = true
            billingGateway.deactivate()
            _deactivating.value = false
        }
    }
}

/** Pro 闸门：非 Pro 时以 Paywall 取代受限内容（六处闸门统一用）。 */
@Composable
fun ProGate(
    gateViewModel: ProGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val isPro by gateViewModel.isPro.collectAsStateWithLifecycle()
    if (isPro) content() else PaywallScreen()
}

@Composable
fun PaywallScreen(gateViewModel: ProGateViewModel = hiltViewModel()) {
    val isPro by gateViewModel.isPro.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    SkyBackground(phase = phase) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.size(24.dp))
            Icon(
                if (isPro) Icons.Outlined.Verified else Icons.Outlined.WorkspacePremium,
                contentDescription = null, tint = OcOrange, modifier = Modifier.size(56.dp),
            )
            Text(stringResource(R.string.paywall_title), color = onSky, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(if (isPro) R.string.settings_pro_unlocked else R.string.paywall_subtitle),
                color = onSky.copy(alpha = 0.7f), fontSize = 14.sp,
            )
            Spacer(Modifier.size(8.dp))
            listOf(
                R.string.paywall_feat_multi,
                R.string.paywall_feat_storage,
                R.string.paywall_feat_tail,
                R.string.paywall_feat_security,
                R.string.paywall_feat_analytics,
            ).forEach { FeatureLine(stringResource(it), onSky) }
            Spacer(Modifier.size(12.dp))
            if (isPro) {
                // 已解锁：上方对勾 + 「已解锁全部功能」+ 功能清单即为已购状态。
                // direct 风味额外提供「反激活」以释放本设备名额。
                if (BuildConfig.IS_DIRECT) DirectDeactivate(gateViewModel, onSky)
            } else if (BuildConfig.IS_DIRECT) {
                DirectPurchase(gateViewModel, activity, onSky)
            } else {
                // play：价格从 Play ProductDetails 动态取；oss 风味本屏不展示（isPro 恒真）。
                Button(
                    onClick = { activity?.let { gateViewModel.purchase(it, BillingGateway.PLAN_YEARLY) } },
                    colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.paywall_yearly))
                }
                Button(
                    onClick = { activity?.let { gateViewModel.purchase(it, BillingGateway.PLAN_MONTHLY) } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.paywall_monthly))
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { activity?.let { gateViewModel.purchase(it, BillingGateway.PLAN_LIFETIME) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.paywall_lifetime))
                }
                Text(stringResource(R.string.paywall_note), color = onSky.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}

/** direct 风味的购买区：买断按钮（打开 Web 售卖页）+ 激活码输入。 */
@Composable
private fun DirectPurchase(
    gateViewModel: ProGateViewModel,
    activity: android.app.Activity?,
    onSky: Color,
) {
    val redeeming by gateViewModel.redeeming.collectAsStateWithLifecycle()
    val outcome by gateViewModel.redeemOutcome.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }

    Button(
        onClick = { activity?.let { gateViewModel.purchase(it, BillingGateway.PLAN_LIFETIME) } },
        colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.paywall_buy_lifetime))
    }
    Spacer(Modifier.size(6.dp))
    Text(stringResource(R.string.paywall_have_code), color = onSky.copy(alpha = 0.7f), fontSize = 13.sp)
    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.paywall_code_hint)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { gateViewModel.redeem(code) },
        enabled = code.isNotBlank() && !redeeming,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(if (redeeming) R.string.paywall_activating else R.string.paywall_activate))
    }
    outcome?.let { oc ->
        val msgRes = when (oc) {
            RedeemOutcome.SUCCESS -> R.string.redeem_ok
            RedeemOutcome.INVALID -> R.string.redeem_invalid
            RedeemOutcome.REVOKED -> R.string.redeem_revoked
            RedeemOutcome.DEVICE_LIMIT -> R.string.redeem_device_limit
            else -> R.string.redeem_network
        }
        Text(
            stringResource(msgRes),
            color = if (oc == RedeemOutcome.SUCCESS) OcOrange else onSky.copy(alpha = 0.85f),
            fontSize = 13.sp,
        )
    }
    Text(stringResource(R.string.paywall_note_direct), color = onSky.copy(alpha = 0.5f), fontSize = 12.sp)
}

/** direct 风味的反激活：释放本设备名额（带确认弹窗）。 */
@Composable
private fun DirectDeactivate(gateViewModel: ProGateViewModel, onSky: Color) {
    val deactivating by gateViewModel.deactivating.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    TextButton(onClick = { confirm = true }, enabled = !deactivating) {
        Text(stringResource(R.string.paywall_deactivate), color = onSky.copy(alpha = 0.7f))
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.paywall_deactivate)) },
            text = { Text(stringResource(R.string.paywall_deactivate_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    gateViewModel.deactivate()
                }) { Text(stringResource(R.string.paywall_deactivate)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FeatureLine(text: String, onSky: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = OcOrange, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = onSky.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}
