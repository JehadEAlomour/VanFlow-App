package com.jehadalomour.flowvan.feature.print

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Asked once when a receipt is opened that has the same item on several units.
 *
 * Deliberately NOT dismissable by tapping outside: the two answers print different documents,
 * and a stray tap must not silently pick one. Both strings are localized (values/ + values-en/),
 * so the rep sees it in whichever language the app is running.
 */
@Composable
fun CompactLinesDialog(
    mergeableCount: Int,
    onMerge: () -> Unit,
    onKeepAll: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* a choice is required — see the note above */ },
        title = {
            Text(
                stringResource(Res.string.print_compact_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        },
        text = {
            Text(
                stringResource(Res.string.print_compact_body),
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onMerge) {
                Text(
                    "${stringResource(Res.string.print_compact_yes)} ($mergeableCount)",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepAll) {
                Text(stringResource(Res.string.print_compact_no))
            }
        },
    )
}
