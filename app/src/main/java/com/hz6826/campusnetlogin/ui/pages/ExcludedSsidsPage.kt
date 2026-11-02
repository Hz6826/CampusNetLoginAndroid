package com.hz6826.campusnetlogin.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.ui.components.SettingsCard
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** 「排除列表」二级页：列出不自动登录的 Wi-Fi，支持左滑/按钮删除与撤销。 */
@Composable
fun ExcludedSsidsPage(
    excluded: List<String>,
    onRemove: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    // 本地展示列表：删除时先播放淡出/收起动画，动画完全结束后再真正从设置中移除。
    var displayItems by remember { mutableStateOf(excluded) }
    var removing by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastRemoved by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(excluded) {
        val keep = displayItems.filter { it in excluded || it in removing }
        val added = excluded.filter { it !in keep }
        displayItems = keep + added
    }
    LaunchedEffect(lastRemoved) {
        if (lastRemoved != null) {
            delay(4000)
            lastRemoved = null
        }
    }

    fun requestRemove(ssid: String) {
        if (ssid in removing) return
        removing = removing + ssid
    }

    fun finishRemove(ssid: String) {
        onRemove(ssid)
        displayItems = displayItems - ssid
        removing = removing - ssid
        lastRemoved = ssid
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            if (displayItems.isEmpty() && removing.isEmpty()) {
                Text(
                    text = "暂无已排除的 Wi-Fi",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                Text(
                    text = "连接宿舍网等网络时，可在状态卡选择「加入排除列表」，也可以点击右上角 + 手动添加。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                SettingsCard {
                    displayItems.forEach { ssid ->
                        AnimatedExcludedRow(
                            ssid = ssid,
                            removing = ssid in removing,
                            onDelete = { requestRemove(ssid) },
                            onRemoved = { finishRemove(ssid) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }

        lastRemoved?.let { ssid ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                cornerRadius = 16.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "已取消排除 $ssid",
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "撤销",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            onRestore(ssid)
                            if (ssid !in displayItems) displayItems = displayItems + ssid
                            lastRemoved = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedExcludedRow(
    ssid: String,
    removing: Boolean,
    onDelete: () -> Unit,
    onRemoved: () -> Unit,
) {
    val visibleState = remember(ssid) { MutableTransitionState(true) }
    LaunchedEffect(removing) {
        if (removing) visibleState.targetState = false
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(200)) + expandVertically(tween(250)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
        label = "excludedItem-$ssid",
    ) {
        SwipeDeleteRow(ssid = ssid, onDelete = onDelete)
    }
    // 等收起动画彻底结束再通知页面移除，避免条目的布局高度被瞬间抽走导致下方条目跳动。
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) onRemoved()
    }
}

@Composable
private fun SwipeDeleteRow(ssid: String, onDelete: () -> Unit) {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    var offsetX by remember(ssid) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "excludedSwipe")
    Box(modifier = Modifier.fillMaxWidth()) {
        // 左滑时露出的红色背景。
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MiuixTheme.colorScheme.error),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .background(MiuixTheme.colorScheme.surfaceVariant)
                .pointerInput(ssid) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-threshold * 2, 0f)
                        },
                        onDragEnd = {
                            if (offsetX <= -threshold) onDelete()
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                    )
                }
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(ssid, style = MiuixTheme.textStyles.body1, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.error,
                )
            }
        }
    }
}
