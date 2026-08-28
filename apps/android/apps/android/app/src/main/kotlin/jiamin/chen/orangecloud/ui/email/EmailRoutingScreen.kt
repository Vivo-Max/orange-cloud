package jiamin.chen.orangecloud.ui.email

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.data.model.EmailDestinationAddress
import jiamin.chen.orangecloud.data.model.EmailRoutingRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailRoutingScreen(
    onBack: () -> Unit,
    viewModel: EmailRoutingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddRule by remember { mutableStateOf(false) }
    var showAddAddress by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<EmailRoutingRule?>(null) }
    var addressToDelete by remember { mutableStateOf<EmailDestinationAddress?>(null) }

    val savedMsg = stringResource(R.string.email_saved)
    val deletedMsg = stringResource(R.string.email_deleted)
    val addrMsg = stringResource(R.string.email_address_added)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EmailEvent.Saved -> { showAddRule = false; snackbarHostState.showSnackbar(savedMsg) }
                EmailEvent.AddressAdded -> { showAddAddress = false; snackbarHostState.showSnackbar(addrMsg) }
                EmailEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is EmailEvent.Error -> snackbarHostState.showSnackbar(event.message ?: "")
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = stringResource(R.string.email_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                when {
                    state.missingScope ->
                        SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                    state.settings == null && state.isLoading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            SettingsCard(
                                enabled = state.settings?.isEnabled ?: false,
                                status = state.settings?.status,
                                canWrite = state.canWrite,
                                onToggle = { viewModel.setEnabled(it) },
                            )
                        }
                        item { SectionHeader(stringResource(R.string.email_rules_section), onSky) }
                        if (state.rules.isEmpty()) {
                            item { EmptyHint(stringResource(R.string.email_rules_empty)) }
                        } else {
                            items(state.rules, key = { it.id }) { rule ->
                                RuleRow(rule, state.canWrite) { ruleToDelete = rule }
                            }
                        }
                        if (state.canWrite) {
                            item { AddRowButton(stringResource(R.string.email_add_rule)) { showAddRule = true } }
                        }
                        item { SectionHeader(stringResource(R.string.email_addresses_section), onSky) }
                        if (state.addresses.isEmpty()) {
                            item { EmptyHint(stringResource(R.string.email_addresses_empty)) }
                        } else {
                            items(state.addresses, key = { it.id }) { addr ->
                                AddressRow(addr, viewModel.canAddAddress) { addressToDelete = addr }
                            }
                        }
                        if (viewModel.canAddAddress) {
                            item { AddRowButton(stringResource(R.string.email_add_address)) { showAddAddress = true } }
                        }

                        // 规则决定往哪转，抑制决定不往哪转——
                        // 「规则配了却收不到」多半就是命中了这里，所以要能看见
                        if (state.suppressionsAvailable) {
                            item { SectionHeader(stringResource(R.string.email_suppression_section), onSky) }
                            if (state.suppressions.isEmpty()) {
                                item { EmptyHint(stringResource(R.string.email_suppression_empty)) }
                            } else {
                                items(state.suppressions, key = { "sup-${it.id}" }) { item ->
                                    SuppressionRow(item, state.canWrite) { viewModel.removeSuppression(item) }
                                }
                            }
                            item { EmptyHint(stringResource(R.string.email_suppression_hint)) }
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    if (showAddRule) {
        ModalBottomSheet(
            onDismissRequest = { if (!state.isSaving) showAddRule = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AddRuleForm(
                isSaving = state.isSaving,
                destinations = state.addresses.filter { it.isVerified }.map { it.email },
                onSave = { match, dest, name -> viewModel.createForwardRule(name, match, dest) },
            )
        }
    }

    if (showAddAddress) {
        ModalBottomSheet(
            onDismissRequest = { if (!state.isSaving) showAddAddress = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AddAddressForm(isSaving = state.isSaving, onSave = { viewModel.addAddress(it) })
        }
    }

    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text(stringResource(R.string.email_delete_rule_title)) },
            text = { Text(stringResource(R.string.email_delete_rule_msg)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRule(rule); ruleToDelete = null }) {
                    Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { ruleToDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    addressToDelete?.let { addr ->
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text(stringResource(R.string.email_delete_address_title)) },
            text = { Text(addr.email) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAddress(addr); addressToDelete = null }) {
                    Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { addressToDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSky: Color) {
    Text(title, color = onSky, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun SettingsCard(enabled: Boolean, status: String?, canWrite: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.email_enable), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                status?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Switch(checked = enabled, onCheckedChange = onToggle, enabled = canWrite)
        }
    }
}

@Composable
private fun RuleRow(rule: EmailRoutingRule, canWrite: Boolean, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    rule.name?.takeIf { it.isNotBlank() } ?: (rule.matchAddress ?: stringResource(R.string.email_catch_all)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val dest = rule.actions.firstOrNull()?.value?.joinToString(", ").orEmpty()
                Text(
                    "${rule.matchAddress ?: stringResource(R.string.email_catch_all)} → $dest",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (canWrite) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AddressRow(addr: EmailDestinationAddress, canWrite: Boolean, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = OcOrange)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(addr.email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(if (addr.isVerified) R.string.email_verified else R.string.email_unverified),
                    fontSize = 12.sp,
                    color = if (addr.isVerified) OcSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canWrite) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AddRowButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = OcOrange)
        Spacer(Modifier.width(8.dp))
        Text(label, color = OcOrange)
    }
}

@Composable
private fun AddRuleForm(
    isSaving: Boolean,
    destinations: List<String>,
    onSave: (matchAddress: String, destination: String, name: String?) -> Unit,
) {
    var matchAddress by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf(destinations.firstOrNull().orEmpty()) }
    var name by rememberSaveable { mutableStateOf("") }
    val canSave = matchAddress.isNotBlank() && destination.isNotBlank() && !isSaving

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.email_add_rule), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(value = matchAddress, onValueChange = { matchAddress = it }, label = { Text(stringResource(R.string.email_field_match)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text(stringResource(R.string.email_field_destination)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        if (destinations.isNotEmpty()) {
            Text(stringResource(R.string.email_destination_hint, destinations.joinToString(", ")), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.email_field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onSave(matchAddress, destination, name.takeIf { it.isNotBlank() }) },
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) { CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(stringResource(R.string.dns_save))
        }
    }
}

@Composable
private fun AddAddressForm(isSaving: Boolean, onSave: (email: String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    val canSave = email.isNotBlank() && !isSaving
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.email_add_address), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.email_add_address_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.email_field_address)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onSave(email) },
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) { CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(stringResource(R.string.dns_save))
        }
    }
}

@Composable
private fun SuppressionRow(
    item: jiamin.chen.orangecloud.data.model.EmailSuppression,
    canWrite: Boolean,
    onLift: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.email ?: item.id,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(suppressionReasonLabel(item.reason)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canWrite) {
                TextButton(onClick = onLift) { Text(stringResource(R.string.email_suppression_lift)) }
            }
        }
    }
}

private fun suppressionReasonLabel(reason: String?) = when (reason) {
    "hard_bounce" -> R.string.email_reason_hard_bounce
    "soft_bounce" -> R.string.email_reason_soft_bounce
    "complaint" -> R.string.email_reason_complaint
    "manual" -> R.string.email_reason_manual
    else -> R.string.hc_status_unknown
}

