package com.hz6826.xjtunetlogin.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hz6826.xjtunetlogin.model.CampusState
import com.hz6826.xjtunetlogin.model.SessionState
import com.hz6826.xjtunetlogin.session.SessionController
import com.hz6826.xjtunetlogin.ui.components.InternetCheckStatusCard
import com.hz6826.xjtunetlogin.ui.components.SwitchRow
import com.hz6826.xjtunetlogin.ui.nav.LocalBottomBarInset
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LoginPage(
    controller: SessionController,
    state: SessionState,
    draft: Draft,
    onOpenKeepAlive: () -> Unit,
    onOpenConnection: () -> Unit,
    onRequestExclude: () -> Unit,
    onDraftChange: (Draft) -> Unit,
) {
    val online = state.online
    val busy = state.busy
    // 与原项目一致：在线和请求中都不能改账户/连接设置，登录成功后锁定。
    val locked = busy || online != null
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        InternetCheckStatusCard(controller = controller, state = state, onRequestExclude = onRequestExclude)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 28.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "账户登录",
                style = MiuixTheme.textStyles.title2,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                TextField(
                    value = draft.username,
                    onValueChange = { onDraftChange(draft.copy(username = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account")
                        .alpha(if (locked) 0.4f else 1f),
                    label = "账号",
                    singleLine = true,
                    enabled = !locked,
                )
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = draft.password,
                    onValueChange = { onDraftChange(draft.copy(password = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password")
                        .alpha(if (locked) 0.4f else 1f),
                    label = "密码",
                    singleLine = true,
                    enabled = !locked,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.testTag("passwordEye"),
                        ) {
                            Icon(
                                imageVector = if (showPassword) MiuixIcons.Hide else MiuixIcons.Show,
                                contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.onBackgroundVariant,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(32.dp))
                SwitchRow(
                    title = "记住账户",
                    summary = "使用系统密钥加密保存在此设备",
                    checked = draft.remember,
                    enabled = !locked,
                ) { checked ->
                    onDraftChange(draft.copy(remember = checked))
                    // 关闭记住账户立即清除磁盘密文。
                    if (!checked && draft.loaded) controller.forgetAccount()
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (online != null) {
                            controller.logout(accountOf(draft))
                        } else {
                            controller.login(accountOf(draft))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("auth"),
                    enabled = !busy && !(online == null && state.campus == CampusState.OffCampus),
                    colors = if (online == null) {
                        ButtonDefaults.buttonColorsPrimary()
                    } else {
                        // 原项目注销按钮为红色，这里用主题 error 色作底色。
                        ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.error,
                            contentColor = MiuixTheme.colorScheme.onError,
                        )
                    },
                ) { Text(if (online != null) "注销当前连接" else "登录校园网") }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "保活设置",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onOpenKeepAlive() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        text = "连接设置",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .clickable { onOpenConnection() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(LocalBottomBarInset.current))
        }
    }
}
