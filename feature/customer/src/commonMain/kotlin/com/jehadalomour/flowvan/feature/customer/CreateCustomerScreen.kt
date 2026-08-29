package com.jehadalomour.flowvan.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.FvButton
import com.jehadalomour.flowvan.core.designsystem.components.FvNotice
import com.jehadalomour.flowvan.core.designsystem.components.FvSectionLabel
import com.jehadalomour.flowvan.core.designsystem.components.FvTone
import com.jehadalomour.flowvan.core.designsystem.components.ReportTopBar
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.ic_check_circle
import com.jehadalomour.flowvan.core.designsystem.resources.ic_map
import com.jehadalomour.flowvan.core.designsystem.resources.ic_warning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * عميل جديد — a form the rep fills standing in the shop doorway.
 *
 * Three sections, each its own bordered block: من هو العميل, أين هو, وثيقته. The
 * document used to live inside the location card, which made "وثيقة العميل
 * (إلزامي)" read as part of capturing GPS — and it is the one field that decides
 * whether the save is even accepted.
 *
 * Save is pinned to the bottom rather than scrolling with the form: it is the
 * only action here, and its enabled state is the screen's answer to "am I done".
 */
/**
 * Prefilled from a customer-search result: name, phone and location seeded, and
 * the lead id carried through so the saved customer records where it came from.
 */
@Composable
fun CreateCustomerScreen(
    prefill: CreateCustomerPrefill,
    onBack: () -> Unit,
    onSaved: (customerId: String) -> Unit,
) {
    val viewModel: CreateCustomerViewModel = koinViewModel { parametersOf(prefill) }
    CreateCustomerScreen(onBack = onBack, onSaved = onSaved, viewModel = viewModel)
}

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

        ReportTopBar(title = "عميل جديد", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            // Said BEFORE the form, not after the save. Filling this in means
            // photographing a document — a rep who learns only from the answer
            // that their shop is not open for business yet has already spent
            // the visit on it, in front of the shopkeeper.
            if (state.willNeedApproval && !state.awaitingApproval &&
                state.approvalDecision == null
            ) {
                FvNotice(
                    title = "هذا العميل يحتاج موافقة الإدارة",
                    body = "سيُرسل للمراجعة عند الحفظ، ولا يمكن البيع له قبل الاعتماد.",
                    tone = FvTone.Warning,
                    icon = painterResource(Res.drawable.ic_warning),
                )
            }

            // ── Who ─────────────────────────────────────────────────────────────
            FormSection(
                title = "بيانات العميل",
                trailing = {
                    Text("إلزامي", color = Fv.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                },
            ) {
                FieldLabel("اسم العميل")
                Spacer(Modifier.height(6.dp))
                Field(
                    value = state.name,
                    onChange = { viewModel.onEvent(CreateCustomerEvent.NameChanged(it)) },
                    placeholder = "مثال: سوبرماركت السلام",
                )
                Spacer(Modifier.height(12.dp))
                FieldLabel("رقم الهاتف")
                Spacer(Modifier.height(6.dp))
                Field(
                    value = state.phone,
                    onChange = { viewModel.onEvent(CreateCustomerEvent.PhoneChanged(it)) },
                    placeholder = "07xxxxxxxx",
                    keyboard = KeyboardType.Phone,
                )
            }

            // ── Where ───────────────────────────────────────────────────────────
            LocationSection(state, viewModel)

            // ── Proof ───────────────────────────────────────────────────────────
            DocumentSection(state, viewModel)

            // ── What happened ───────────────────────────────────────────────────
            // The rep stays here until the office decides. Sending them back to a
            // list with no idea whether the customer exists is what makes them
            // phone the office.
            if (state.awaitingApproval) {
                FvNotice(
                    title = "بانتظار موافقة المشرف…",
                    body = "ابقَ على هذه الشاشة، سيتم إعلامك فور الاعتماد.",
                    tone = FvTone.Warning,
                    busy = true,
                )
            }

            when (state.approvalDecision) {
                ApprovalDecision.Approved -> FvNotice(
                    title = "تم اعتماد العميل",
                    body = "يمكنك الآن البيع لهذا العميل.",
                    tone = FvTone.Success,
                    icon = painterResource(Res.drawable.ic_check_circle),
                )
                // Previously this arrived as a bare red line indistinguishable
                // from a validation message, on a form whose save button had
                // silently stopped working. It is a decision, so it looks like one.
                ApprovalDecision.Rejected -> FvNotice(
                    title = "تم رفض العميل",
                    body = state.errorAr ?: "راجع المشرف قبل إعادة المحاولة.",
                    tone = FvTone.Danger,
                    icon = painterResource(Res.drawable.ic_warning),
                )
                null -> state.errorAr?.let { msg ->
                    FvNotice(
                        title = msg,
                        tone = FvTone.Danger,
                        icon = painterResource(Res.drawable.ic_warning),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        FvButton(
            label = "حفظ العميل",
            onClick = { if (state.canSave) viewModel.onEvent(CreateCustomerEvent.Save) },
            enabled = state.canSave,
            busy = state.isSaving,
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ── Sections ──────────────────────────────────────────────────────────────────

/** A bordered white block with a heading — the form's unit of grouping. */
@Composable
private fun FormSection(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Fv.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, Fv.Border, RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FvSectionLabel(title, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun LocationSection(state: CreateCustomerState, viewModel: CreateCustomerViewModel) {
    FormSection(
        title = "الموقع",
        trailing = {
            Text("إلزامي", color = Fv.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        },
    ) {
        if (state.hasLocation) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(Res.drawable.ic_map),
                    contentDescription = null,
                    tint = Fv.Green,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${fmt(state.lat)}، ${fmt(state.lng)}",
                    color = Fv.TextHigh,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "مسح",
                    color = Fv.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.onEvent(CreateCustomerEvent.ClearLocation) }
                        .padding(6.dp),
                )
            }
        } else {
            Text("التقط موقع العميل الحالي عبر GPS.", color = Fv.TextMid, fontSize = 13.sp)
        }

        state.locationErrorAr?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Fv.Amber, fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))
        FvButton(
            label = if (state.hasLocation) "إعادة التقاط الموقع" else "التقاط الموقع الحالي",
            onClick = { viewModel.onEvent(CreateCustomerEvent.CaptureLocation) },
            enabled = !state.isCapturingLocation,
            busy = state.isCapturingLocation,
            primary = false,
        )
    }
}

@Composable
private fun DocumentSection(state: CreateCustomerState, viewModel: CreateCustomerViewModel) {
    val picker = rememberDocumentPicker()
    val scope = rememberCoroutineScope()

    FormSection(
        title = "صور العميل",
        // At least one photo decides whether the save is accepted at all, so the
        // requirement is marked on the heading rather than in a sentence below it.
        trailing = {
            Text("إلزامي", color = Fv.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        },
    ) {
        Text(
            "صوّر المحل ووثيقته — صورة واحدة على الأقل، ويمكنك إضافة المزيد.",
            color = Fv.TextMid,
            fontSize = 13.sp,
        )

        if (state.photos.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            state.photos.forEach { photo ->
                PhotoRow(photo) {
                    viewModel.onEvent(CreateCustomerEvent.RemovePhoto(photo.localId))
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        state.documentErrorAr?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Fv.Amber, fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = Fv.Border)
        Spacer(Modifier.height(10.dp))

        // Both sources offered side by side: the rep either photographs the
        // shop now, or picks a shot already taken. Each pick adds another image.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FvButton(
                label = "الكاميرا",
                onClick = {
                    scope.launch {
                        picker.capture()?.let {
                            viewModel.onEvent(CreateCustomerEvent.DocumentPicked(it))
                        }
                    }
                },
                primary = false,
                modifier = Modifier.weight(1f),
            )
            FvButton(
                label = "المعرض",
                onClick = {
                    scope.launch {
                        picker.pickFromGallery()?.let {
                            viewModel.onEvent(CreateCustomerEvent.DocumentPicked(it))
                        }
                    }
                },
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One picked image: a status icon, its label, and a remove action. */
@Composable
private fun PhotoRow(photo: CustomerPhoto, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            photo.uploading -> CircularProgressIndicator(
                color = Fv.Blue,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
            photo.failed -> Icon(
                painterResource(Res.drawable.ic_warning),
                contentDescription = null,
                tint = Fv.Red,
                modifier = Modifier.size(18.dp),
            )
            else -> Icon(
                painterResource(Res.drawable.ic_check_circle),
                contentDescription = null,
                tint = Fv.Green,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                photo.uploading -> "جارٍ رفع ${photo.label}…"
                photo.failed -> "فشل رفع ${photo.label}"
                else -> "تم إرفاق ${photo.label}"
            },
            color = when {
                photo.failed -> Fv.Red
                photo.uploading -> Fv.TextMid
                else -> Fv.Green
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            "مسح",
            color = Fv.Red,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onRemove() }.padding(6.dp),
        )
    }
}

// ── Fields ────────────────────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        placeholder = { Text(placeholder, color = Fv.TextLow, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(6.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Fv.Surface,
            unfocusedContainerColor = Fv.Surface,
            focusedTextColor = Fv.TextHigh,
            unfocusedTextColor = Fv.TextHigh,
            cursorColor = Fv.Blue,
            focusedIndicatorColor = Fv.Blue,
            // Was SurfaceTop, which is almost the field's own fill — an invisible
            // outline outdoors, so the field read as a gap in the card.
            unfocusedIndicatorColor = Fv.Border,
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
