# PhoneClaw UI 技术方案

> 基于 Material Design 3 + 传统 XML 布局 + MVP 架构

## 1. 技术栈

### 1.1 核心依赖

```kotlin
// app/build.gradle.kts

dependencies {
    // Material Design 3
    implementation("com.google.android.material:material:1.11.0")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // UI Components
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Image Loading
    implementation("io.coil-kt:coil:2.5.0")
    
    // 现有依赖保持不变
    implementation(files("libs/hanai-1.0-SNAPSHOT.jar"))
    implementation("dev.langchain4j:langchain4j-core:<version>")`n    implementation("dev.langchain4j:langchain4j-open-ai:<version>")
    // ...
}
```

### 1.2 Android 配置

```kotlin
android {
    namespace = "top.yudoge.phoneclaw"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "top.yudoge.phoneclaw"
        minSdk = 33
        targetSdk = 36
    }
    
    buildFeatures {
        viewBinding = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

## 2. 目录结构

```
app/src/main/java/top/yudoge/phoneclaw/
├── ui/
│   ├── chat/
│   │   ├── ChatActivity.kt                 # 主对话页面
│   │   ├── ChatContract.kt                 # MVP 契约接口
│   │   ├── ChatPresenter.kt                # Presenter 实现
│   │   ├── MessageAdapter.kt               # 消息列表适配器
│   │   ├── model/
│   │   │   └── MessageItem.kt              # 消息数据模型
│   │   ├── viewholders/
│   │   │   ├── BaseMessageViewHolder.kt    # 基类 ViewHolder
│   │   │   ├── UserMessageViewHolder.kt    # 用户消息
│   │   │   ├── AgentMessageViewHolder.kt   # Agent 消息
│   │   │   ├── ToolCallViewHolder.kt       # 工具调用
│   │   │   ├── SkillCallViewHolder.kt      # 技能调用
│   │   │   └── ThinkingViewHolder.kt       # 思考状态
│   │   └── drawer/
│   │       ├── DrawerFragment.kt           # 侧滑抽屉
│   │       ├── SessionAdapter.kt           # 会话列表适配器
│   │       └── SessionGroupAdapter.kt      # 分组会话适配器
│   │
│   ├── settings/
│   │   ├── SettingsActivity.kt             # 设置主页面
│   │   ├── model/
│   │   │   ├── ModelSettingsFragment.kt    # 模型设置
│   │   │   ├── ModelEditActivity.kt        # 模型编辑
│   │   │   └── ModelAdapter.kt             # 模型列表适配器
│   │   ├── skill/
│   │   │   ├── SkillSettingsFragment.kt    # 技能设置
│   │   │   ├── SkillDetailActivity.kt      # 技能详情
│   │   │   └── SkillAdapter.kt             # 技能列表适配器
│   │   ├── PermissionSettingsFragment.kt   # 权限设置
│   │   └── ScriptServerFragment.kt         # 脚本服务器
│   │
│   ├── floating/
│   │   ├── FloatingWindowService.kt        # 悬浮窗服务
│   │   └── FloatingViewFactory.kt          # 悬浮窗视图工厂
│   │
│   └── components/
│       ├── ExpandableCardView.kt           # 可展开卡片
│       ├── StatusBadgeView.kt              # 状态徽章
│       └── ModelSpinner.kt                 # 模型选择下拉框
│
├── core/
│   ├── AgentStatusManager.kt               # 全局状态管理
│   └── SessionManager.kt                   # 会话管理
│
├── db/
│   ├── PhoneClawDbHelper.java              # 现有数据库
│   └── MessageRepository.kt                # 消息仓库 (新增)
│
└── (现有代码保持不变)
```

### 2.1 资源目录

```
app/src/main/res/
├── layout/
│   ├── activity_chat.xml                   # 主对话页面
│   ├── fragment_drawer.xml                 # 侧滑抽屉
│   ├── item_session.xml                    # 会话项
│   ├── item_session_group.xml              # 会话分组标题
│   ├── item_message_user.xml               # 用户消息
│   ├── item_message_agent.xml              # Agent 消息
│   ├── item_message_tool.xml               # 工具调用
│   ├── item_message_skill.xml              # 技能调用
│   ├── item_message_thinking.xml           # 思考状态
│   ├── activity_settings.xml               # 设置页面
│   ├── fragment_model_settings.xml         # 模型设置
│   ├── fragment_skill_settings.xml         # 技能设置
│   ├── fragment_permission_settings.xml    # 权限设置
│   ├── fragment_script_server.xml          # 脚本服务器
│   ├── layout_floating_window.xml          # 悬浮窗
│   └── view_expandable_card.xml           # 可展开卡片
│
├── layout-land/                            # 横屏布局
│   └── activity_chat.xml
│
├── layout-sw600dp/                         # 平板布局
│   ├── activity_chat.xml
│   └── fragment_drawer.xml
│
├── values/
│   ├── colors.xml                          # 颜色
│   ├── themes.xml                          # 主题
│   ├── styles.xml                          # 样式
│   └── dimens.xml                          # 尺寸
│
├── values-night/                           # 暗色主题
│   └── colors.xml
│
├── drawable/
│   ├── bg_message_user.xml                 # 用户消息背景
│   ├── bg_message_agent.xml                # Agent 消息背景
│   ├── bg_tool_card.xml                    # 工具卡片背景
│   ├── ic_thinking.xml                     # 思考图标
│   ├── ic_tool.xml                         # 工具图标
│   ├── ic_skill.xml                        # 技能图标
│   └── bg_floating_window.xml              # 悬浮窗背景
│
└── menu/
    ├── menu_drawer.xml                     # 抽屉菜单
    └── menu_session_context.xml            # 会话上下文菜单
```

## 3. 核心代码实现

### 3.1 ChatContract.kt

```kotlin
package top.yudoge.phoneclaw.ui.chat

import android.net.Uri
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

interface ChatContract {
    
    interface View {
        fun showMessages(messages: List<MessageItem>)
        fun appendMessage(message: MessageItem)
        fun updateMessage(position: Int, message: MessageItem)
        fun removeMessage(position: Int)
        fun scrollToBottom()
        fun showThinking()
        fun hideThinking()
        fun updateThinkingStatus(status: String)
        fun setSendButtonEnabled(enabled: Boolean)
        fun showStopButton()
        fun hideStopButton()
        fun showError(message: String)
        fun showSessionTitle(title: String)
        fun showModelSelector(models: List<String>, selectedIndex: Int)
        fun navigateToSettings()
    }
    
    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun loadSession(sessionId: String?)
        fun createNewSession()
        fun sendMessage(content: String, images: List<Uri>? = null)
        fun stopAgent()
        fun deleteSession(sessionId: String)
        fun renameSession(sessionId: String, newTitle: String)
        fun selectModel(modelId: String)
        fun toggleInputMode()
    }
}
```

### 3.2 ChatActivity.kt

```kotlin
package top.yudoge.phoneclaw.ui.chat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import top.yudoge.phoneclaw.databinding.ActivityChatBinding
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

class ChatActivity : AppCompatActivity(), ChatContract.View {
    
    private lateinit var binding: ActivityChatBinding
    private lateinit var presenter: ChatContract.Presenter
    private lateinit var messageAdapter: MessageAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        presenter = ChatPresenter(this, this)
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
                    presenter.navigateToSettings()
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
        
        binding.modelSelector.setOnClickListener {
            // Show model selection dialog
        }
        
        binding.imageButton.setOnClickListener {
            // Open image picker
        }
    }
    
    private fun observeAgentStatus() {
        lifecycleScope.launch {
            AgentStatusManager.status.collect { status ->
                updateFloatingWindowStatus(status)
            }
        }
    }
    
    // View implementation
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
        // Update model selector spinner
    }
    
    override fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }
}
```

### 3.3 ChatPresenter.kt

```kotlin
package top.yudoge.phoneclaw.ui.chat

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import top.yudoge.phoneclaw.db.PhoneClawDbHelper
import top.yudoge.phoneclaw.llm.agent.PhoneClawAgent
import top.yudoge.phoneclaw.core.AgentStatusManager
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import java.util.UUID

class ChatPresenter(
    private val context: Context,
    private val dbHelper: PhoneClawDbHelper
) : ChatContract.Presenter {
    
    private var view: ChatContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentSessionId: String? = null
    private var currentAgent: PhoneClawAgent? = null
    private var currentJob: Job? = null
    
    override fun attachView(view: ChatContract.View) {
        this.view = view
    }
    
    override fun detachView() {
        this.view = null
        scope.cancel()
    }
    
    override fun loadSession(sessionId: String?) {
        currentSessionId = sessionId ?: createNewSessionInternal()
        
        val session = dbHelper.getSession(currentSessionId!!)
        if (session != null) {
            view?.showSessionTitle(session.title ?: "新对话")
            
            val messages = dbHelper.getMessages(currentSessionId!!)
            val messageItems = messages.map { record ->
                when (record.role) {
                    "user" -> MessageItem.UserMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        content = record.content
                    )
                    "agent" -> MessageItem.AgentMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        content = record.content
                    )
                    "tool" -> MessageItem.ToolCallMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        toolName = record.toolName ?: "",
                        params = record.toolParams ?: "",
                        result = record.toolResult,
                        state = parseToolState(record.toolState, record.success)
                    )
                    "skill" -> MessageItem.SkillCallMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        skillName = record.toolName ?: "",
                        state = parseToolState(record.toolState, record.success)
                    )
                    else -> null
                }
            }.filterNotNull()
            
            view?.showMessages(messageItems)
        }
    }
    
    override fun createNewSession() {
        currentSessionId = createNewSessionInternal()
        view?.showMessages(emptyList())
        view?.showSessionTitle("新对话")
    }
    
    private fun createNewSessionInternal(): String {
        val sessionId = UUID.randomUUID().toString()
        dbHelper.saveSession(
            sessionId,
            "新对话",
            System.currentTimeMillis(),
            null
        )
        return sessionId
    }
    
    override fun sendMessage(content: String, images: List<Uri>?) {
        val sessionId = currentSessionId ?: return
        
        // Add user message
        val userMessage = MessageItem.UserMessage(
            id = System.currentTimeMillis().toString(),
            timestamp = System.currentTimeMillis(),
            content = content,
            images = images?.map { MessageItem.ImageInfo(it.toString()) }
        )
        view?.appendMessage(userMessage)
        
        // Save to database
        dbHelper.saveMessage(sessionId, PhoneClawDbHelper.MessageRecord().apply {
            role = "user"
            this.content = content
            timestamp = userMessage.timestamp
        })
        
        // Start agent
        startAgent(content)
    }
    
    private fun startAgent(input: String) {
        view?.showThinking()
        view?.setSendButtonEnabled(false)
        view?.showStopButton()
        AgentStatusManager.setStatus(AgentStatusManager.AgentStatus.Thinking())
        
        currentJob = scope.launch {
            try {
                val modelConfig = dbHelper.getDefaultModel()
                    ?: return@launch view?.showError("请先配置模型")
                
                val agent = PhoneClawAgent.builder()
                    .llmClient(createClient(modelConfig))
                    .llmModel(createModel(modelConfig))
                    .skillsDir(File(context.filesDir, "skills"))
                    .build()
                
                currentAgent = agent
                
                // Add thinking message
                val thinkingMessage = MessageItem.ThinkingMessage(
                    id = "thinking",
                    timestamp = System.currentTimeMillis()
                )
                view?.appendMessage(thinkingMessage)
                
                // Run agent
                val result = agent.runSuspend(input)
                
                // Remove thinking and add response
                view?.hideThinking()
                
                val agentMessage = MessageItem.AgentMessage(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    content = result
                )
                view?.appendMessage(agentMessage)
                
                // Save to database
                dbHelper.saveMessage(currentSessionId!!, PhoneClawDbHelper.MessageRecord().apply {
                    role = "agent"
                    this.content = result
                    timestamp = agentMessage.timestamp
                })
                
                // Generate title if first message
                val messages = dbHelper.getMessages(currentSessionId!!)
                if (messages.size <= 2) {
                    generateSessionTitle(input, result)
                }
                
            } catch (e: CancellationException) {
                // User stopped
                view?.hideThinking()
            } catch (e: Exception) {
                view?.hideThinking()
                view?.showError(e.message ?: "发生错误")
            } finally {
                AgentStatusManager.reset()
                view?.setSendButtonEnabled(true)
                view?.hideStopButton()
            }
        }
    }
    
    override fun stopAgent() {
        currentJob?.cancel()
        currentJob = null
        AgentStatusManager.reset()
        view?.hideThinking()
        view?.setSendButtonEnabled(true)
        view?.hideStopButton()
    }
    
    override fun deleteSession(sessionId: String) {
        dbHelper.deleteSession(sessionId)
        if (sessionId == currentSessionId) {
            createNewSession()
        }
    }
    
    override fun renameSession(sessionId: String, newTitle: String) {
        dbHelper.updateSessionTitle(sessionId, newTitle)
        if (sessionId == currentSessionId) {
            view?.showSessionTitle(newTitle)
        }
    }
    
    override fun selectModel(modelId: String) {
        dbHelper.setDefaultModel(modelId)
        currentSessionId?.let {
            dbHelper.updateSessionModel(it, modelId)
        }
    }
    
    override fun toggleInputMode() {
        // Switch between voice and keyboard input
    }
    
    private suspend fun generateSessionTitle(input: String, response: String) {
        // Use LLM to generate a short title
        // Update in database and UI
    }
    
    private fun parseToolState(state: String?, success: Boolean): MessageItem.ToolCallMessage.CallState {
        return when {
            state == "running" -> MessageItem.ToolCallMessage.CallState.RUNNING
            success -> MessageItem.ToolCallMessage.CallState.SUCCESS
            else -> MessageItem.ToolCallMessage.CallState.FAILED
        }
    }
}
```

### 3.4 MessageAdapter.kt

```kotlin
package top.yudoge.phoneclaw.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.databinding.*
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import top.yudoge.phoneclaw.ui.chat.viewholders.*

class MessageAdapter : ListAdapter<MessageItem, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    
    private var thinkingPosition: Int = -1
    
    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AGENT = 1
        private const val TYPE_TOOL = 2
        private const val TYPE_SKILL = 3
        private const val TYPE_THINKING = 4
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MessageItem.UserMessage -> TYPE_USER
            is MessageItem.AgentMessage -> TYPE_AGENT
            is MessageItem.ToolCallMessage -> TYPE_TOOL
            is MessageItem.SkillCallMessage -> TYPE_SKILL
            is MessageItem.ThinkingMessage -> TYPE_THINKING
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserMessageViewHolder(
                ItemMessageUserBinding.inflate(inflater, parent, false)
            )
            TYPE_AGENT -> AgentMessageViewHolder(
                ItemMessageAgentBinding.inflate(inflater, parent, false)
            )
            TYPE_TOOL -> ToolCallViewHolder(
                ItemMessageToolBinding.inflate(inflater, parent, false)
            )
            TYPE_SKILL -> SkillCallViewHolder(
                ItemMessageSkillBinding.inflate(inflater, parent, false)
            )
            TYPE_THINKING -> ThinkingViewHolder(
                ItemMessageThinkingBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserMessageViewHolder -> holder.bind(getItem(position) as MessageItem.UserMessage)
            is AgentMessageViewHolder -> holder.bind(getItem(position) as MessageItem.AgentMessage)
            is ToolCallViewHolder -> holder.bind(getItem(position) as MessageItem.ToolCallMessage)
            is SkillCallViewHolder -> holder.bind(getItem(position) as MessageItem.SkillCallMessage)
            is ThinkingViewHolder -> holder.bind(getItem(position) as MessageItem.ThinkingMessage)
        }
    }
    
    fun addItem(message: MessageItem) {
        val currentList = currentList.toMutableList()
        currentList.add(message)
        submitList(currentList)
    }
    
    fun updateItem(position: Int, message: MessageItem) {
        val currentList = currentList.toMutableList()
        if (position in currentList.indices) {
            currentList[position] = message
            submitList(currentList)
        }
    }
    
    fun removeItem(position: Int) {
        val currentList = currentList.toMutableList()
        if (position in currentList.indices) {
            currentList.removeAt(position)
            submitList(currentList)
        }
    }
    
    fun addThinkingItem() {
        val thinking = MessageItem.ThinkingMessage(
            id = "thinking_temp",
            timestamp = System.currentTimeMillis()
        )
        addItem(thinking)
        thinkingPosition = currentList.size - 1
    }
    
    fun removeThinkingItem() {
        if (thinkingPosition >= 0 && thinkingPosition < currentList.size) {
            removeItem(thinkingPosition)
            thinkingPosition = -1
        }
    }
    
    fun updateThinkingStatus(status: String) {
        if (thinkingPosition >= 0 && thinkingPosition < currentList.size) {
            val thinking = (getItem(thinkingPosition) as? MessageItem.ThinkingMessage)
                ?.copy(status = status)
            thinking?.let { updateItem(thinkingPosition, it) }
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<MessageItem>() {
    override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem == newItem
    }
}
```

### 3.5 AgentStatusManager.kt

```kotlin
package top.yudoge.phoneclaw.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AgentStatusManager {
    
    private val _status = MutableStateFlow<AgentStatus>(AgentStatus.Idle)
    val status: StateFlow<AgentStatus> = _status.asStateFlow()
    
    sealed class AgentStatus {
        object Idle : AgentStatus()
        data class Thinking(val message: String = "思考中...") : AgentStatus()
        data class ToolCalling(
            val name: String,
            val state: CallState
        ) : AgentStatus()
        data class SkillCalling(
            val name: String,
            val state: CallState
        ) : AgentStatus()
    }
    
    enum class CallState { RUNNING, SUCCESS, FAILED }
    
    fun setStatus(status: AgentStatus) {
        _status.value = status
    }
    
    fun setThinking(message: String = "思考中...") {
        _status.value = AgentStatus.Thinking(message)
    }
    
    fun setToolCalling(name: String, state: CallState) {
        _status.value = AgentStatus.ToolCalling(name, state)
    }
    
    fun setSkillCalling(name: String, state: CallState) {
        _status.value = AgentStatus.SkillCalling(name, state)
    }
    
    fun reset() {
        _status.value = AgentStatus.Idle
    }
}
```

### 3.6 FloatingWindowService.kt

```kotlin
package top.yudoge.phoneclaw.ui.floating

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.core.AgentStatusManager
import top.yudoge.phoneclaw.databinding.LayoutFloatingWindowBinding

class FloatingWindowService : LifecycleService() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var binding: LayoutFloatingWindowBinding
    private lateinit var floatingView: View
    
    private var isVisible = false
    
    companion object {
        var isRunning = false
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
        observeAgentStatus()
        isRunning = true
    }
    
    private fun createFloatingWindow() {
        binding = LayoutFloatingWindowBinding.inflate(LayoutInflater.from(this))
        floatingView = binding.root
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100 // Offset from top
        }
        
        windowManager.addView(floatingView, params)
        floatingView.visibility = View.GONE
    }
    
    private fun observeAgentStatus() {
        lifecycleScope.launch {
            AgentStatusManager.status.collect { status ->
                when (status) {
                    is AgentStatusManager.AgentStatus.Idle -> {
                        hideFloatingWindow()
                    }
                    is AgentStatusManager.AgentStatus.Thinking -> {
                        showThinking(status.message)
                    }
                    is AgentStatusManager.AgentStatus.ToolCalling -> {
                        showToolCall(status.name, status.state)
                    }
                    is AgentStatusManager.AgentStatus.SkillCalling -> {
                        showSkillCall(status.name, status.state)
                    }
                }
            }
        }
    }
    
    private fun showThinking(message: String) {
        if (!isVisible) {
            showWithAnimation()
        }
        
        binding.icon.setImageResource(R.drawable.ic_thinking)
        binding.title.text = "思考中"
        binding.status.text = message
        
        animateStatusChange()
    }
    
    private fun showToolCall(name: String, state: AgentStatusManager.CallState) {
        if (!isVisible) {
            showWithAnimation()
        }
        
        binding.icon.setImageResource(R.drawable.ic_tool)
        binding.title.text = name
        
        binding.status.text = when (state) {
            AgentStatusManager.CallState.RUNNING -> "调用中..."
            AgentStatusManager.CallState.SUCCESS -> "成功"
            AgentStatusManager.CallState.FAILED -> "失败"
        }
        
        binding.status.setTextColor(
            when (state) {
                AgentStatusManager.CallState.SUCCESS -> getColor(R.color.success)
                AgentStatusManager.CallState.FAILED -> getColor(R.color.error)
                else -> currentTextColor
            }
        )
        
        animateStatusChange()
    }
    
    private fun showSkillCall(name: String, state: AgentStatusManager.CallState) {
        if (!isVisible) {
            showWithAnimation()
        }
        
        binding.icon.setImageResource(R.drawable.ic_skill)
        binding.title.text = name
        
        binding.status.text = when (state) {
            AgentStatusManager.CallState.RUNNING -> "执行中..."
            AgentStatusManager.CallState.SUCCESS -> "成功"
            AgentStatusManager.CallState.FAILED -> "失败"
        }
        
        animateStatusChange()
    }
    
    private fun showWithAnimation() {
        isVisible = true
        floatingView.visibility = View.VISIBLE
        
        // Scale animation (like Dynamic Island)
        val scaleX = ObjectAnimator.ofFloat(floatingView, "scaleX", 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(floatingView, "scaleY", 0.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(floatingView, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 300
            interpolator = AnticipateOvershootInterpolator()
        }.start()
    }
    
    private fun hideFloatingWindow() {
        if (!isVisible) return
        
        val scaleX = ObjectAnimator.ofFloat(floatingView, "scaleX", 1f, 0.5f)
        val scaleY = ObjectAnimator.ofFloat(floatingView, "scaleY", 1f, 0.5f)
        val alpha = ObjectAnimator.ofFloat(floatingView, "alpha", 1f, 0f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 200
            interpolator = DecelerateInterpolator()
        }.apply {
            doOnEnd {
                floatingView.visibility = View.GONE
                isVisible = false
            }
        }.start()
    }
    
    private fun animateStatusChange() {
        // Pulse animation
        val scaleDown = ObjectAnimator.ofFloat(binding.cardView, "scaleX", 1f, 0.95f)
        val scaleUp = ObjectAnimator.ofFloat(binding.cardView, "scaleX", 0.95f, 1f)
        
        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            duration = 150
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        isRunning = false
    }
}
```

### 3.7 DrawerFragment.kt

```kotlin
package top.yudoge.phoneclaw.ui.chat.drawer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import top.yudoge.phoneclaw.databinding.FragmentDrawerBinding
import top.yudoge.phoneclaw.db.PhoneClawDbHelper
import java.text.SimpleDateFormat
import java.util.*

class DrawerFragment : Fragment() {
    
    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: PhoneClawDbHelper
    private lateinit var sessionAdapter: SessionGroupAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = PhoneClawDbHelper(requireContext())
        
        setupRecyclerView()
        setupNewSessionButton()
        setupSettingsButton()
        loadSessions()
    }
    
    private fun setupRecyclerView() {
        sessionAdapter = SessionGroupAdapter(
            onSessionClick = { session ->
                (activity as? ChatActivity)?.loadSession(session.id)
                closeDrawer()
            },
            onSessionLongClick = { session ->
                showSessionContextMenu(session)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
        }
    }
    
    private fun setupNewSessionButton() {
        binding.newSessionButton.setOnClickListener {
            (activity as? ChatActivity)?.createNewSession()
            closeDrawer()
        }
    }
    
    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener {
            (activity as? ChatActivity)?.navigateToSettings()
            closeDrawer()
        }
    }
    
    private fun loadSessions() {
        val sessions = dbHelper.allSessions
        val groupedSessions = groupSessionsByTime(sessions)
        sessionAdapter.submitList(groupedSessions)
    }
    
    private fun groupSessionsByTime(
        sessions: List<PhoneClawDbHelper.SessionRecord>
    ): List<SessionGroup> {
        val calendar = Calendar.getInstance()
        val oneWeekAgo = calendar.apply {
            add(Calendar.WEEK_OF_YEAR, -1)
        }.timeInMillis
        
        val recentSessions = sessions.filter { it.updatedAt >= oneWeekAgo }
        val olderSessions = sessions.filter { it.updatedAt < oneWeekAgo }
        
        val groups = mutableListOf<SessionGroup>()
        
        if (recentSessions.isNotEmpty()) {
            groups.add(SessionGroup("最近一周", recentSessions))
        }
        
        if (olderSessions.isNotEmpty()) {
            groups.add(SessionGroup("更早", olderSessions))
        }
        
        return groups
    }
    
    private fun closeDrawer() {
        (activity as? ChatActivity)?.closeDrawer()
    }
    
    private fun showSessionContextMenu(session: PhoneClawDbHelper.SessionRecord) {
        // Show dialog with delete/rename options
    }
    
    override fun onResume() {
        super.onResume()
        loadSessions()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class SessionGroup(
    val title: String,
    val sessions: List<PhoneClawDbHelper.SessionRecord>
)
```

## 4. 布局文件示例

### 4.1 activity_chat.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.drawerlayout.widget.DrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true"
    tools:context=".ui.chat.ChatActivity">
    
    <!-- Main Content -->
    <androidx.coordinatorlayout.widget.CoordinatorLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        
        <!-- Top App Bar -->
        <com.google.android.material.appbar.AppBarLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content">
            
            <com.google.android.material.appbar.MaterialToolbar
                android:id="@+id/toolbar"
                android:layout_width="match_parent"
                android:layout_height="?attr/actionBarSize"
                app:navigationIcon="@drawable/ic_menu"
                app:title="新对话"
                app:menu="@menu/menu_chat" />
                
        </com.google.android.material.appbar.AppBarLayout>
        
        <!-- Message List -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_above="@id/input_container"
            android:layout_below="@id/toolbar"
            android:clipToPadding="false"
            android:padding="8dp"
            app:layout_behavior="@string/appbar_scrolling_view_behavior" />
        
        <!-- Input Area -->
        <LinearLayout
            android:id="@+id/input_container"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom"
            android:orientation="vertical"
            android:background="?attr/colorSurface"
            android:elevation="4dp">
            
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="?attr/colorOutlineVariant" />
            
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:padding="8dp">
                
                <!-- Image Button -->
                <ImageButton
                    android:id="@+id/image_button"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:src="@drawable/ic_image"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:contentDescription="@string/select_image" />
                
                <!-- Input Mode Toggle -->
                <ImageButton
                    android:id="@+id/input_mode_button"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:src="@drawable/ic_keyboard"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:contentDescription="@string/toggle_input_mode" />
                
                <!-- Input Field -->
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/input_edit_text"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginHorizontal="8dp"
                    android:hint="@string/type_message"
                    android:maxLines="4"
                    android:inputType="textMultiLine" />
                
                <!-- Model Selector -->
                <TextView
                    android:id="@+id/model_selector"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="GPT-4"
                    android:drawableEnd="@drawable/ic_dropdown"
                    android:padding="8dp"
                    android:background="?attr/selectableItemBackground" />
                
                <!-- Send Button -->
                <ImageButton
                    android:id="@+id/send_button"
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:src="@drawable/ic_send"
                    android:background="@drawable/bg_send_button"
                    android:contentDescription="@string/send" />
                
                <!-- Stop Button (hidden by default) -->
                <ImageButton
                    android:id="@+id/stop_button"
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:src="@drawable/ic_stop"
                    android:background="@drawable/bg_stop_button"
                    android:visibility="gone"
                    android:contentDescription="@string/stop" />
                    
            </LinearLayout>
            
        </LinearLayout>
        
    </androidx.coordinatorlayout.widget.CoordinatorLayout>
    
    <!-- Drawer -->
    <FrameLayout
        android:id="@+id/drawer_fragment_container"
        android:layout_width="280dp"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        android:background="?attr/colorSurface" />
        
</androidx.drawerlayout.widget.DrawerLayout>
```

### 4.2 layout_floating_window.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/card_view"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="24dp"
    app:cardElevation="4dp"
    app:cardBackgroundColor="#E6000000">
    
    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:paddingVertical="12dp">
        
        <ImageView
            android:id="@+id/icon"
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@drawable/ic_thinking"
            android:tint="#FFFFFF" />
        
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:layout_marginStart="12dp">
            
            <TextView
                android:id="@+id/title"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="思考中"
                android:textColor="#FFFFFF"
                android:textSize="14sp"
                android:textStyle="bold"
                android:maxWidth="120dp"
                android:ellipsize="end"
                android:maxLines="1" />
            
            <TextView
                android:id="@+id/status"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="..."
                android:textColor="#B3FFFFFF"
                android:textSize="12sp"
                android:maxWidth="120dp"
                android:ellipsize="end"
                android:maxLines="1" />
                
        </LinearLayout>
        
    </LinearLayout>
    
</com.google.android.material.card.MaterialCardView>
```

## 5. AndroidManifest.xml 更新

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
    <!-- 现有权限 -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS"
        tools:ignore="ProtectedPermissions" />
    
    <application
        android:name=".PhoneClawApp"
        ...>
        
        <!-- 替换 MainActivity 为 ChatActivity -->
        <activity
            android:name=".ui.chat.ChatActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.PhoneClaw">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- Settings Activity -->
        <activity
            android:name=".ui.settings.SettingsActivity"
            android:exported="false"
            android:label="@string/settings"
            android:theme="@style/Theme.PhoneClaw" />
        
        <!-- Model Edit Activity -->
        <activity
            android:name=".ui.settings.model.ModelEditActivity"
            android:exported="false"
            android:label="@string/model_settings"
            android:theme="@style/Theme.PhoneClaw" />
        
        <!-- Floating Window Service -->
        <service
            android:name=".ui.floating.FloatingWindowService"
            android:enabled="true"
            android:exported="false" />
        
        <!-- 现有服务保持不变 -->
        <service android:name=".emu.EmuAccessibilityService" ... />
        
    </application>
    
</manifest>
```

## 6. 实施步骤

### Phase 1: 基础框架 (优先级: 高)

1. 创建目录结构
2. 实现 ChatActivity + DrawerLayout
3. 实现 MessageAdapter + 基础 ViewHolder
4. 实现用户消息和 Agent 消息展示
5. 实现基础输入功能

### Phase 2: Agent 集成 (优先级: 高)

1. 实现 ChatPresenter 连接 PhoneClawAgent
2. 实现 AgentStatusManager 全局状态
3. 实现工具调用/技能调用消息类型
4. 实现消息持久化和加载
5. 实现会话管理

### Phase 3: 设置页面 (优先级: 中)

1. 实现 SettingsActivity 框架
2. 实现模型提供商 CRUD
3. 实现技能管理
4. 整合权限控制
5. 整合脚本服务器

### Phase 4: 悬浮窗 (优先级: 中)

1. 实现 FloatingWindowService
2. 实现状态同步
3. 实现动画效果
4. 实现点击穿透配置

### Phase 5: 优化和完善 (优先级: 低)

1. 横屏/平板适配
2. 暗色主题
3. 性能优化
4. 边缘情况处理

## 7. 注意事项

1. **保持现有代码兼容**: 不修改 `llm/`、`emu/`、`scripts/` 等核心模块
2. **API 设计优化**: 如果发现现有 API 不合适，提出修改建议
3. **模块化原则**: 每个 UI 模块独立，职责清晰
4. **状态同步**: 持久层变化时触发 UI 刷新
5. **生命周期管理**: 正确处理 Activity/Fragment/Service 生命周期
6. **内存管理**: RecyclerView 复用、图片加载、大消息列表优化
