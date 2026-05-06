package top.yudoge.phoneclaw.ui.settings.taskscript

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class TaskScriptUiSmokeTest {

    @Test
    fun `list activity should launch`() {
        val activity = Robolectric.buildActivity(TaskScriptListActivity::class.java).setup().get()
        assertNotNull(activity)
    }
}
