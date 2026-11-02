package com.hz6826.campusnetlogin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.update.UpdateRelease
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 发现新版本提示框：底部半屏，与「加入排除列表」同款。
 * 拖拽顶部操作杆或点击窗口外都能关闭；下载中不允许关闭，避免看不到进度。
 */
@Composable
fun UpdateDialog(
    release: UpdateRelease,
    show: Boolean,
    busy: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
    onInstall: () -> Unit,
    onIgnore: () -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = "发现新版本",
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        allowDismiss = !busy,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        ) {
            Text(
                text = release.tag + if (release.prerelease) "（预览版）" else "",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = release.body.ifBlank { release.title },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
            )
            if (busy) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "正在下载并校验 APK…",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("安装") }
            Spacer(Modifier.height(8.dp))
            TextButton(
                text = "暂不安装",
                onClick = onDismissRequest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "忽略该版本",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !busy) { onIgnore() }
                    .padding(vertical = 12.dp),
            )
        }
    }
}
