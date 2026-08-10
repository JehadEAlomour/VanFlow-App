package com.jehadalomour.flowvan.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun CreateCustomerScreen(
    onBack: () -> Unit,
    onSaved: (customerId: String) -> Unit,
    viewModel: CreateCustomerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Navigate to the new customer's page once the backend returns it. On the
    // approval path, hold for a beat first — otherwise the "approved" card the
    // rep has been waiting on flashes past before they can read it.
    LaunchedEffect(state.savedCustomerId) {
        val id = state.savedCustomerId ?: return@LaunchedEffect
        if (state.approvalDecision == ApprovalDecision.Approved) delay(1_200)
        onSaved(id)
    }

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {
        // ── Top bar ─────────────────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), color = Fv.Surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Fv.SurfaceTop)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "عميل جديد",
                    color = Fv.TextHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Said BEFORE the form, not after the save. Filling this in means
            // photographing a document — a rep who learns only from the answer
            // that their shop is not open for business yet has already spent
            // the visit on it, in front of the shopkeeper.
            if (state.willNeedApproval && !state.awaitingApproval &&
                state.approvalDecision == null
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = Fv.SurfaceTop) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("ℹ️", fontSize = 16.sp)
                        Column {
                            Text(
                                "هذا العميل يحتاج موافقة الإدارة",
                                color = Fv.Amber,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "سيُرسل للمراجعة عند الحفظ، ولا يمكن البيع له قبل الاعتماد.",
                                color = Fv.TextMid,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            FieldLabel("اسم العميل")
            Field(
                value = state.name,
                onChange = { viewModel.onEvent(CreateCustomerEvent.NameChanged(it)) },
                placeholder = "مثال: سوبرماركت السلام",
            )

            FieldLabel("رقم الهاتف")
            Field(
                value = state.phone,
                onChange = { viewModel.onEvent(CreateCustomerEvent.PhoneChanged(it)) },
                placeholder = "07xxxxxxxx",
                keyboard = KeyboardType.Phone,
            )

            FieldLabel("الموقع")
            LocationCard(state, viewModel)

            state.errorAr?.let { msg ->
                Text(msg, color = Fv.Red, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(4.dp))

            // ── Save ────────────────────────────────────────────────────────────
            Surface(
                onClick = { if (state.canSave) viewModel.onEvent(CreateCustomerEvent.Save) },
                enabled = state.canSave,
                shape = RoundedCornerShape(12.dp),
                color = if (state.canSave) Fv.Blue else Fv.SurfaceTop,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            "حفظ العميل",
                            color = if (state.canSave) Color.White else Fv.TextMid,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationCard(state: CreateCustomerState, viewModel: CreateCustomerViewModel) {
    Surface(shape = RoundedCornerShape(12.dp), color = Fv.Surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.hasLocation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${fmt(state.lat)}، ${fmt(state.lng)}",
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "مسح",
                        color = Fv.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.onEvent(CreateCustomerEvent.ClearLocation) },
                    )
                }
            } else {
                Text(
                    "التقط موقع العميل الحالي عبر GPS.",
                    color = Fv.TextMid,
                    fontSize = 13.sp,
                )
            }

            state.locationErrorAr?.let { Text(it, color = Fv.Amber, fontSize = 12.sp) }

            Surface(
                onClick = { if (!state.isCapturingLocation) viewModel.onEvent(CreateCustomerEvent.CaptureLocation) },
                shape = RoundedCornerShape(10.dp),
                color = Fv.SurfaceTop,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isCapturingLocation) {
                        CircularProgressIndicator(color = Fv.Blue, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            if (state.hasLocation) "إعادة التقاط الموقع" else "التقاط الموقع الحالي",
                            color = Fv.Blue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // ── Document photo (required) ─────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            FieldLabel("وثيقة العميل (إلزامي)")

            val picker = rememberDocumentPicker()
            val scope = rememberCoroutineScope()

            when {
                state.isUploadingDocument -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(color = Fv.Blue, modifier = Modifier.size(16.dp))
                    Text("جارٍ رفع الصورة…", color = Fv.TextMid, fontSize = 13.sp)
                }

                state.hasDocument -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✓ تم إرفاق الوثيقة", color = Fv.Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "مسح",
                        color = Fv.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            viewModel.onEvent(CreateCustomerEvent.ClearDocument)
                        },
                    )
                }

                else -> Text(
                    "صوّر السجل التجاري أو هوية صاحب المحل — لا يمكن حفظ العميل بدونها.",
                    color = Fv.TextMid,
                    fontSize = 13.sp,
                )
            }

            state.documentErrorAr?.let { Text(it, color = Fv.Amber, fontSize = 12.sp) }

            // Both sources offered side by side: the rep either photographs the
            // paper now, or picks the shot they already took on the way in.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DocumentSourceButton(
                    label = "الكاميرا",
                    enabled = !state.isUploadingDocument,
                    modifier = Modifier.weight(1f),
                ) {
                    scope.launch {
                        picker.capture()?.let {
                            viewModel.onEvent(CreateCustomerEvent.DocumentPicked(it))
                        }
                    }
                }
                DocumentSourceButton(
                    label = "المعرض",
                    enabled = !state.isUploadingDocument,
                    modifier = Modifier.weight(1f),
                ) {
                    scope.launch {
                        picker.pickFromGallery()?.let {
                            viewModel.onEvent(CreateCustomerEvent.DocumentPicked(it))
                        }
                    }
                }
            }

            // The rep stays here until the office decides. Sending them back to a
            // list with no idea whether the customer exists is what makes them
            // phone the office.
            if (state.awaitingApproval) {
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Fv.SurfaceTop) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(color = Fv.Amber, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                "بانتظار موافقة المشرف…",
                                color = Fv.Amber,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "ابقَ على هذه الشاشة، سيتم إعلامك فور الاعتماد.",
                                color = Fv.TextMid,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            if (state.approvalDecision == ApprovalDecision.Approved) {
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Fv.SurfaceTop) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            "✓ تم اعتماد العميل",
                            color = Fv.Green,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("يمكنك الآن البيع لهذا العميل.", color = Fv.TextMid, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentSourceButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(10.dp),
        color = Fv.SurfaceTop,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = if (enabled) Fv.Blue else Fv.TextMid,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Fv.TextMid, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Fv.Surface,
            unfocusedContainerColor = Fv.Surface,
            focusedTextColor = Fv.TextHigh,
            unfocusedTextColor = Fv.TextHigh,
            cursorColor = Fv.Blue,
            focusedIndicatorColor = Fv.Blue,
            unfocusedIndicatorColor = Fv.SurfaceTop,
        ),
    )
}

private fun fmt(v: Double?): String {
    if (v == null) return "-"
    // Trim to ~5 decimal places without relying on platform String.format.
    val scaled = (v * 100000).toLong()
    val whole = scaled / 100000
    val frac = kotlin.math.abs(scaled % 100000).toString().padStart(5, '0')
    return "$whole.$frac"
}
