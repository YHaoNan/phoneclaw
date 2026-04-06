package top.yudoge.phoneclaw.ui.settings.model

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ActivityProviderListBinding
import top.yudoge.phoneclaw.db.PhoneClawDbHelper
import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelProviderRecord
import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelRecord

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
        
        val dbHelper = PhoneClawDbHelper(this)
        presenter = ProviderListPresenter(this, dbHelper)
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
    }

    private fun showProviderOptionsDialog(provider: ModelProviderRecord) {
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

    private fun confirmDeleteProvider(provider: ModelProviderRecord) {
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

    private fun openEditProvider(provider: ModelProviderRecord) {
        val intent = Intent(this, ProviderEditActivity::class.java)
        intent.putExtra(ProviderEditActivity.EXTRA_PROVIDER_ID, provider.id)
        startActivity(intent)
    }

    private fun openAddModel(provider: ModelProviderRecord) {
        android.util.Log.d("ProviderList", "openAddModel: provider.id=${provider.id}, name=${provider.name}")
        val intent = Intent(this, ModelEditActivity::class.java)
        intent.putExtra(ModelEditActivity.EXTRA_PROVIDER_ID, provider.id)
        android.util.Log.d("ProviderList", "Intent extras: ${intent.getLongExtra(ModelEditActivity.EXTRA_PROVIDER_ID, -999)}")
        startActivity(intent)
    }

    private fun openEditModel(model: ModelRecord) {
        val intent = Intent(this, ModelEditActivity::class.java)
        intent.putExtra(ModelEditActivity.EXTRA_MODEL_ID, model.id)
        intent.putExtra(ModelEditActivity.EXTRA_PROVIDER_ID, model.providerId)
        startActivity(intent)
    }

    override fun showProviders(providers: List<ModelProviderRecord>, providerModels: Map<Long, List<ModelRecord>>) {
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
