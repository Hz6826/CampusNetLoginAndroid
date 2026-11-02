package com.hz6826.campusnetlogin.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hz6826.campusnetlogin.model.LogEntry
import com.hz6826.campusnetlogin.session.SessionController
import com.hz6826.campusnetlogin.ui.nav.LocalBottomBarInset
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LogPage(
    controller: SessionController,
    logsState: State<List<LogEntry>>,
    logHttp: Boolean,
    onLogHttpChange: (Boolean) -> Unit,
) {
    val logs by logsState
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("运行日志", style = MiuixTheme.textStyles.title3, modifier = Modifier.weight(1f))
            Checkbox(
                state = if (logHttp) ToggleableState.On else ToggleableState.Off,
                onClick = { onLogHttpChange(!logHttp) },
                modifier = Modifier.testTag("logHttp"),
            )
            Spacer(Modifier.width(4.dp))
            Text("记录 HTTP 日志", style = MiuixTheme.textStyles.body2)
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { controller.clearLogs() },
                enabled = logs.isNotEmpty(),
                modifier = Modifier.testTag("clearLogs"),
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = "清空日志",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val logScroll = rememberScrollState()
        LaunchedEffect(logs.lastOrNull()) {
            withFrameNanos { }
            logScroll.scrollTo(logScroll.maxValue)
        }
        SelectionContainer(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E))
                    .verticalScroll(logScroll)
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp + LocalBottomBarInset.current),
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "暂无日志",
                        color = Color(0xFFD4D4D4).copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                } else {
                    logs.forEach { entry ->
                        Text(
                            text = "[${entry.level}] ${entry.message}",
                            color = logColor(entry.level),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

/** 日志级别配色，对齐原项目 MainWindowLog.cpp。 */
private fun logColor(level: String): Color = when (level) {
    "ERROR" -> Color(0xFFF44747)
    "WARN" -> Color(0xFFDCDCAA)
    "SUCCESS" -> Color(0xFF4EC96B)
    "STEP" -> Color(0xFF569CD6)
    "HTTP" -> Color(0xFF808080)
    else -> Color(0xFFD4D4D4)
}
