package com.hz6826.xjtunetlogin.ui.pages

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hz6826.xjtunetlogin.model.SessionState
import com.hz6826.xjtunetlogin.model.formatBytes
import com.hz6826.xjtunetlogin.model.formatDuration
import com.hz6826.xjtunetlogin.session.SessionController
import com.hz6826.xjtunetlogin.ui.components.InternetCheckStatusCard
import com.hz6826.xjtunetlogin.ui.nav.LocalBottomBarInset
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/** 在线信息页：顶部与登录页相同的状态卡，标题在顶栏，信息靠上。 */
@Composable
fun OnlineInfoPage(controller: SessionController, state: SessionState, onRequestExclude: () -> Unit) {
    val info = state.online
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
