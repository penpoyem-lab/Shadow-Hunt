package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale

@Composable
fun NeonGamingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingText: String? = ">>",
    gradientColors: List<Color> = listOf(LiquidCrimsonCoral, LiquidCrimsonOrange),
    borderColor: Color = Color(0xFFFF5252),
    waterShadowColor: Color = WaterShadowCrimson,
    height: Dp = 58.dp,
    enabled: Boolean = true
) {
    LiquidGlassButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        trailingText = trailingText,
        gradientColors = gradientColors,
        waterShadowColor = waterShadowColor,
        height = height,
        enabled = enabled
    )
}

@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingText: String? = ">>",
    gradientColors: List<Color> = listOf(LiquidCrimsonCoral, LiquidCrimsonOrange),
    waterShadowColor: Color = WaterShadowCrimson,
    height: Dp = 58.dp,
    shape: Shape = RoundedCornerShape(30.dp),
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquidButtonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "liquidGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquidGlowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .height(height)
            // Liquid Water Drop Shadow (Diffusion pool)
            .shadow(
                elevation = if (isPressed) 6.dp else 16.dp,
                shape = shape,
                ambientColor = waterShadowColor.copy(alpha = if (enabled) 0.5f * glowAlpha else 0.15f),
                spotColor = waterShadowColor.copy(alpha = if (enabled) 0.85f * glowAlpha else 0.25f)
            )
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (enabled) gradientColors else listOf(Color(0xFF242938), Color(0xFF191C28))
                )
            )
            // Dual specular glass border (surface tension highlight at top)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xAAFFFFFF), // Specular light highlight on top curved rim
                        waterShadowColor.copy(alpha = 0.6f),
                        Color(0x33000000)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Top Glass Specular Arc Reflection (Lens refraction)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55FFFFFF),
                            Color(0x15FFFFFF),
                            Color.Transparent
                        )
                    )
                )
        )

        // Button Content with crisp typography
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.3.sp,
                    color = Color.White
                )
            )
            if (trailingText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkCardBorder,
    backgroundColor: Color = DarkSurfaceGlass,
    waterShadowColor: Color = WaterShadowAqua.copy(alpha = 0.35f),
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.2.dp,
    elevation: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = waterShadowColor,
                spotColor = waterShadowColor.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x66FFFFFF),
                        borderColor,
                        borderColor.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            )
    ) {
        // Specular sheen top edge line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66FFFFFF),
                            Color(0x9900F2FE),
                            Color(0x66FFFFFF),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun StatCounterBox(
    value: String,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HowToPlayStepItem(
    stepNumber: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.5.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$stepNumber. $title",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
            color = TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
