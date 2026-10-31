package com.hz6826.xjtunetlogin.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Settings

/** 液态玻璃底栏是浮动的，页面内容需要在滚动内容末尾多留这段距离。 */
val LocalBottomBarInset = compositionLocalOf<Dp> { 0.dp }

data class BottomTab(val label: String, val icon: ImageVector)

val bottomTabs: List<BottomTab> = listOf(
    BottomTab("登录", MiuixIcons.Home),
    BottomTab("记录", MiuixIcons.Recent),
    BottomTab("信息", MiuixIcons.Info),
    BottomTab("设置", MiuixIcons.Settings),
)

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        bottomTabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = tab.icon,
                label = tab.label,
            )
        }
    }
}
