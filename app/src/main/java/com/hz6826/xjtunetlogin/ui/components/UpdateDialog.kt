package com.hz6826.xjtunetlogin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hz6826.xjtunetlogin.update.UpdateRelease
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 发现新版本提示框：底部半屏，安装 / 暂不安装 / 忽略该版本。 */
@Composable
fun UpdateDialog(
    release: UpdateRelease,
    busy: Boolean,
    onInstall: () -> Unit,
    onLater: () -> Unit,
    onIgnore: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!busy) onLater() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 48.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.55f),
                cornerRadius = 24.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text("发现新版本", style = MiuixTheme.textStyles.title2)
                    Spacer(Modifier.height(4.dp))
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
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
                        onClick = onLater,
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
    }
}
