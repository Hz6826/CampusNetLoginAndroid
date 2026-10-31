<!--suppress HtmlDeprecatedAttribute -->

<div align="center">

# <img src="doc/full-icon-half.png" alt="仙交网络助手" width="400" />

### 基于 Kotlin 与 Miuix 的西安交通大学校园网鉴权 Android 客户端

[![GitHub License](https://img.shields.io/github/license/Hz6826/XJTUNetLoginAndroid)](LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/Hz6826/XJTUNetLoginAndroid)](https://github.com/Hz6826/XJTUNetLoginAndroid/issues)
[![GitHub PRs](https://img.shields.io/github/issues-pr/Hz6826/XJTUNetLoginAndroid)](https://github.com/Hz6826/XJTUNetLoginAndroid/pulls)
[![CI](https://img.shields.io/github/actions/workflow/status/Hz6826/XJTUNetLoginAndroid/ci.yml?label=ci&logo=github)](https://github.com/Hz6826/XJTUNetLoginAndroid/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Hz6826/XJTUNetLoginAndroid?label=latest&logo=github)](https://github.com/Hz6826/XJTUNetLoginAndroid/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Hz6826/XJTUNetLoginAndroid/total?logo=github)](https://github.com/Hz6826/XJTUNetLoginAndroid/releases)
[![Platform](https://img.shields.io/badge/platform-Android-blue?logo=android)](https://github.com/Hz6826/XJTUNetLoginAndroid/releases/latest)
![GitHub Stars](https://img.shields.io/github/stars/Hz6826/XJTUNetLoginAndroid?style=social)

<img src="doc/meme.png" alt="老实登录，很枯燥吧" width="800"/>

> “老实登录，很枯燥吧”

</div>

## 快速开始

请在 [Releases](https://github.com/Hz6826/XJTUNetLoginAndroid/releases) 界面获取最新版本；若暂时没有可用的 Release，可在 [Actions](https://github.com/Hz6826/XJTUNetLoginAndroid/actions/workflows/ci.yml) 的构建产物中下载 debug APK，或按下方[构建](#构建)说明在本地打包。

> 想要在电脑上体验相同功能吗？欢迎访问 [XJTUNetLogin](https://github.com/Hz6826/XJTUNetLogin)。

- 支持 Android 8.0（API 26）及以上
- 使用与桌面版相同的门户协议，账号密码经 Android Keystore + AES-GCM 加密保存在本机

> [!WARNING]
> **安全说明**
> - 账户密码只在 Android Keystore 中保存密钥，磁盘上只有密文；关闭「记住账户」会立即删除已保存的账户。
> - 运行记录默认不记录 HTTP 请求；开启「记录 HTTP 日志」后只记录方法与路径，不记录参数、密码或 token，请勿随意分享包含账号信息的截图。

## 功能列表

- :white_check_mark: 已完成
- :memo: 进行中
- :x: 近期无计划实现

| 功能          | 实现情况               |
|-------------|--------------------|
| 登录/注销       | :white_check_mark: |
| 测试连接        | :white_check_mark: |
| 获取在线信息      | :white_check_mark: |
| 账户加密存储      | :white_check_mark: |
| 自动登录 / 前台保活 | :white_check_mark: |
| 开机自启        | :white_check_mark: |
| 深色模式 / 主题色  | :white_check_mark: |
| 液态玻璃底栏      | :white_check_mark: |
| 记录 HTTP 日志  | :white_check_mark: |
| 自动检查更新      | :white_check_mark: |
| SHA-256 校验更新包 | :white_check_mark: |
| 托盘最小化等桌面特性  | :x:                 |
| 多平台（Windows/macOS/Linux） | :x: ^1             |

^1: 桌面版（Windows/macOS/Linux）：[XJTUNetLogin](https://github.com/Hz6826/XJTUNetLogin)。

应用内更新通过 GitHub Releases 的匿名 API 检查，默认在启动时静默检查（每天最多一次），可在「设置 → 更新」中开启预览版或手动检查。下载 APK 后会先按 API 提供的 SHA-256 校验，再交给系统安装器；首次安装需要允许「安装未知应用」权限。

## 使用

1. 打开应用，输入学号或工号及校园网密码。默认账户域为 `@stu`，AC ID 为 `1`，认证地址为 `http://10.6.21.2`；可在「设置 → 连接 → 连接设置」中修改。
2. 如需后台自动认证，先在登录页勾选「记住账户」并登录成功，再进入「设置 → 连接 → 保活配置」开启「前台保活与自动登录」。也可在连接 Wi-Fi 后点击「登录校园网」手动登录。登录成功后账户与连接设置自动锁定，按钮变为「注销当前连接」；注销后解锁。
3. Android 10 及以上可在「连接设置」中点击「启用校园 Wi-Fi 自动连接」，按系统提示允许后，系统会将开放网络 `XJTU-STU` 纳入自动连接候选。Android 8 和 9 可在系统 Wi-Fi 设置中连接并启用自动连接。
4. 底部有「登录」「记录」「信息」「设置」四个标签页。登录页顶部状态卡显示连接状态（蓝「等待连接」/ 灰「无需登录」/ 绿「已连接」），卡内按钮刷新状态并检测外网。设置页可切换「深色模式」（跟随系统/浅色/深色）、「主题色」（Miuix 蓝（默认）、跟随系统取色或仙交蓝/校园绿等预设）和「液态玻璃底栏」（Android 12+）；「连接设置」「保活配置」「关于」「贡献者」「参考项目及许可」都是独立二级页。

「记住账户」使用 Android Keystore 和 AES-GCM 加密存储账户配置。关闭后立即清除已保存的数据。自动登录与前台保活合并为「保活配置」里的一个开关，开启条件为登录成功过且当前记住了账户。

开启「前台保活与自动登录」后，应用以前台服务常驻，并在开机后自动恢复（需系统允许自启动）：通知标题是当前状态，正文显示账号与已用时长，并提供登录/注销与关闭应用按钮；服务在 Wi-Fi 接入时检查认证状态并每 30 秒刷新，发现校园网未认证时自动登录。自动自检不改变界面状态，输入账户密码时不会被中断。手动注销后暂停自动登录，重新连接 Wi-Fi 后恢复。保活页提供「忽略电池优化」和「应用电池设置」两个系统入口，用于减少后台被清理。系统对通知的显示受通知权限影响；强制停止应用后需重新打开应用。认证和外网检测均通过 Wi-Fi 发送。

## 外观与主题

设置 → 外观 可以切换主题，选择会即时保存：

- 「Miuix 蓝（默认）」：Miuix 原生浅色/深色配色
- 「跟随系统取色」：尝试读取系统 Monet 取色（Android 12+），读不到时回落到蓝色
- 预设主题色：仙交蓝、校园绿、梧桐橙、创新港青、梧桐紫、交大红（基于种子色生成整套配色）
- 「液态玻璃底栏」：Android 12+ 可开启 Apple 风格浮动胶囊底栏

## 原理

本仓库协议实现参考了登录门户 JS 与桌面版 [XJTUNetLogin](https://github.com/Hz6826/XJTUNetLogin)，核心代码在 `net/` 目录（`SrunClient`、`SrunCrypto`、`WifiTransport`）：

### 登录

1. 第一次 `requestChallenge`：向服务器请求 challenge token，附带 `username`，此时 `ip` 为空。本次 challenge 丢弃，仅通过响应体获取服务器处的真实内网 IP。
2. 第二次 `requestChallenge`：携带步骤 1 中获取的 `ip`，重新获取 challenge token，服务器返回 `token`。
3. `requestAuth`：登录主体，进行主要的加密、签名操作：
   1. `SrunCrypto.hmacMd5`：使用 HMAC-MD5 加密，返回 32 位小写 hex 字符串，存入 `password`。
   2. `SrunCrypto.encodeUserInfo`：拼接 JSON，使用 XXTEA 将 JSON 用 `token` 加密，返回的 `{SRBX1}` 存入 `info`。
   3. `SrunCrypto.buildChksum`：将上述参数等拼接后计算 SHA-1 作为校验和。
   4. 请求：携带所有参数向服务器 `/cgi-bin/srun_portal` 发送 `GET` 请求，`action` 为 `login`，并读取响应。
4. `verify`：若登录成功，确认在线状态，默认 3 次重试。

### 注销

携带 `username`、`ip`、`ac_id` 等向服务器 `/cgi-bin/srun_portal` 发送 `action=logout` 请求（使用服务器返回的在线 IP）；若请求被接受，再通过轮询确认已下线，默认 3 次。

### 查询

直接向服务器 `/cgi-bin/rad_user_info` 发送 `GET` 请求，服务器返回 JSON/JSONP 信息，客户端解析后展示账号、IP、MAC、计费策略、已用流量/时长、余额和在线设备。

## 构建

在 Android Studio 中打开本目录，使用 **JDK 21 或 25**（编译目标为 Java 21），安装 **Android SDK Platform 37** 和 **Build-Tools 37.0.0**。通过 Android Studio 配置 SDK，或在本目录的 `local.properties` 中设置 `sdk.dir`。

Windows：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

macOS / Linux：

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

安装包输出：`app/build/outputs/apk/debug/app-debug.apk`。正式发布时通过 Android Studio 的 Generate Signed App Bundle / APK 使用自己的签名密钥。

协议测试涵盖加密固定向量、JSON/JSONP、两次 challenge、认证拒绝、在线确认重试和注销时使用服务器在线 IP。校园网实机验收需在校内 Wi-Fi 上完成登录、刷新、注销及重连自动登录。

连接模拟器或 Android 设备后，可运行 `:app:connectedDebugAndroidTest`。注意：仪器测试会清空设备上已保存的账户，并可能注销当前校园网会话，请勿在日常使用的设备上直接运行。

## 许可证

本项目以 **[GPL-3.0](LICENSE)** 发布。使用的开源组件与参考项目见应用内「关于 → 参考项目及许可」。

桌面版项目 [XJTUNetLogin](https://github.com/Hz6826/XJTUNetLogin) 以 MIT License 独立开源。

## 致谢

- [@The-time-forever](https://github.com/The-time-forever)：做出了应用初版并提供宝贵的框架参考。
- [@Triangle-GitHub](https://github.com/Triangle-GitHub)：提供了应用保活的参考。
- [Miuix](https://github.com/compose-miuix-ui/miuix)：Compose UI 组件库。

### 贡献者

<a href="https://github.com/Hz6826/XJTUNetLoginAndroid/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Hz6826/XJTUNetLoginAndroid" alt="Contributors" />
</a>

## Star History

_如果觉得这个项目对你有帮助的话，麻烦点一下右上角的 Star，你的支持是我最大的动力，谢谢喵~_

[![Star History Chart](https://api.star-history.com/svg?repos=Hz6826/XJTUNetLoginAndroid&type=Date)](https://www.star-history.com/#Hz6826/XJTUNetLoginAndroid&Date)
