package com.hz6826.campusnetlogin.ui.pages

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.ui.components.SettingsField
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 「连接设置」二级页：账户域 / AC ID / 认证地址与校园 Wi-Fi 相关操作。 */
@Composable
fun ConnectionPage(
    draft: Draft,
    onDraftChange: (Draft) -> Unit,
    locked: Boolean,
    lockedReason: String?,
    onOpenWifi: () -> Unit,
    onSuggestWifi: () -> Unit,
    campusSsid: String,
    onCampusSsidChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        if (lockedReason != null) {
            Text(
                text = lockedReason,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            )
        }
        SettingsField(
            label = "账户域",
            value = draft.domain,
            enabled = !locked,
            onValueChange = { onDraftChange(draft.copy(domain = it)) },
        )
        Spacer(Modifier.height(12.dp))
        SettingsField(
            label = "AC ID",
            value = draft.acId,
            enabled = !locked,
            keyboardType = KeyboardType.Number,
            onValueChange = { onDraftChange(draft.copy(acId = it)) },
        )
        Spacer(Modifier.height(12.dp))
        SettingsField(
            label = "认证地址",
            value = draft.baseUrl,
            enabled = !locked,
            keyboardType = KeyboardType.Uri,
            onValueChange = { onDraftChange(draft.copy(baseUrl = it)) },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "默认使用学校 HTTP 网关；自定义网关使用 HTTPS。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
        SettingsField(
            label = "校园 Wi-Fi 名称",
            value = campusSsid,
            enabled = !locked,
            onValueChange = onCampusSsidChange,
        )
        Spacer(Modifier.height(12.dp))
        // 与上面的输入框同宽（不再额外加左右留白），两个跳转项占满这一行宽度。
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant),
        ) {
            ArrowPreference(
                title = "打开 Wi-Fi 设置",
                summary = "跳转系统 Wi-Fi 页面",
                onClick = onOpenWifi,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ArrowPreference(
                    title = "启用校园 Wi-Fi 自动连接",
                    summary = if (campusSsid.isBlank()) {
                        "向系统添加当前校园 Wi-Fi 的连接建议"
                    } else {
                        "向系统添加 $campusSsid 的连接建议"
                    },
                    onClick = onSuggestWifi,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
