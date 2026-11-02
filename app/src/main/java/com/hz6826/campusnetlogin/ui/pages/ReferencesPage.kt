package com.hz6826.campusnetlogin.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.ui.components.SettingsCard
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 参考项目及许可证二级页（不含岱宗盒子，它单独作为伙伴项目）。 */
@Composable
fun ReferencesPage() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SettingsCard {
            ArrowPreference(
                title = "Miuix",
                summary = "Apache-2.0 · compose-miuix-ui/miuix",
                onClick = { uriHandler.openUri("https://github.com/compose-miuix-ui/miuix") },
            )
            ArrowPreference(
                title = "AndroidLiquidGlass",
                summary = "Apache-2.0 · Kyant0/AndroidLiquidGlass",
                onClick = { uriHandler.openUri("https://github.com/Kyant0/AndroidLiquidGlass") },
            )
            ArrowPreference(
                title = "backdrop",
                summary = "Apache-2.0 · Kyant0/AndroidLiquidGlass/backdrop",
                onClick = { uriHandler.openUri("https://github.com/Kyant0/AndroidLiquidGlass/tree/kmp/backdrop") },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "以上项目按各自许可证使用。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
