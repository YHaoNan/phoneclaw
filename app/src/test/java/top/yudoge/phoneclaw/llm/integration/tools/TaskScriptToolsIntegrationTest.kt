package top.yudoge.phoneclaw.llm.integration.tools

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yudoge.phoneclaw.app.AppContainer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class TaskScriptToolsIntegrationTest {

    @Before
    fun setup() {
        AppContainer.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `list tool should return created script`() {
        val facade = AppContainer.getInstance().taskScriptFacade
        facade.createScript("checkin", "daily checkin", "return 'ok'")
        val result = ListTaskScriptsTool().listTaskScripts()
        assertTrue(result.contains("checkin"))
    }

    @Test
    fun `execute tool should fail fast when script missing`() {
        val result = ExecuteTaskScriptTool().executeTaskScript("missing-id")
        assertTrue(result.contains("Script not found"))
    }
}
