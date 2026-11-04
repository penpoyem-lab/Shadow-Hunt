package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Promotional banner slide data model.
 */
data class BannerSlide(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val slideNumber: String,
    val imageRes: Int,
    val primaryColor: Color,
    val glowColor: Color,
    val borderColors: List<Color>,
    val buttonText: String = "PLAY NOW",
    val hasEmbeddedGraphicText: Boolean = false
)

val defaultBannerSlides = listOf(
    BannerSlide(
        id = "shadow_hunt",
        title = "SHADOW HUNT",
        subtitle = "FIND THEM BEFORE THE ZONE DISAPPEARS.",
        badge = "🔥 NEW",
        slideNumber = "1/5",
        imageRes = R.drawable.banner_shadow_hunt,
        primaryColor = NeonPurple,
        glowColor = Color(0x669D00FF),
        borderColors = listOf(Color(0xFF9D00FF), Color(0xFFFF007F), Color(0xFF00E5FF)),
        buttonText = "PLAY NOW ▶",
        hasEmbeddedGraphicText = true
    ),
    BannerSlide(
        id = "ready_to_hunt",
        title = "READY TO BE HUNTED?",
        subtitle = "Hide. Survive. Outsmart.",
        badge = "SURVIVAL",
        slideNumber = "2/5",
        imageRes = R.drawable.banner_ready_to_hunt,
        primaryColor = NeonRed,
        glowColor = Color(0x66FF1E43),
        borderColors = listOf(Color(0xFFFF1E43), Color(0xFFFF5252), Color(0xFF9D00FF)),
        buttonText = "PLAY NOW ▶",
        hasEmbeddedGraphicText = true
    ),
    BannerSlide(
        id = "battle_zone",
        title = "BATTLE ZONE",
        subtitle = "Be the Last One Standing!",
        badge = "RANKED",
        slideNumber = "3/5",
        imageRes = R.drawable.banner_battle_zone,
        primaryColor = ElectricBlue,
        glowColor = Color(0x6600E5FF),
        borderColors = listOf(Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF7C4DFF)),
        buttonText = "PLAY NOW ▶",
        hasEmbeddedGraphicText = true
    ),
    BannerSlide(
        id = "night_survival",
        title = "NIGHT SURVIVAL",
        subtitle = "Survive the darkness.",
        badge = "NIGHT OPS",
        slideNumber = "4/5",
        imageRes = R.drawable.banner_night_survival,
        primaryColor = NeonGreen,
        glowColor = Color(0x6600E676),
        borderColors = listOf(Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFF1DE9B6)),
        buttonText = "PLAY NOW ▶",
        hasEmbeddedGraphicText = false
    ),
    BannerSlide(
        id = "ultimate_challenge",
        title = "ULTIMATE CHALLENGE",
        subtitle = "Play, Compete and Win.",
        badge = "CHAMPIONS",
        slideNumber = "5/5",
        imageRes = R.drawable.banner_ultimate_challenge,
        primaryColor = GoldAccent,
        glowColor = Color(0x66FFD700),
        borderColors = listOf(Color(0xFFFFD700), Color(0xFFFF9100), Color(0xFFFF1744)),
        buttonText = "PLAY NOW ▶",
        hasEmbeddedGraphicText = false
    )
)

/**
 * Premium Hero Banner Carousel with smooth touch swiping, 4-second auto-slide,
 * 3D card depth scaling, preview edges, and glowing animated pagination dots.
 */
@Composable
fun HeroBannerCarousel(
    modifier: Modifier = Modifier,
    slides: List<BannerSlide> = defaultBannerSlides,
    onBannerClick: (BannerSlide) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    // 4-second auto-slide loop that pauses while user touches/drags
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            while (true) {
                delay(4000L)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % slides.size
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(
                            durationMillis = 650,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Dynamic responsive height scaling cleanly across all Android screen sizes
        val bannerHeight = (maxWidth * 0.58f).coerceIn(210.dp, 275.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 12.dp
            ) { page ->
                val slide = slides[page]

                // 3D scaling, alpha, and Y-rotation calculation for depth effect
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

                val scale = lerp(0.88f, 1.0f, 1f - absOffset)
                val alpha = lerp(0.55f, 1.0f, 1f - absOffset)
                val rotationY = pageOffset * -10f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            this.rotationY = rotationY
                            cameraDistance = 14f * density
                        }
                ) {
                    HeroBannerCard(
                        slide = slide,
                        onClick = { onBannerClick(slide) }
                    )
                }
            }

            // Animated 5 Pagination Dots with Neon Purple Glow
            Row(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 26.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "dotWidth_$index"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) NeonPurple else Color(0xFF25293E),
                        animationSpec = tween(300),
                        label = "dotColor_$index"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.5.dp)
                            .height(7.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFFD580FF), NeonPurple)
                                        ),
                                        shape = CircleShape
                                    )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Individual Card inside HeroBannerCarousel with glassmorphic border,
 * rich artwork, badges, typography, and glowing button.
 */
@Composable
private fun HeroBannerCard(
    slide: BannerSlide,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = slide.glowColor,
                spotColor = slide.glowColor
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0F111E),
        border = BorderStroke(
            width = 1.8.dp,
            brush = Brush.linearGradient(colors = slide.borderColors)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Artwork
            Image(
                painter = painterResource(id = slide.imageRes),
                contentDescription = "${slide.title} Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top and Bottom Dark Gradient Vignette for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = if (slide.hasEmbeddedGraphicText) 0.25f else 0.85f)
                            )
                        )
                    )
            )

            // Top Badges Row (e.g. "🔥 NEW" / "NIGHT OPS" and "1/5", "4/5")
            if (!slide.hasEmbeddedGraphicText) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category / Tag Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .border(1.dp, slide.primaryColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = slide.badge,
                            color = slide.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Index Badge (e.g. "4/5")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, Color(0xFF3B4058), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = slide.slideNumber,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom Content for Banners 4 & 5 (Title, Subtitle, and Glowing Button)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = slide.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = slide.subtitle,
                        color = slide.primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    // Glowing Action Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        slide.primaryColor,
                                        slide.borderColors.getOrElse(1) { slide.primaryColor }
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = slide.buttonText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
