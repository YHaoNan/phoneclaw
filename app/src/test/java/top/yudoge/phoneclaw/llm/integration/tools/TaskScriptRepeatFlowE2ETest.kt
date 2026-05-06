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
class TaskScriptRepeatFlowE2ETest {

    @Before
    fun setup() {
        AppContainer.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `repeat task should reuse script on second run`() {
        val facade = AppContainer.getInstance().taskScriptFacade
        val created = facade.createScript(
            name = "enterprise-checkin",
            summary = "Clock in for enterprise app",
            codeContent = "return 'checkin-done'"
        )
        assertTrue(created)

        val listOutput = ListTaskScriptsTool().listTaskScripts()
        assertTrue(listOutput.contains("enterprise-checkin"))

        val id = facade.getScriptByName("enterprise-checkin")!!.id
        val executeOutput = ExecuteTaskScriptTool().executeTaskScript(id)
        assertTrue(executeOutput.contains("Script executed successfully"))
    }
}
