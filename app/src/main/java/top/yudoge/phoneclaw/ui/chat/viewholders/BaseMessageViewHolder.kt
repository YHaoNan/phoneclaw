package top.yudoge.phoneclaw.ui.chat.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

abstract class BaseMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: MessageItem)
}
