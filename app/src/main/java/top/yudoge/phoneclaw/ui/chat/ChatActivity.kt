package top.yudoge.phoneclaw.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.core.AgentStatusManager
import top.yudoge.phoneclaw.databinding.ActivityChatBinding
import top.yudoge.phoneclaw.data.message.MessageRepositoryImpl
import top.yudoge.phoneclaw.data.session.SessionRepositoryImpl
import top.yudoge.phoneclaw.llm.agent.AgentOrchestrator
import top.yudoge.phoneclaw.domain.ModelSelector
import top.yudoge.phoneclaw.domain.SessionManager
import top.yudoge.phoneclaw.llm.provider.ModelProviderRepositoryImpl
import top.yudoge.phoneclaw.llm.provider.ModelRepositoryImpl
import top.yudoge.phoneclaw.ui.chat.drawer.DrawerFragment
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import top.yudoge.phoneclaw.ui.settings.SettingsActivity

class ChatActivity : AppCompatActivity(), ChatContract.View {

    private lateinit var binding: ActivityChatBinding
    private lateinit var presenter: ChatContract.Presenter
    private lateinit var messageAdapter: MessageAdapter
    private var modelNames: List<String> = emptyList()
    private var selectedModelIndex: Int = 0

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

        val providerRepo = ModelProviderRepositoryImpl(this)
        val modelRepo = ModelRepositoryImpl(this)
        val sessionRepo = SessionRepositoryImpl(this)
        val messageRepo = MessageRepositoryImpl(this)

        val sessionManager = SessionManager(sessionRepo)
        val modelSelector = ModelSelector(providerRepo, modelRepo)
        val agentOrchestrator = AgentOrchestrator(this, modelSelector)

        presenter = ChatPresenter(sessionManager, messageRepo, modelSelector, agentOrchestrator)
        presenter.attachView(this)

        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupInputArea()
        observeAgentStatus()

        presenter.loadSession(null)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
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

    private fun observeAgentStatus() {
        lifecycleScope.launch {
            AgentStatusManager.status.collect { status ->
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
        println("[ChatActivity] updateAgentMessageContent: ${content.take(50)}...")
        val lastPosition = messageAdapter.itemCount - 1
        println("[ChatActivity] lastPosition: $lastPosition, itemCount: ${messageAdapter.itemCount}")
        if (lastPosition >= 0) {
            val lastItem = messageAdapter.getMessageItemAt(lastPosition)
            println("[ChatActivity] lastItem type: ${lastItem::class.simpleName}")
            if (lastItem is MessageItem.AgentMessage) {
                val updated = lastItem.copy(content = content)
                messageAdapter.updateItem(lastPosition, updated)
                scrollToBottom()
                println("[ChatActivity] AgentMessage updated")
            }
        }
    }
    
    override fun getCurrentMessageCount(): Int = messageAdapter.itemCount
    
    override fun getItemAt(position: Int): MessageItem = messageAdapter.getMessageItemAt(position)

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
        super.onDestroy()
        presenter.detachView()
    }
}
