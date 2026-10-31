package com.hz6826.xjtunetlogin.ui.pages

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hz6826.xjtunetlogin.R
import com.hz6826.xjtunetlogin.ui.components.SettingsCard
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val REPO_URL = "https://github.com/Hz6826/XJTUNetLogin"
private const val GPL_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val TOOLBOX_URL = "https://github.com/yeliqin666/xjtu-toolbox-android"

/** 关于二级页：图标/名称/版本居中，下面是链接、贡献者、伙伴项目与参考项目。 */
@Composable
fun AboutPage(
    onOpenContributors: () -> Unit,
    onOpenReferences: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        @Suppress("DEPRECATION")
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "仙交网络助手",
            style = MiuixTheme.textStyles.title1,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "版本 v$versionName",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        SmallTitle("项目")
        SettingsCard {
            ArrowPreference(
                title = "项目主页",
                summary = "GitHub · Hz6826/XJTUNetLogin",
                onClick = { uriHandler.openUri(REPO_URL) },
            )
            ArrowPreference(
                title = "项目许可证",
                summary = "GNU GPLv3",
                onClick = { uriHandler.openUri(GPL_URL) },
            )
            ArrowPreference(
                title = "反馈问题",
                summary = "GitHub Issues",
                onClick = { uriHandler.openUri("$REPO_URL/issues") },
            )
            ArrowPreference(
                title = "作者主页",
                summary = "GitHub · Hz6826",
                onClick = { uriHandler.openUri("https://github.com/Hz6826") },
            )
        }

        SmallTitle("贡献者")
        SettingsCard {
            ArrowPreference(
                title = "贡献者",
                summary = "感谢每一位参与贡献的人",
                onClick = onOpenContributors,
            )
        }

        SmallTitle("伙伴项目")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(TOOLBOX_URL) }
                    .padding(start = 12.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.toolbox_logo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("岱宗盒子", style = MiuixTheme.textStyles.body1)
                    Text(
                        text = "XJTU-ToolBox · 校园服务工具箱",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            }
        }

        SmallTitle("参考项目")
        SettingsCard {
            ArrowPreference(
                title = "参考项目及许可",
                summary = "开源项目与各项目许可证",
                onClick = onOpenReferences,
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = "麻烦点一下仓库的 Star，谢谢喵~",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            textAlign = TextAlign.Center,
        )
    }
}
