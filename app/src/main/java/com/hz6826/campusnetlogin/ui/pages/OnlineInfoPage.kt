package com.hz6826.campusnetlogin.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.model.SessionState
import com.hz6826.campusnetlogin.model.formatBytes
import com.hz6826.campusnetlogin.model.formatDuration
import com.hz6826.campusnetlogin.session.SessionController
import com.hz6826.campusnetlogin.ui.components.InternetCheckStatusCard
import com.hz6826.campusnetlogin.ui.nav.LocalBottomBarInset
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/** 在线信息页：顶部与登录页相同的状态卡，标题在顶栏，信息靠上；下拉可刷新在线信息。 */
@Composable
fun OnlineInfoPage(controller: SessionController, state: SessionState, onRequestExclude: () -> Unit) {
    val info = state.online
    var refreshing by remember { mutableStateOf(false) }
    // 刷新任务结束（控制器回到空闲）时收起下拉指示器。
    LaunchedEffect(state.busy) { if (!state.busy) refreshing = false }

    PullToRefresh(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            controller.refresh()
            // 没连 Wi-Fi 或上一个任务还没结束时刷新不会真正发起，直接收起。
            if (!controller.state.busy) refreshing = false
        },
        modifier = Modifier.fillMaxSize(),
        refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新…", "刷新完成"),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            InternetCheckStatusCard(controller = controller, state = state, onRequestExclude = onRequestExclude)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 16.dp),
            ) {
                SelectionContainer {
                    Column {
                        InfoRow("账号", info?.account ?: "-")
                        InfoRow("IP", info?.ip ?: "-")
                        InfoRow("MAC", info?.mac ?: "-")
                        InfoRow("产品", info?.product ?: "-")
                        InfoRow("计费策略", info?.billing ?: "-")
                        InfoRow("已用流量", info?.let { formatBytes(it.usedBytes) } ?: "-")
                        InfoRow("已用时长", info?.let { formatDuration(it.usedSeconds) } ?: "-")
                        InfoRow("余额", info?.let { String.format(Locale.CHINA, "%.2f 元", it.balance) } ?: "-")
                        InfoRow("在线设备", info?.let { "${it.deviceTotal} 台" } ?: "-")
                    }
                }
                Spacer(Modifier.height(LocalBottomBarInset.current))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.width(90.dp),
        )
        Text(value, style = MiuixTheme.textStyles.title3)
    }
}
