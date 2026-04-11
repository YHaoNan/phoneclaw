package top.yudoge.phoneclaw.ui.settings.model

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.databinding.ActivityProviderEditBinding
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType

class ProviderEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROVIDER_ID = "provider_id"
        private const val TAG = "ProviderEdit"
    }

    private lateinit var binding: ActivityProviderEditBinding
    private var providerId: Long = 0
    private var selectedProviderType: ProviderType = ProviderType.OpenAICompatible

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityProviderEditBinding.inflate(layoutInflater)
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

        providerId = intent.getLongExtra(EXTRA_PROVIDER_ID, 0)

        setupToolbar()
        setupProviderTypeSelection()
        setupButtons()
        
        if (providerId > 0) {
            loadProvider()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupProviderTypeSelection() {
        binding.radioOpenaiCompatible.isChecked = true
        
        binding.apiTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_openai_compatible -> {
                    selectedProviderType = ProviderType.OpenAICompatible
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val name = binding.nameInput.text?.toString()?.trim()
            
            if (name.isNullOrEmpty()) {
                binding.nameInputLayout.error = "请输入提供商名称"
                return@setOnClickListener
            }
            
            binding.nameInputLayout.error = null
            saveProviderAndContinue(name)
        }
    }

    private fun loadProvider() {
        val provider = AppContainer.getInstance().modelProviderFacade.getProviderById(providerId)
        
        if (provider != null) {
            binding.toolbar.title = getString(R.string.edit_provider)
            binding.nameInput.setText(provider.name)
            selectedProviderType = provider.providerType
            
            when (provider.providerType) {
                ProviderType.OpenAICompatible -> binding.radioOpenaiCompatible.isChecked = true
            }
        }
    }

    private fun saveProviderAndContinue(name: String) {
        if (providerId > 0) {
            val provider = AppContainer.getInstance().modelProviderFacade.getProviderById(providerId)
            if (provider != null) {
                AppContainer.getInstance().modelProviderFacade.updateProvider(
                    AppContainer.getInstance().modelProviderFactory.create(
                        providerId, name, selectedProviderType, provider.parseToConfig()
                    )
                )
            }
            Log.d(TAG, "Updated provider, id=$providerId")
            
            val intent = Intent(this, ProviderConfigActivity::class.java)
            intent.putExtra(ProviderConfigActivity.EXTRA_PROVIDER_ID, providerId)
            startActivity(intent)
        } else {
            val newId = AppContainer.getInstance().modelProviderFacade.addProvider(
                name, selectedProviderType, ""
            )
            Log.d(TAG, "Created new provider, id=$newId")
            
            val intent = Intent(this, ProviderConfigActivity::class.java)
            intent.putExtra(ProviderConfigActivity.EXTRA_PROVIDER_ID, newId)
            startActivity(intent)
        }
        finish()
    }
}
