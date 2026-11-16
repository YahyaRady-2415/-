package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCelColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Opaque Cel-Shaded Card with bold outline and hard offset shadow.
 * Zero Glassmorphism / zero blur.
 */
@Composable
fun CelCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalCelColors.current.cardSurface,
    borderColor: Color = LocalCelColors.current.cardBorder,
    shadowColor: Color = LocalCelColors.current.celShadow,
    accentStripeColor: Color? = null,
    cornerRadius: Dp = 14.dp,
    shadowOffset: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = CutCornerShape(topStart = cornerRadius, bottomEnd = cornerRadius, topEnd = 4.dp, bottomStart = 4.dp)
    val baseModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Box(modifier = baseModifier) {
        // Hard cel-shaded offset shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-shadowOffset), y = shadowOffset)
                .background(color = shadowColor, shape = shape)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                ),
            shape = shape,
            color = containerColor,
            border = BorderStroke(2.dp, borderColor),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (accentStripeColor != null) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                    ) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(24.dp.toPx(), 0f)
                            lineTo(0f, 24.dp.toPx())
                            close()
                        }
                        drawPath(path = path, color = accentStripeColor)
                    }
                }
                Box(modifier = Modifier.padding(contentPadding), content = content)
            }
        }
    }
}

@Composable
fun CelActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalCelColors.current.primaryAccent,
    contentColor: Color = if (LocalCelColors.current.isDark) Color(0xFF040D1A) else Color.White,
    borderColor: Color = LocalCelColors.current.cardBorder,
    enabled: Boolean = true,
    testTag: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val cel = LocalCelColors.current
    val shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
    val tagModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Box(modifier = tagModifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-3).dp, y = 3.dp)
                .background(if (enabled) cel.celShadow else cel.celShadow.copy(alpha = 0.4f), shape)
        )
        Surface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick),
            shape = shape,
            color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
            border = BorderStroke(2.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
    }
}

/**
 * Eight-pointed Rub el Hizb Islamic geometric star (two rotated squares).
 * Never uses any cross motif.
 */
@Composable
fun RubElHizbStar(
    modifier: Modifier = Modifier,
    fillColor: Color = LocalCelColors.current.primaryAccent,
    strokeColor: Color = LocalCelColors.current.cardBorder,
    innerColor: Color = LocalCelColors.current.goldAccent,
) {
    Canvas(modifier = modifier) {
        val side = size.minDimension * 0.64f
        val half = side / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Square 1
        drawRect(
            color = fillColor,
            topLeft = Offset(center.x - half, center.y - half),
            size = Size(side, side),
        )
        drawRect(
            color = strokeColor,
            topLeft = Offset(center.x - half, center.y - half),
            size = Size(side, side),
            style = Stroke(width = 2.dp.toPx()),
        )

        // Square 2 rotated 45 deg
        rotate(45f, pivot = center) {
            drawRect(
                color = fillColor,
                topLeft = Offset(center.x - half, center.y - half),
                size = Size(side, side),
            )
            drawRect(
                color = strokeColor,
                topLeft = Offset(center.x - half, center.y - half),
                size = Size(side, side),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // Inner radiant circle
        drawCircle(
            color = innerColor,
            radius = side * 0.24f,
            center = center,
        )
        drawCircle(
            color = strokeColor,
            radius = side * 0.24f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

@Composable
fun CelSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val cel = LocalCelColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            RubElHizbStar(
                modifier = Modifier.size(28.dp),
                fillColor = cel.primaryAccent,
                strokeColor = cel.cardBorder,
                innerColor = cel.goldAccent,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = cel.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cel.textSecondary,
                    )
                }
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun CelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = LocalCelColors.current.primaryAccent,
    trackColor: Color = LocalCelColors.current.cardSurfaceAlt,
    borderColor: Color = LocalCelColors.current.cardBorder,
    height: Dp = 14.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor, shape)
            .clip(shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fillWidth = size.width * clamped
            if (fillWidth > 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(fillColor, fillColor.copy(alpha = 0.85f))
                    ),
                    size = Size(fillWidth, size.height),
                )
                // Kinetic cel highlight strip on top edge
                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(0f, 1.dp.toPx()),
                    size = Size(fillWidth, 3.dp.toPx()),
                )
            }
            // Segmented ticks
            val segments = 4
            for (i in 1 until segments) {
                val x = size.width * (i / segments.toFloat())
                drawLine(
                    color = borderColor.copy(alpha = 0.35f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            drawRect(
                color = borderColor,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

/**
 * Full-screen cel-shaded winter aurora background with crisp geometric lines & optional snowfall.
 */
@Composable
fun CelWinterBackground(
    snowfallEnabled: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val cel = LocalCelColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(cel.backgroundTop, cel.backgroundBottom)
                )
            )
    ) {
        // Subtle anime speed-lines & Islamic geometric corner star
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Diagonal kinetic aurora rays
            val rayColor = if (cel.isDark) cel.primaryAccent.copy(alpha = 0.05f) else cel.primaryAccent.copy(alpha = 0.04f)
            for (i in 0..5) {
                val startX = w * (0.15f + i * 0.16f)
                drawLine(
                    color = rayColor,
                    start = Offset(startX, 0f),
                    end = Offset(startX - w * 0.25f, h * 0.45f),
                    strokeWidth = (24 - i * 2).dp.toPx(),
                )
            }
            // Large subtle 8-pointed star watermark at bottom-left
            val starSize = w * 0.55f
            val starCenter = Offset(w * 0.12f, h * 0.85f)
            val wmColor = if (cel.isDark) cel.primaryAccent.copy(alpha = 0.05f) else cel.primaryAccent.copy(alpha = 0.04f)
            drawRect(
                color = wmColor,
                topLeft = Offset(starCenter.x - starSize / 2, starCenter.y - starSize / 2),
                size = Size(starSize, starSize),
                style = Stroke(width = 2.dp.toPx()),
            )
            rotate(45f, pivot = starCenter) {
                drawRect(
                    color = wmColor,
                    topLeft = Offset(starCenter.x - starSize / 2, starCenter.y - starSize / 2),
                    size = Size(starSize, starSize),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        if (snowfallEnabled) {
            SnowfallOverlay(color = cel.snowflakeColor)
        }

        content()
    }
}

private data class SnowflakeSeed(
    val xFraction: Float,
    val yOffset: Float,
    val speed: Float,
    val radiusDp: Float,
    val swayAmplitude: Float,
    val isCrystal: Boolean,
)

@Composable
fun SnowfallOverlay(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val seeds = remember {
        val rng = Random(294)
        List(28) { idx ->
            SnowflakeSeed(
                xFraction = rng.nextFloat(),
                yOffset = rng.nextFloat(),
                speed = 0.45f + rng.nextFloat() * 0.85f,
                radiusDp = 2.5f + rng.nextFloat() * 4.5f,
                swayAmplitude = 10f + rng.nextFloat() * 20f,
                isCrystal = idx % 3 == 0,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "snowfall")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "snowProgress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        seeds.forEach { seed ->
            val yNorm = (seed.yOffset + progress * seed.speed) % 1f
            val y = yNorm * h
            val sway = sin((yNorm * 2 * PI + seed.xFraction * 10)).toFloat() * seed.swayAmplitude.dp.toPx()
            val x = (seed.xFraction * w + sway).coerceIn(0f, w)
            val r = seed.radiusDp.dp.toPx()
            if (seed.isCrystal) {
                // Draw crisp 6-pointed ice crystal (never a cross)
                for (arm in 0 until 3) {
                    val angle = (arm * 60f + progress * 180f) * (PI / 180.0)
                    val dx = (cos(angle) * r * 1.4f).toFloat()
                    val dy = (sin(angle) * r * 1.4f).toFloat()
                    drawLine(
                        color = color.copy(alpha = 0.48f),
                        start = Offset(x - dx, y - dy),
                        end = Offset(x + dx, y + dy),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            } else {
                drawCircle(
                    color = color.copy(alpha = 0.42f),
                    radius = r,
                    center = Offset(x, y),
                )
            }
        }
    }
}

/**
 * Guilty Gear Strive-inspired cel-shaded energy burst effect when completing an act of worship.
 */
@Composable
fun CelebrationBurstOverlay(
    triggerKey: Int,
    label: String?,
    onFinished: () -> Unit,
) {
    if (triggerKey <= 0) return
    val cel = LocalCelColors.current
    val anim = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    val t = anim.value
    val alpha = if (t < 0.7f) 1f else ((1f - t) / 0.3f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.48f * t

            // Expanding cel shockwave octagon
            drawCircle(
                color = cel.primaryAccent.copy(alpha = alpha * 0.7f),
                radius = maxR,
                center = center,
                style = Stroke(width = (8f * (1f - t) + 2f).dp.toPx()),
            )
            drawCircle(
                color = cel.goldAccent.copy(alpha = alpha * 0.85f),
                radius = maxR * 0.72f,
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )

            // 12 radial anime speed-burst shards
            for (i in 0 until 12) {
                val angle = (i * 30.0) * (PI / 180.0)
                val inner = maxR * 0.38f
                val outer = maxR * (if (i % 2 == 0) 1.15f else 0.88f)
                val p1 = Offset(
                    center.x + (cos(angle) * inner).toFloat(),
                    center.y + (sin(angle) * inner).toFloat(),
                )
                val p2 = Offset(
                    center.x + (cos(angle) * outer).toFloat(),
                    center.y + (sin(angle) * outer).toFloat(),
                )
                drawLine(
                    color = (if (i % 2 == 0) cel.goldAccent else cel.primaryAccent).copy(alpha = alpha),
                    start = p1,
                    end = p2,
                    strokeWidth = (if (i % 2 == 0) 4.dp else 2.5.dp).toPx(),
                    cap = StrokeCap.Square,
                )
            }
        }

        if (!label.isNullOrBlank() && alpha > 0.1f) {
            CelCard(
                containerColor = cel.cardSurface,
                borderColor = cel.goldAccent,
                shadowColor = cel.celShadow,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RubElHizbStar(
                        modifier = Modifier.size(28.dp),
                        fillColor = cel.goldAccent,
                        strokeColor = cel.cardBorder,
                        innerColor = cel.primaryAccent,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cel.textPrimary.copy(alpha = alpha),
                    )
                }
            }
        }
    }
}
