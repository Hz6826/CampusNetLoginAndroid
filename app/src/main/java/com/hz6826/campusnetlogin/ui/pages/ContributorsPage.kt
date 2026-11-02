package com.hz6826.campusnetlogin.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.R
import com.hz6826.campusnetlogin.ui.components.SettingsCard
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 贡献者二级页：目前只有作者，后续继续往这里加。 */
@Composable
fun ContributorsPage() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SettingsCard {
            ContributorRow(
                avatarRes = R.drawable.avatar_hz6826,
                title = "@Hz6826",
                summary = "GitHub: github.com/Hz6826 · 项目所有者",
                onClick = { uriHandler.openUri("https://github.com/Hz6826") },
            )
            ContributorRow(
                avatarRes = R.drawable.avatar_deepseek,
                title = "@DeepSeek",
                summary = "www.deepseek.com · 开发·测试·维护·吃白饭",
                onClick = { uriHandler.openUri("https://www.deepseek.com") },
            )
            ContributorRow(
                avatarRes = R.drawable.avatar_the_time_forever,
                title = "@The-time-forever",
                summary = "GitHub: github.com/The-time-forever · 应用初版与框架参考",
                onClick = { uriHandler.openUri("https://github.com/The-time-forever") },
            )
            ContributorRow(
                avatarRes = R.drawable.avatar_triangle_github,
                title = "@Triangle-GitHub",
                summary = "GitHub: github.com/Triangle-GitHub · 保活参考",
                onClick = { uriHandler.openUri("https://github.com/Triangle-GitHub") },
            )
        }
        Spacer(Modifier.height(16.dp))
        val repoUrl = "https://github.com/Hz6826/CampusNetLoginAndroid"
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "想要贡献该项目？欢迎访问 ",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Text(
                text = "GitHub 仓库",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.clickable { uriHandler.openUri(repoUrl) },
            )
        }
    }
}

@Composable
private fun ContributorRow(
    avatarRes: Int,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(avatarRes),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MiuixTheme.textStyles.body1)
            Text(
                text = summary,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}
