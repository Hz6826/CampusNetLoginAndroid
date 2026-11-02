package com.hz6826.campusnetlogin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.model.CampusState
import com.hz6826.campusnetlogin.model.SessionState
import com.hz6826.campusnetlogin.session.SessionController
import com.hz6826.campusnetlogin.ui.theme.StatusDisabled
import com.hz6826.campusnetlogin.ui.theme.StatusOnline
import com.hz6826.campusnetlogin.ui.theme.StatusWaiting
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.Color

/** 设置页的分组列表容器（岱宗盒子 SettingsCard 风格：无内边距，行自带边距）。 */
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant),
        content = content,
    )
}

@Composable
fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MiuixTheme.textStyles.body1)
            Text(
                text = summary,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** 设置项输入框：label 放在输入框外侧上方（不放进 TextField 的浮动 label）。 */
@Composable
fun SettingsField(
    label: String,
    value: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f),
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}

/**
 * 顶部校园网状态卡：状态文案在卡内水平/垂直居中，右上角彩色徽标，
 * 徽标下方是测试外网图标按钮（刷新已移到顶栏）。
 */
@Composable
fun StatusCard(
    state: SessionState,
    onRefreshAndCheck: () -> Unit,
    onRequestExclude: (() -> Unit)? = null,
) {
    val badgeText: String
    val badgeColor: Color
    when {
        state.online != null -> {
            badgeText = "已连接"
            badgeColor = StatusOnline
        }
        state.campus == CampusState.OffCampus -> {
            badgeText = "无需登录"
            badgeColor = StatusDisabled
        }
        else -> {
            badgeText = "等待连接"
            badgeColor = StatusWaiting
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("statusCard"),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(state.status, style = MiuixTheme.textStyles.title3)
                Spacer(Modifier.size(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(badgeColor, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(badgeText, style = MiuixTheme.textStyles.body2, color = badgeColor)
                    if (state.loginFailed && state.online == null && state.campus == CampusState.OnCampus &&
                        onRequestExclude != null
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "这不是校园网？加入排除列表…",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.clickable(onClick = onRequestExclude),
                        )
                    }
                }
            }
            IconButton(
                onClick = onRefreshAndCheck,
                enabled = !state.busy,
                modifier = Modifier.testTag("refreshAndCheck"),
            ) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = "刷新状态并检测外网",
                    modifier = Modifier.size(22.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        }
    }
}


/** 状态卡 + 刷新/外网检测的 Toast 反馈，登录页和在线信息页共用。 */
@Composable
fun InternetCheckStatusCard(
    controller: SessionController,
    state: SessionState,
    onRequestExclude: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    StatusCard(
        state = state,
        onRequestExclude = onRequestExclude,
        onRefreshAndCheck = {
            controller.refreshAndCheckInternet { reachable ->
                Toast.makeText(
                    context,
                    if (reachable) "Wi-Fi 外网连接正常" else "Wi-Fi 外网尚未连通，请登录校园网",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )
}
