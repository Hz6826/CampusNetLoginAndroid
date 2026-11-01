package com.hz6826.xjtunetlogin.ui.pages

import androidx.compose.animation.core.animateFloatAsState
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
import com.hz6826.xjtunetlogin.ui.components.SettingsCard
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
    var lastRemoved by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lastRemoved) {
        if (lastRemoved != null) {
            delay(4000)
            lastRemoved = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            if (excluded.isEmpty()) {
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
                    excluded.forEach { ssid ->
                        SwipeDeleteRow(
                            ssid = ssid,
                            onDelete = {
                                onRemove(ssid)
                                lastRemoved = ssid
                            },
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
                            lastRemoved = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeDeleteRow(ssid: String, onDelete: () -> Unit) {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    var offsetX by remember(ssid) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "excludedSwipe")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
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
