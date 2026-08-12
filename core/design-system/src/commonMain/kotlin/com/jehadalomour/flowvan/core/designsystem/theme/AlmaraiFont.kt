package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.almarai_bold
import com.jehadalomour.flowvan.core.designsystem.resources.almarai_extrabold
import com.jehadalomour.flowvan.core.designsystem.resources.almarai_regular
import org.jetbrains.compose.resources.Font

/**
 * Almarai (المراعي) — the interface family for the whole app.
 *
 * Bundled rather than requested from the system. The platform Arabic default is
 * a different face on every handset the field uses, so a layout proved on one
 * phone reflows on the next; and these devices are frequently offline, where a
 * downloaded font is not an option at all.
 *
 * Three weights, no Light. At the sizes this app uses — 11sp labels read at
 * arm's length in sunlight — a 300 weight disappears, so it is not shipped
 * rather than merely discouraged.
 */
@Composable
fun almaraiFamily(): FontFamily = FontFamily(
    Font(Res.font.almarai_regular, FontWeight.Normal),
    Font(Res.font.almarai_bold, FontWeight.Bold),
    Font(Res.font.almarai_extrabold, FontWeight.ExtraBold),
)
