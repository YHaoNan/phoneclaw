package top.yudoge.phoneclaw.ui.settings.model

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ActivityProviderListBinding
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType
import top.yudoge.phoneclaw.llm.integration.openai.OpenAIModelConfig

class ProviderListActivity : AppCompatActivity(), ProviderListContract.View {

    private lateinit var binding: ActivityProviderListBinding
    private lateinit var presenter: ProviderListPresenter
    private lateinit var adapter: ProviderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityProviderListBinding.inflate(layoutInflater)
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
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupFab()
        
        presenter = ProviderListPresenter(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.loadProviders()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProviderAdapter(
            onProviderClick = { provider ->
                adapter.toggleExpanded(provider.id)
            },
            onProviderLongClick = { provider ->
                showProviderOptionsDialog(provider)
            },
            onAddModelClick = { provider ->
                openAddModel(provider)
            },
            onEditModelClick = { model ->
                openEditModel(model)
            },
            onDeleteModelClick = { model ->
                presenter.deleteModel(model.id)
            },
            onDeleteProviderClick = { provider ->
                presenter.deleteProvider(provider.id)
            }
        )
        binding.providersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.providersRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddProvider.setOnClickListener {
            openAddProvider()
        }
        binding.fabAutoAddProvider.setOnClickListener {
            showAutoDetectDialog()
        }
    }

    private fun showAutoDetectDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val input = EditText(this).apply {
            hint = "粘贴配置文本（URL/APIKey/Model等）"
            minLines = 6
            maxLines = 12
        }
        val preview = EditText(this).apply {
            hint = "识别预览"
            minLines = 6
            maxLines = 12
            isFocusable = false
            isClickable = false
        }
        container.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        container.addView(
            preview,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        )

        fun refreshPreview() {
            val result = AutoProviderImportParser.parse(input.text?.toString().orEmpty())
            preview.setText(result.toPreviewText())
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                refreshPreview()
            }
        })
        refreshPreview()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("自动识别创建（OpenAI兼容）")
            .setView(container)
            .setPositiveButton("创建") { _, _ ->
                createProviderByAutoDetection(input.text?.toString().orEmpty())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createProviderByAutoDetection(raw: String) {
        val result = AutoProviderImportParser.parse(raw)

        val baseUrl = result.baseUrl
        val apiKey = result.apiKey
        if (baseUrl.isNullOrBlank() || apiKey.isNullOrBlank()) {
            Toast.makeText(this, "识别失败：至少需要 baseUrl 和 apiKey", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val providerId = AppContainer.getInstance().modelProviderFacade.addProvider(
                result.providerName,
                ProviderType.OpenAICompatible,
                ""
            )
            val configJson = OpenAIModelConfig(
                baseUrl = baseUrl,
                apiKey = apiKey
            ).toJson()
            val provider = AppContainer.getInstance().modelProviderFactory.create(
                providerId,
                result.providerName,
                ProviderType.OpenAICompatible,
                configJson
            )
            AppContainer.getInstance().modelProviderFacade.updateProvider(provider)

            var addedModels = 0
            result.models.forEach { modelId ->
                AppContainer.getInstance().modelProviderFacade.addModel(
                    Model(
                        id = modelId,
                        providerId = providerId,
                        displayName = modelId,
                        hasVisualCapability = false
                    )
                )
                addedModels++
            }

            presenter.loadProviders()
            Toast.makeText(
                this,
                "创建成功：${result.providerName}，模型 ${addedModels} 个",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "自动创建失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showProviderOptionsDialog(provider: ModelProvider) {
        val options = arrayOf("编辑", "删除")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(provider.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openEditProvider(provider)
                    1 -> confirmDeleteProvider(provider)
                }
            }
            .show()
    }

    private fun confirmDeleteProvider(provider: ModelProvider) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除提供商")
            .setMessage("确定要删除 ${provider.name} 及其所有模型吗？")
            .setPositiveButton("删除") { _, _ ->
                presenter.deleteProvider(provider.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openAddProvider() {
        startActivity(Intent(this, ProviderEditActivity::class.java))
    }

    private fun openEditProvider(provider: ModelProvider) {
        val intent = Intent(this, ProviderEditActivity::class.java)
        intent.putExtra(ProviderEditActivity.EXTRA_PROVIDER_ID, provider.id)
        startActivity(intent)
    }

    private fun openAddModel(provider: ModelProvider) {
        android.util.Log.d("ProviderList", "openAddModel: provider.id=${provider.id}, name=${provider.name}")
        val intent = Intent(this, ModelEditActivity::class.java)
        intent.putExtra(ModelEditActivity.EXTRA_PROVIDER_ID, provider.id)
        android.util.Log.d("ProviderList", "Intent extras: ${intent.getLongExtra(ModelEditActivity.EXTRA_PROVIDER_ID, -999)}")
        startActivity(intent)
    }

    private fun openEditModel(model: ModelAdapterItem) {
        val intent = Intent(this, ModelEditActivity::class.java)
        intent.putExtra(ModelEditActivity.EXTRA_MODEL_ID, model.id)
        intent.putExtra(ModelEditActivity.EXTRA_PROVIDER_ID, model.providerId)
        startActivity(intent)
    }

    override fun showProviders(providers: List<ModelProvider>, providerModels: Map<Long, List<ModelAdapterItem>>) {
        adapter.setData(providers, providerModels)
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onProviderDeleted() {
        presenter.loadProviders()
    }

    override fun onModelDeleted() {
        presenter.loadProviders()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
