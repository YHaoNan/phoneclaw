package top.yudoge.phoneclaw.ui.settings.model

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import top.yudoge.phoneclaw.R
import android.util.Log
import top.yudoge.phoneclaw.databinding.ActivityProviderConfigBinding
import top.yudoge.phoneclaw.llm.provider.ModelProviderRepositoryImpl
import top.yudoge.phoneclaw.llm.provider.APIType

class ProviderConfigActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROVIDER_ID = "provider_id"
    }

    private lateinit var binding: ActivityProviderConfigBinding
    private lateinit var providerRepo: ModelProviderRepositoryImpl
    private var providerId: Long = 0
    private var providerApiType: String = ""
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityProviderConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        providerRepo = ModelProviderRepositoryImpl(this)
        
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

        providerId = intent.getLongExtra(EXTRA_PROVIDER_ID, 0)
        Log.d("ProviderConfig", "onCreate: received providerId=$providerId")
        if (providerId == 0L) {
            Log.e("ProviderConfig", "providerId is 0, finishing")
            finish()
            return
        }

        setupToolbar()
        loadProvider()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadProvider() {
        val provider = providerRepo.getProvider(providerId)
        Log.d("ProviderConfig", "loadProvider: looking for id=$providerId, found=${provider != null}")
        
        if (provider != null) {
            Log.d("ProviderConfig", "Found provider: id=${provider.id}, name=${provider.name}")
            binding.providerNameLabel.text = provider.name
            providerApiType = provider.apiType.name
            loadFragment(provider.apiType.name, provider.modelProviderConfig)
        } else {
            Log.e("ProviderConfig", "Provider not found for id=$providerId")
        }
    }

    private fun loadFragment(apiType: String, config: String?) {
        currentFragment = when (apiType) {
            APIType.OpenAICompatible.name -> OpenAIConfigFragment().apply {
                arguments = Bundle().apply {
                    putString("config", config)
                }
            }
            else -> OpenAIConfigFragment().apply {
                arguments = Bundle().apply {
                    putString("config", config)
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, currentFragment!!)
            .commit()
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnNext.setOnClickListener {
            val fragment = currentFragment as? ProviderConfigFragment ?: return@setOnClickListener
            
            if (fragment.onNextStep()) {
                saveConfig(fragment.getConfigJson())
                
                if (providerApiType == APIType.OpenAICompatible.name) {
                    (fragment as? OpenAIConfigFragment)?.showDetectModelsDialog()
                } else {
                    onModelsDetected(emptyList())
                }
            }
        }
    }

    private fun saveConfig(configJson: String) {
        val provider = providerRepo.getProvider(providerId)
        provider?.let {
            val updated = it.copy(modelProviderConfig = configJson)
            providerRepo.updateProvider(updated)
        }
    }

    fun onModelsDetected(models: List<String>) {
        Log.d("ProviderConfig", "onModelsDetected: providerId=$providerId, models=${models.size}")
        val intent = Intent(this, ModelEditActivity::class.java)
        intent.putExtra(ModelEditActivity.EXTRA_PROVIDER_ID, providerId)
        intent.putExtra(ModelEditActivity.EXTRA_IS_NEW_PROVIDER, true)
        intent.putStringArrayListExtra(ModelEditActivity.EXTRA_DETECTED_MODELS, ArrayList(models))
        Log.d("ProviderConfig", "Starting ModelEditActivity with providerId=$providerId")
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from model edit
        loadProvider()
    }
}
