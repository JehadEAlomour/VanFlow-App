package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Asked once when a receipt is opened that could fold rows together.
 *
 * Up to three answers, each printing a different document, so the dialog is deliberately NOT
 * dismissable by tapping outside — a stray tap must not silently pick one:
 *   - Merge units:        [unitCount] > 0 — several units of the SAME item become one row.
 *   - Merge alternatives: [altCount] > [unitCount] — same-priced DIFFERENT items also fold.
 *   - Keep all:           print every line.
 * Both merge buttons show how many lines that choice removes. All strings are localized.
 */
@Composable
fun CompactLinesDialog(
    unitCount: Int,
    altCount: Int,
    onMergeUnits: () -> Unit,
    onMergeAlternatives: () -> Unit,
    onKeepAll: () -> Unit,
) {
    val showUnits = unitCount > 0
    // Only offer the alternatives merge when it removes MORE than the plain unit merge —
    // otherwise the two buttons would do the same thing.
    val showAlts = altCount > unitCount

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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showAlts) {
                    TextButton(onClick = onMergeAlternatives) {
                        Text(
                            "${stringResource(Res.string.print_compact_alternatives)} ($altCount)",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (showUnits) {
                    TextButton(onClick = onMergeUnits) {
                        Text(
                            "${stringResource(Res.string.print_compact_yes)} ($unitCount)",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepAll) {
                Text(stringResource(Res.string.print_compact_no))
            }
        },
    )
}
