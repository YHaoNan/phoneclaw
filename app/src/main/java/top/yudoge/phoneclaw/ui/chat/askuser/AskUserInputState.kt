package top.yudoge.phoneclaw.ui.chat.askuser

object AskUserInputState {
    const val OTHER_OPTION_TAG = -1

    fun canConfirm(selectedTag: Int?, otherText: String?): Boolean {
        if (selectedTag == null) return false
        if (selectedTag == OTHER_OPTION_TAG) {
            return !otherText.isNullOrBlank()
        }
        return true
    }
}

