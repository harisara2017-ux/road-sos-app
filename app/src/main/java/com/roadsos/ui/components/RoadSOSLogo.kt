package com.roadsos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RoadSOS Brand Logo:
 * Merges a perspective highway road icon with an emergency cross / pulse line emblem.
 */
@Composable
fun RoadSOSBrandIcon(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.28f),
        color = Color(0xFF0F172A), // Dark slate/black base
        shadowElevation = 4.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Draw highway perspective road (white lines)
            val leftRoad = Path().apply {
                moveTo(w * 0.18f, h * 0.95f)
                lineTo(w * 0.38f, h * 0.15f)
            }
            val rightRoad = Path().apply {
                moveTo(w * 0.82f, h * 0.95f)
                lineTo(w * 0.62f, h * 0.15f)
            }
            drawPath(leftRoad, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = w * 0.08f))
            drawPath(rightRoad, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = w * 0.08f))

            // Center lane dashed marker
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(w * 0.5f, h * 0.88f),
                end = Offset(w * 0.5f, h * 0.7f),
                strokeWidth = w * 0.06f
            )

            // 2. Center Emergency Medical Cross in Red
            val crossSize = w * 0.46f
            val cx = w * 0.5f
            val cy = h * 0.44f
            val armThick = crossSize * 0.34f

            // Vertical arm
            drawRect(
                color = Color(0xFFDC2626), // Emergency Red
                topLeft = Offset(cx - armThick / 2, cy - crossSize / 2),
                size = Size(armThick, crossSize)
            )
            // Horizontal arm
            drawRect(
                color = Color(0xFFDC2626),
                topLeft = Offset(cx - crossSize / 2, cy - armThick / 2),
                size = Size(crossSize, armThick)
            )
        }
    }
}

@Composable
fun RoadSOSHeaderLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoadSOSBrandIcon(size = if (compact) 36.dp else 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Road",
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "SOS",
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFDC2626) // Emergency Red accent
                )
            }
            if (!compact) {
                Text(
                    text = "EMERGENCY DISPATCH & SAFETY",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
