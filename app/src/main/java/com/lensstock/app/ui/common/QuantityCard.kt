package com.lensstock.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lensstock.app.domain.PairFormat

private val QuantityTint = Color(0xFF4F3DC4)
private val QuantityBg = Color(0xFFEDEAFB)
private val QuantityBadgeBg = Color(0xFFDAD4F7)
private val QuantityChipBg = Color(0xFFE3DEF9)

/** The quantity stepper card shared by the Add/Edit lens sheets — pieces in, pairs shown. */
@Composable
fun QuantityCard(
    quantity: Long,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onDecreasePair: () -> Unit,
    onIncreasePair: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(18.dp), color = QuantityBg, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Layers, contentDescription = null, tint = QuantityTint, modifier = Modifier.size(20.dp))
                    Text("Quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = QuantityTint)
                }
                Surface(shape = CircleShape, color = QuantityBadgeBg) {
                    Text(
                        "= ${PairFormat.format(quantity)} pair",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = QuantityTint,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            QuantityStepper(quantity = quantity, onDecrease = onDecrease, onIncrease = onIncrease, onQuantityChange = onQuantityChange)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PairStepButton("− 1 pair", onDecreasePair, Modifier.weight(1f))
                PairStepButton("+ 1 pair", onIncreasePair, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PairStepButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = QuantityChipBg,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = QuantityTint,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

/** A single pill-shaped stepper — no clashing icon-button / text-field / icon-button seams. */
@Composable
private fun QuantityStepper(
    quantity: Long,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onQuantityChange: (String) -> Unit,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepperIconButton(Icons.Filled.Remove, "Decrease", onDecrease)
            BasicTextField(
                value = quantity.toString(),
                onValueChange = onQuantityChange,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
            )
            StepperIconButton(Icons.Filled.Add, "Increase", onIncrease)
        }
    }
}

@Composable
private fun StepperIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(QuantityBadgeBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = QuantityTint, modifier = Modifier.size(20.dp))
    }
}
