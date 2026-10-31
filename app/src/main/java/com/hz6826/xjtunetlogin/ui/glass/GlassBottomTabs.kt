// 玻璃底栏：移植自 XJTU-ToolBox（GPL-3.0），上游参考 Kyant0/AndroidLiquidGlass（Apache-2.0）。
package com.hz6826.xjtunetlogin.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** 玻璃底栏本体的高度。 */
val GLASS_BAR_HEIGHT = 58.dp

/** 当前是否深色主题（跟随应用设置，而不是系统）。 */
val LocalIsDarkTheme = compositionLocalOf { false }

/** 滑块盖住的格子轻微放大，只在染色层可见。 */
val LocalGlassNavTabScale = staticCompositionLocalOf { { 1f } }

/** 悬浮玻璃的阴影：往主题色里掺一点环境光，比纯黑柔和。 */
fun floatingGlassShadow(accent: Color, isDark: Boolean): Shadow = Shadow(
    radius = 28.dp,
    offset = DpOffset(0.dp, 10.dp),
    color = androidx.compose.ui.graphics.lerp(Color.Black, accent, 0.4f).copy(alpha = if (isDark) 0.45f else 0.16f),
)

class GlassTab(
    val key: Any,
    val content: @Composable ColumnScope.(selected: Boolean) -> Unit,
)

/**
 * 悬浮液态玻璃底栏：按住滑块可拖动换 tab，松手吸附；带模糊、折射、高光和阴影。
 */
@Composable
fun GlassBottomTabs(
    tabs: List<GlassTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val tabsCount = tabs.size
    if (tabsCount == 0) return

    val isDark = LocalIsDarkTheme.current
    val accentColor = MiuixTheme.colorScheme.primary
    val containerColor = MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
    val barShadow = remember(accentColor, isDark) { floatingGlassShadow(accentColor, isDark) }
    val restTint = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    val pillShape = remember { RoundedCornerShape(28.dp) }
    val innerHeight = GLASS_BAR_HEIGHT - 8.dp
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidthPx = with(density) { (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density, constraints.maxWidth) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth.toFloat()).coerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember { mutableIntStateOf(selectedIndex.coerceIn(0, tabsCount - 1)) }
        val onTabSelectedUpdated by rememberUpdatedState(onTabSelected)

        val dampedDrag = remember(animationScope, tabsCount) {
            GlassDampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = GlassDragMath.snappedIndex(targetValue, tabsCount)
                    if (currentIndex != targetIndex) {
                        currentIndex = targetIndex
                        onTabSelectedUpdated(targetIndex)
                    }
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    val deltaX = if (isLtr) dragAmount.x else -dragAmount.x
                    updateValue(GlassDragMath.dragTargetValue(targetValue, deltaX, tabWidthPx, tabsCount))
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                },
            )
        }

        // 程序切 tab（比如点击）时把滑块同步过去。
        LaunchedEffect(selectedIndex, tabsCount) {
            val target = selectedIndex.coerceIn(0, tabsCount - 1)
            if (currentIndex != target) {
                currentIndex = target
                dampedDrag.animateToValue(target.toFloat())
            }
        }

        val interactiveHighlight = remember(animationScope, isLtr, dampedDrag) {
            GlassInteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        x = if (isLtr) {
                            (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                        } else {
                            size.width - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                        },
                        y = size.height / 2f,
                    )
                },
            )
        }

        // 静止时不画染色层和滑块，避免无谓的着色器开销。
        val glassActive by remember(dampedDrag, interactiveHighlight) {
            derivedStateOf {
                dampedDrag.pressProgress > 0f || interactiveHighlight.pressProgress > 0f ||
                    dampedDrag.scaleX != 1f || dampedDrag.scaleY != 1f ||
                    abs(dampedDrag.value - dampedDrag.value.roundToInt()) > 0.001f
            }
        }

        fun tabOnClick(index: Int): () -> Unit = {
            if (currentIndex != index) {
                currentIndex = index
                onTabSelectedUpdated(index)
            }
            dampedDrag.animateToValue(index.toFloat())
        }

        val realContent: @Composable RowScope.() -> Unit = {
            tabs.forEachIndexed { index, tab ->
                GlassNavTabSlot(onClick = tabOnClick(index)) {
                    tab.content(this, index == currentIndex)
                }
            }
        }

        // 整排都按选中态画一遍（alpha=0），作为滑块里「玻璃 + 强调色」的采样源。
        val tintedContent: @Composable RowScope.() -> Unit = {
            tabs.forEachIndexed { index, tab ->
                GlassNavTabSlot(onClick = tabOnClick(index)) {
                    tab.content(this, true)
                }
            }
        }

        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { pillShape },
                    effects = {
                        padding = maxOf(padding, 16.dp.toPx())
                        vibrancy()
                        blur(8.dp.toPx())
                        val progress = dampedDrag.pressProgress
                        lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                    },
                    layerBlock = {
                        val progress = dampedDrag.pressProgress
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    shadow = { barShadow },
                    onDrawSurface = { drawRect(containerColor) },
                )
                .then(interactiveHighlight.modifier)
                .height(GLASS_BAR_HEIGHT)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = realContent,
        )

        CompositionLocalProvider(LocalGlassNavTabScale provides { lerp(1f, 1.2f, dampedDrag.pressProgress) }) {
            Row(
                Modifier
                    .alpha(0f)
                    .clearAndSetSemantics { }
                    .drawWithContent { if (glassActive) drawContent() }
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            padding = maxOf(padding, 16.dp.toPx())
                            vibrancy()
                            blur(8.dp.toPx())
                            val progress = dampedDrag.pressProgress
                            lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                        },
                        highlight = { Highlight.Default.copy(alpha = dampedDrag.pressProgress) },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .then(interactiveHighlight.modifier)
                    .height(innerHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = tintedContent,
            )
        }

        if (tabWidthPx > 0f) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        translationX =
                            if (isLtr) dampedDrag.value * tabWidthPx + panelOffset
                            else size.width - (dampedDrag.value + 1f) * tabWidthPx + panelOffset
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDrag.modifier)
                    .drawWithContent {
                        if (glassActive) drawContent()
                        else drawOutline(pillShape.createOutline(size, layoutDirection, this), restTint)
                    }
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { pillShape },
                        effects = {
                            val progress = dampedDrag.pressProgress
                            lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, chromaticAberration = true)
                        },
                        highlight = { Highlight.Default.copy(alpha = dampedDrag.pressProgress) },
                        layerBlock = {
                            scaleX = dampedDrag.scaleX
                            scaleY = dampedDrag.scaleY
                            val v = abs(dampedDrag.velocity) / 10f
                            scaleX /= 1f - (v * 0.75f).coerceIn(0f, 0.2f)
                            scaleY *= 1f - (v * 0.25f).coerceIn(0f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDrag.pressProgress
                            drawRect(restTint, alpha = 1f - progress)
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        },
                    )
                    .height(innerHeight)
                    .fillMaxWidth(1f / tabsCount),
            )
        }
    }
}

/** 底栏里的一格：占满高度、均分宽度。 */
@Composable
private fun RowScope.GlassNavTabSlot(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalGlassNavTabScale.current
    Column(
        modifier
            .fillMaxHeight()
            .weight(1f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = null, indication = null, role = Role.Tab, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
