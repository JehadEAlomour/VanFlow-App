package com.jehadalomour.flowvan.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.shared.domain.model.AppTheme
import com.jehadalomour.flowvan.shared.domain.model.TaxType
import com.jehadalomour.flowvan.shared.presentation.feature.settings.SettingsEvent
import com.jehadalomour.flowvan.shared.presentation.feature.settings.SettingsViewModel
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbar.showSnackbar("تم حفظ الإعدادات")
            viewModel.onEvent(SettingsEvent.DismissSaved)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                item {
                    SettingsTopBar(onBack = onBack)
                }

                // ── Appearance ──────────────────────────────────────────────
                item { SectionHeader("المظهر واللغة") }

                item {
                    SettingsCard {
                        SegmentedRow(
                            label = "المظهر",
                            options = listOf(
                                AppTheme.SYSTEM to "تلقائي",
                                AppTheme.LIGHT to "فاتح",
                                AppTheme.DARK to "داكن",
                            ),
                            selected = state.theme,
                            onSelect = { viewModel.onEvent(SettingsEvent.ThemeChanged(it)) },
                        )
                        SettingsDivider()
                        SegmentedRow(
                            label = "اللغة",
                            options = listOf(
                                AppLanguage.AR to "العربية",
                                AppLanguage.EN to "English",
                            ),
                            selected = state.language,
                            onSelect = { viewModel.onEvent(SettingsEvent.LanguageChanged(it)) },
                        )
                    }
                }

                // ── Tax ─────────────────────────────────────────────────────
                item { SectionHeader("الضريبة") }

                item {
                    SettingsCard {
                        SegmentedRow(
                            label = "نوع الضريبة",
                            options = listOf(
                                TaxType.EXCLUDED_TAX to "غير شاملة",
                                TaxType.INCLUDED_TAX to "شاملة",
                            ),
                            selected = state.taxType,
                            onSelect = { viewModel.onEvent(SettingsEvent.TaxTypeChanged(it)) },
                        )
                    }
                }

                // ── Connection ──────────────────────────────────────────────
                item { SectionHeader("الاتصال") }

                item {
                    SettingsCard {
                        SettingsTextField(
                            label = "عنوان IP",
                            value = state.ipAddress,
                            placeholder = "192.168.1.100",
                            keyboardType = KeyboardType.Uri,
                            onChange = { viewModel.onEvent(SettingsEvent.IpAddressChanged(it)) },
                        )
                    }
                }

                // ── Salesman ────────────────────────────────────────────────
                item { SectionHeader("بيانات المندوب") }

                item {
                    SettingsCard {
                        SettingsTextField(
                            label = "رقم المندوب",
                            value = state.salesmanNumber,
                            placeholder = "SM-001",
                            keyboardType = KeyboardType.Text,
                            onChange = { viewModel.onEvent(SettingsEvent.SalesmanNumberChanged(it)) },
                        )
                        SettingsDivider()
                        SettingsTextField(
                            label = "الفرع",
                            value = state.branch,
                            placeholder = "الفرع الرئيسي",
                            keyboardType = KeyboardType.Text,
                            onChange = { viewModel.onEvent(SettingsEvent.BranchChanged(it)) },
                        )
                    }
                }

                // ── Voucher Limits ──────────────────────────────────────────
                item { SectionHeader("حدود الفواتير") }

                item {
                    SettingsCard {
                        SettingsTextField(
                            label = "أقصى رقم فاتورة بيع",
                            value = state.maxSaleVoucherNumber,
                            placeholder = "9999",
                            keyboardType = KeyboardType.Number,
                            onChange = { viewModel.onEvent(SettingsEvent.MaxSaleVoucherChanged(it)) },
                        )
                        SettingsDivider()
                        SettingsTextField(
                            label = "أقصى رقم فاتورة مرتجع",
                            value = state.maxReturnVoucherNumber,
                            placeholder = "9999",
                            keyboardType = KeyboardType.Number,
                            onChange = { viewModel.onEvent(SettingsEvent.MaxReturnVoucherChanged(it)) },
                        )
                        SettingsDivider()
                        SettingsTextField(
                            label = "أقصى رقم طلب مسبق",
                            value = state.maxOrderVoucherNumber,
                            placeholder = "9999",
                            keyboardType = KeyboardType.Number,
                            onChange = { viewModel.onEvent(SettingsEvent.MaxOrderVoucherChanged(it)) },
                        )
                    }
                }

                // ── Permissions ─────────────────────────────────────────────
                item { SectionHeader("صلاحيات المندوب") }

                item {
                    SettingsCard {
                        ToggleRow(
                            label = "تعديل السعر",
                            sublabel = "السماح بتغيير سعر المنتج عند البيع",
                            checked = state.canEditPrice,
                            onCheckedChange = { viewModel.onEvent(SettingsEvent.CanEditPriceChanged(it)) },
                        )
                        SettingsDivider()
                        ToggleRow(
                            label = "وضع عدم الاتصال",
                            sublabel = "العمل بدون اتصال بالإنترنت",
                            checked = state.offlineModeEnabled,
                            onCheckedChange = { viewModel.onEvent(SettingsEvent.OfflineModeChanged(it)) },
                        )
                    }
                }

                // ── Save Button ─────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2C6FE4), Color(0xFF1A4FBF))
                                )
                            )
                            .clickable { viewModel.onEvent(SettingsEvent.Save) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "حفظ الإعدادات",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        ) { data ->
            Snackbar(
                containerColor = Fv.Green,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(data.visuals.message, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(Res.drawable.ic_back),
                contentDescription = null,
                tint = Fv.TextHigh,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            "الإعدادات",
            color = Fv.TextHigh,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(48.dp))
    }
}

// ── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = Fv.TextMid,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 6.dp),
    )
}

// ── Card wrapper ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Fv.Surface)
            .border(1.dp, Fv.Border, RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(Fv.Border),
    )
}

// ── Segmented option row ─────────────────────────────────────────────────────

@Composable
private fun <T> SegmentedRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (value, displayName) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Fv.Blue else Fv.SurfaceHigh)
                        .border(1.dp, if (isSelected) Fv.Blue else Fv.Border, RoundedCornerShape(8.dp))
                        .clickable { onSelect(value) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        displayName,
                        color = if (isSelected) Color.White else Fv.TextMid,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

// ── Text field row ───────────────────────────────────────────────────────────

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(label, color = Fv.TextMid, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Fv.TextLow, fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Fv.Blue,
                unfocusedBorderColor = Fv.Border,
                focusedTextColor = Fv.TextHigh,
                unfocusedTextColor = Fv.TextHigh,
                cursorColor = Fv.Blue,
                focusedContainerColor = Fv.SurfaceHigh,
                unfocusedContainerColor = Fv.SurfaceHigh,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

// ── Toggle row ───────────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sublabel, color = Fv.TextLow, fontSize = 11.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Fv.Blue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Fv.SurfaceTop,
                uncheckedBorderColor = Fv.Border,
            ),
        )
    }
}
