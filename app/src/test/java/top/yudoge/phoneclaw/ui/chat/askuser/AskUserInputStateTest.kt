package top.yudoge.phoneclaw.ui.chat.askuser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AskUserInputStateTest {

    @Test
    fun `canConfirm should be false when nothing selected`() {
        assertFalse(AskUserInputState.canConfirm(selectedTag = null, otherText = null))
    }

    @Test
    fun `canConfirm should be true for regular option`() {
        assertTrue(AskUserInputState.canConfirm(selectedTag = 0, otherText = null))
    }

    @Test
    fun `canConfirm should require text for other option`() {
        assertFalse(
            AskUserInputState.canConfirm(
                selectedTag = AskUserInputState.OTHER_OPTION_TAG,
                otherText = " "
            )
        )
        assertTrue(
            AskUserInputState.canConfirm(
                selectedTag = AskUserInputState.OTHER_OPTION_TAG,
                otherText = "自定义答案"
            )
        )
    }
}

