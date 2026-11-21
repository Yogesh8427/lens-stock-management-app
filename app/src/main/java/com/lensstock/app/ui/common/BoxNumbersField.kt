package com.lensstock.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A lens's stock is often split across 2-3 physical boxes at once — this lets several box codes
 * be attached to one lens instead of just a single box field. Each confirmed code becomes a
 * removable chip; the text field is for typing the next one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxNumbersField(
    boxNumbers: List<String>,
    onBoxNumbersChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }

    fun commitDraft() {
        val trimmed = draft.trim()
        if (trimmed.isNotEmpty() && trimmed !in boxNumbers) {
            onBoxNumbersChange(boxNumbers + trimmed)
        }
        draft = ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (boxNumbers.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                boxNumbers.forEach { box ->
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("Box $box") },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove box $box",
                                modifier = Modifier.width(16.dp).clickable { onBoxNumbersChange(boxNumbers - box) },
                            )
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconField(
                icon = Icons.Filled.Inventory2,
                placeholder = "Add a box number",
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
            )
            PillTonalButton(text = "Add", enabled = draft.isNotBlank(), onClick = ::commitDraft)
        }
    }
}

/**
 * Read-only context for the Add dialog: which boxes this exact power is already stocked in,
 * fetched from the existing stock row — not part of [boxNumbers] above (which is only for *new*
 * box numbers this add should attach), and not removable here, since a stock write only ever
 * appends box numbers and can't retract one that's already on record.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlreadyInBoxesRow(boxNumbers: List<String>, modifier: Modifier = Modifier) {
    if (boxNumbers.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (boxNumbers.size > 1) "Already in these boxes" else "Already in this box",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            boxNumbers.forEach { box ->
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("Box $box") },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
