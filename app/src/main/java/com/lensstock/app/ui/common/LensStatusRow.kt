package com.lensstock.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lensstock.app.data.model.Lens
import com.lensstock.app.domain.PairFormat

/**
 * One full-width, status-colored row for a lens: power (+ axis/add), box number (only if set),
 * and a trailing quantity. Shared by Full Stock, To Order, and Search so they look identical.
 */
@Composable
fun LensStatusRow(lens: Lens, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val status = lens.stockStatus()
    val color = status.color()
    val shape = RoundedCornerShape(16.dp)
    val (interactionSource, bounce) = rememberBounceInteraction()
    val base = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) bounce else Modifier)
        .clip(shape)
        .background(color.copy(alpha = 0.14f), shape)
        .border(BorderStroke(1.dp, color.copy(alpha = 0.6f)), shape)
        .let {
            if (onClick != null) {
                it.clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            } else it
        }
        .padding(horizontal = 16.dp, vertical = 14.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.then(base),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(status.icon(), contentDescription = null, tint = color)
            Column {
                Text(
                    "${lens.powerLabel()}${lens.axisAddSuffix()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                )
                lens.boxLabel()?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
        Text(
            "${PairFormat.format(lens.quantity)} pair",
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}
