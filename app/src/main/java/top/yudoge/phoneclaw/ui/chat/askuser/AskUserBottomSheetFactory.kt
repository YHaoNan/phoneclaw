package top.yudoge.phoneclaw.ui.chat.askuser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import top.yudoge.phoneclaw.databinding.DialogAskUserBottomSheetBinding

object AskUserBottomSheetFactory {
    fun create(
        context: Context,
        layoutInflater: LayoutInflater
    ): Pair<BottomSheetDialog, DialogAskUserBottomSheetBinding> {
        val binding = DialogAskUserBottomSheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(binding.root)
        return dialog to binding
    }

    fun createWithContentView(context: Context, contentView: View): BottomSheetDialog {
        return BottomSheetDialog(context).apply {
            setContentView(contentView)
        }
    }
}
