package top.yudoge.phoneclaw.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.snackbar.Snackbar
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ActivityChatBinding
import top.yudoge.phoneclaw.databinding.DialogAskUserBottomSheetBinding
import top.yudoge.phoneclaw.llm.domain.objects.AskUserAnswerSource
import top.yudoge.phoneclaw.llm.domain.objects.AskUserRequest
import top.yudoge.phoneclaw.llm.domain.objects.AskUserResponse
import top.yudoge.phoneclaw.ui.chat.askuser.AskUserBottomSheetFactory
import top.yudoge.phoneclaw.ui.chat.askuser.AskUserInputState
import top.yudoge.phoneclaw.ui.chat.drawer.DrawerFragment
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import top.yudoge.phoneclaw.ui.settings.SettingsActivity

class ChatActivity : AppCompatActivity(), ChatContract.View {

    private lateinit var binding: ActivityChatBinding
    private lateinit var presenter: ChatContract.Presenter
    private lateinit var messageAdapter: MessageAdapter
    private var modelNames: List<String> = emptyList()
    private var selectedModelIndex: Int = 0
    private var askUserDialog: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            
            val bottomPadding = if (imeInsets.bottom > 0) {
                imeInsets.bottom
            } else {
                val extraPadding = (8 * resources.displayMetrics.density).toInt()
                systemBars.bottom + extraPadding
            }
            binding.inputContainer.setPadding(0, 0, 0, bottomPadding)
            insets
        }

        presenter = ChatPresenter()
        presenter.attachView(this)

        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupInputArea()

        presenter.loadSession(null)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            (supportFragmentManager.findFragmentById(R.id.drawer_fragment_container) as? DrawerFragment)
                ?.loadSessions()
            binding.drawerLayout.openDrawer(binding.drawerFragmentContainer)
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    navigateToSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDrawer() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.drawer_fragment_container, DrawerFragment())
            .commit()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupInputArea() {
        binding.sendButton.setOnClickListener {
            val text = binding.inputEditText.text.toString()
            if (text.isNotBlank()) {
                presenter.sendMessage(text)
                binding.inputEditText.text?.clear()
            }
        }

        binding.stopButton.setOnClickListener {
            presenter.stopAgent()
        }

        binding.imageButton.setOnClickListener {
        }

        binding.modelSelector.setOnClickListener {
            if (modelNames.isNotEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("选择模型")
                    .setSingleChoiceItems(modelNames.toTypedArray(), selectedModelIndex) { dialog, which ->
                        if (which != selectedModelIndex) {
                            selectedModelIndex = which
                            presenter.selectModel(which)
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun showMessages(messages: List<MessageItem>) {
        messageAdapter.submitList(messages)
    }

    override fun appendMessage(message: MessageItem) {
        messageAdapter.addItem(message)
        scrollToBottom()
    }

    override fun updateMessage(position: Int, message: MessageItem) {
        messageAdapter.updateItem(position, message)
    }

    override fun removeMessage(position: Int) {
        messageAdapter.removeItem(position)
    }

    override fun scrollToBottom() {
        binding.recyclerView.smoothScrollToPosition(messageAdapter.itemCount)
    }

    override fun showThinking() {
        messageAdapter.addThinkingItem()
        scrollToBottom()
    }

    override fun hideThinking() {
        messageAdapter.removeThinkingItem()
    }

    override fun updateThinkingStatus(status: String) {
        messageAdapter.updateThinkingStatus(status)
    }

    override fun setSendButtonEnabled(enabled: Boolean) {
        binding.sendButton.isEnabled = enabled
        binding.inputEditText.isEnabled = enabled
    }

    override fun showStopButton() {
        binding.sendButton.visibility = View.GONE
        binding.stopButton.visibility = View.VISIBLE
    }

    override fun hideStopButton() {
        binding.sendButton.visibility = View.VISIBLE
        binding.stopButton.visibility = View.GONE
    }

    override fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun showSessionTitle(title: String) {
        binding.toolbar.title = title
    }

    override fun showModelSelector(models: List<String>, selectedIndex: Int) {
        this.modelNames = models
        this.selectedModelIndex = selectedIndex
        if (models.isNotEmpty() && selectedIndex in models.indices) {
            val displayName = models[selectedIndex]
            binding.modelSelector.text = if (displayName.length > 6) displayName.take(6) + "…" else displayName
        }
    }

    override fun closeDrawer() {
        binding.drawerLayout.closeDrawer(binding.drawerFragmentContainer)
    }

    override fun openDrawer() {
        binding.drawerLayout.openDrawer(binding.drawerFragmentContainer)
    }
    
    override fun updateAgentMessageContent(content: String) {
        val lastPosition = messageAdapter.itemCount - 1
        if (lastPosition >= 0) {
            val lastItem = messageAdapter.getMessageItemAt(lastPosition)
            if (lastItem is MessageItem.AgentMessage) {
                val updated = lastItem.copy(content = content)
                messageAdapter.updateItem(lastPosition, updated)
                scrollToBottom()
            }
        }
    }
    
    override fun getCurrentMessageCount(): Int = messageAdapter.itemCount
    
    override fun getItemAt(position: Int): MessageItem = messageAdapter.getMessageItemAt(position)

    override fun showAskUserBottomSheet(
        request: AskUserRequest,
        onComplete: (AskUserResponse) -> Unit
    ) {
        askUserDialog?.dismiss()

        val (dialog, sheetBinding) = AskUserBottomSheetFactory.create(this, layoutInflater)
        askUserDialog = dialog

        sheetBinding.askUserQuestion.text = request.question

        val optionButtons = mutableListOf<MaterialRadioButton>()
        request.answers.forEachIndexed { index, answer ->
            val option = MaterialRadioButton(this).apply {
                id = View.generateViewId()
                text = answer
                tag = index
            }
            sheetBinding.askUserOptions.addView(option)
            optionButtons.add(option)
        }

        val otherButton = MaterialRadioButton(this).apply {
            id = View.generateViewId()
            text = getString(R.string.ask_user_other_label)
            tag = AskUserInputState.OTHER_OPTION_TAG
        }
        sheetBinding.askUserOptions.addView(otherButton)
        optionButtons.add(otherButton)

        fun updateConfirmEnabled() {
            val selectedId = sheetBinding.askUserOptions.checkedRadioButtonId
            val selected = optionButtons.firstOrNull { it.id == selectedId }
            val selectedTag = selected?.tag as? Int
            val isOther = selectedTag == AskUserInputState.OTHER_OPTION_TAG
            sheetBinding.askUserOtherInputLayout.visibility = if (isOther) View.VISIBLE else View.GONE
            sheetBinding.askUserConfirmButton.isEnabled = AskUserInputState.canConfirm(
                selectedTag = selectedTag,
                otherText = sheetBinding.askUserOtherInput.text?.toString()
            )
        }

        sheetBinding.askUserOptions.setOnCheckedChangeListener { _, _ ->
            updateConfirmEnabled()
        }
        sheetBinding.askUserOtherInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updateConfirmEnabled()
            }
        })

        var completed = false
        fun complete(response: AskUserResponse) {
            if (completed) return
            completed = true
            onComplete(response)
        }

        sheetBinding.askUserCancelButton.setOnClickListener {
            complete(
                AskUserResponse(
                    requestId = request.requestId,
                    confirmed = false,
                    source = AskUserAnswerSource.CANCELLED,
                    error = "User cancelled"
                )
            )
            dialog.dismiss()
        }

        sheetBinding.askUserConfirmButton.setOnClickListener {
            val selectedId = sheetBinding.askUserOptions.checkedRadioButtonId
            val selected = optionButtons.firstOrNull { it.id == selectedId }
            val isOther = (selected?.tag as? Int) == AskUserInputState.OTHER_OPTION_TAG
            val answer = if (isOther) {
                sheetBinding.askUserOtherInput.text?.toString()?.trim()
            } else {
                selected?.text?.toString()
            }
            val source = if (isOther) AskUserAnswerSource.OTHER else AskUserAnswerSource.OPTION

            complete(
                AskUserResponse(
                    requestId = request.requestId,
                    confirmed = true,
                    answer = answer,
                    source = source
                )
            )
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            if (!completed) {
                complete(
                    AskUserResponse(
                        requestId = request.requestId,
                        confirmed = false,
                        source = AskUserAnswerSource.CANCELLED,
                        error = "Dialog dismissed"
                    )
                )
            }
            askUserDialog = null
        }
        dialog.show()
    }

    fun loadSession(sessionId: String) {
        presenter.loadSession(sessionId)
    }

    fun createNewSession() {
        presenter.createNewSession()
    }

    fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onDestroy() {
        askUserDialog?.dismiss()
        super.onDestroy()
        presenter.detachView()
    }
}
