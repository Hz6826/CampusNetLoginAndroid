package com.hz6826.xjtunetlogin

import androidx.compose.ui.semantics.SemanticsProperties
import com.hz6826.xjtunetlogin.data.AccountStore
import com.hz6826.xjtunetlogin.model.Account
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class AndroidSmokeTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** 在 Activity 启动前清空已保存账户，避免上一次使用留下的账户触发自动登录。 */
    @get:Rule(order = 0)
    val clearStoredAccount: TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                AccountStore(context).save(Account())
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keystoreRoundTripAndForget() {
        val store = AccountStore(context)
        val account = Account("smoke-test-user", "P@ss\"\\中文", remember = true, autoLogin = true)
        store.save(account)
        assertEquals(account, store.load())
        val prefs = context.getSharedPreferences("account", 0)
        val payload = prefs.getString("ciphertext", "")!!
        assertFalse(payload.contains(account.username))
        assertFalse(payload.contains(account.password))
        store.save(account.copy(remember = false, autoLogin = false, domain = "@staff"))
        assertFalse(prefs.contains("ciphertext"))
        assertEquals("", store.load().username)
        assertEquals("", store.load().password)
        assertEquals("@staff", store.load().domain)
    }

    @Test
    fun draftSurvivesActivityRecreation() {
        awaitReady()
        // 密码框默认掩码，先切到明文才能从语义树断言原始内容。
        composeRule.onNodeWithTag("passwordEye").performClick()
        composeRule.onNodeWithTag("account").performTextReplacement("draft-user")
        composeRule.onNodeWithTag("password").performTextReplacement("draft-secret")
        composeRule.activityRule.scenario.recreate()
        awaitReady()
        assertEquals("draft-user", editableText("account"))
        assertEquals("draft-secret", editableText("password"))
    }

    @Test
    fun emptyAccountShowsValidationBeforeNetworkRequest() {
        awaitReady()
        composeRule.onNodeWithTag("account").performTextReplacement("")
        composeRule.onNodeWithTag("password").performTextReplacement("")
        composeRule.onNodeWithTag("auth").performScrollTo().performClick()
        composeRule.onNodeWithText("请输入校园网账号").assertExists()
    }

    private fun awaitReady() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithTag("auth").fetchSemanticsNodes().isNotEmpty() &&
                runCatching { composeRule.onNodeWithTag("auth").assertIsEnabled() }.isSuccess
        }
        // 真机可能已有在线会话（账户与连接设置在在线时锁定），先注销保证输入可用。
        if (authLabel() == "注销当前连接") {
            composeRule.onNodeWithTag("auth").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 30_000) { authLabel() == "登录校园网" }
        }
        composeRule.waitUntil(timeoutMillis = 20_000) {
            runCatching { composeRule.onNodeWithTag("account").assertIsEnabled() }.isSuccess
        }
    }

    private fun authLabel(): String =
        composeRule.onNodeWithTag("auth").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }

    private fun editableText(tag: String): String {
        val config = composeRule.onNodeWithTag(tag).fetchSemanticsNode().config
        val entry = config.firstOrNull { it.key == SemanticsProperties.EditableText }
        return (entry?.value as? AnnotatedString)?.text.orEmpty()
    }
}
