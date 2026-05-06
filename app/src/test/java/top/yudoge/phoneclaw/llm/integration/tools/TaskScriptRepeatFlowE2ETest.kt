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
        val saveOutput = SaveTaskScriptTool().saveTaskScript(
            name = "enterprise-checkin",
            summary = "Clock in for enterprise app",
            codeContent = "return 'checkin-done-v1'"
        )
        assertTrue(saveOutput.contains("Script saved"))

        val listOutput = ListTaskScriptsTool().listTaskScripts()
        assertTrue(listOutput.contains("enterprise-checkin"))

        val contentOutput = GetTaskScriptContentTool().getTaskScriptContent(
            scriptId = null,
            scriptName = "enterprise-checkin"
        )
        assertTrue(contentOutput.contains("return 'checkin-done-v1'"))

        val updateOutput = SaveTaskScriptTool().saveTaskScript(
            name = "enterprise-checkin",
            summary = "Clock in for enterprise app",
            codeContent = "return 'checkin-done-v2'"
        )
        assertTrue(updateOutput.contains("Script saved"))
    }
}
