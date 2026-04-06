package top.yudoge.phoneclaw.ui.settings.model

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ActivityModelEditBinding
import top.yudoge.phoneclaw.llm.provider.ModelEntity
import top.yudoge.phoneclaw.llm.provider.ModelRepositoryImpl

class ModelEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROVIDER_ID = "provider_id"
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_IS_NEW_PROVIDER = "is_new_provider"
        const val EXTRA_DETECTED_MODELS = "detected_models"
        private const val TAG = "ModelEdit"
    }

    private lateinit var binding: ActivityModelEditBinding
    private lateinit var modelAdapter: ModelAdapter
    private lateinit var modelRepository: ModelRepositoryImpl
    private var providerId: Long = 0
    private var isNewProvider: Boolean = false
    private val existingModels = mutableListOf<ModelEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityModelEditBinding.inflate(layoutInflater)
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

        modelRepository = ModelRepositoryImpl(this)

        providerId = intent.getLongExtra(EXTRA_PROVIDER_ID, 0)
        isNewProvider = intent.getBooleanExtra(EXTRA_IS_NEW_PROVIDER, false)
        
        Log.d(TAG, "onCreate: providerId=$providerId, isNewProvider=$isNewProvider")
        Log.d(TAG, "Intent extras: ${intent.extras}")
        
        if (providerId == 0L) {
            Log.e(TAG, "providerId is 0, finishing")
            finish()
            return
        }

        setupToolbar()
        setupExistingModelsList()
        setupButtons()
        loadExistingModels()
        handleDetectedModels()
    }

    override fun onResume() {
        super.onResume()
        loadExistingModels()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupExistingModelsList() {
        modelAdapter = ModelAdapter(
            onEditClick = { model ->
                editModel(model)
            },
            onDeleteClick = { model ->
                deleteModel(model)
            }
        )
        binding.existingModelsRecycler.layoutManager = LinearLayoutManager(this)
        binding.existingModelsRecycler.adapter = modelAdapter
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnDone.setOnClickListener {
            finish()
        }
        
        binding.btnSaveModel.setOnClickListener {
            saveModel()
        }

        if (isNewProvider) {
            binding.btnBack.visibility = View.VISIBLE
        } else {
            binding.btnBack.visibility = View.GONE
        }
    }

    private fun loadExistingModels() {
        Log.d(TAG, "loadExistingModels: providerId=$providerId")
        val models = modelRepository.getModelsByProvider(providerId)
        existingModels.clear()
        existingModels.addAll(models)
        updateExistingModelsList()
    }

    private fun updateExistingModelsList() {
        if (existingModels.isEmpty()) {
            binding.existingModelsCard.visibility = View.GONE
        } else {
            binding.existingModelsCard.visibility = View.VISIBLE
            modelAdapter.setData(existingModels.map { ModelAdapterItem.fromEntity(it) })
        }
    }

    private fun handleDetectedModels() {
        val detectedModels = intent.getStringArrayListExtra(EXTRA_DETECTED_MODELS)
        if (!detectedModels.isNullOrEmpty()) {
            binding.modelIdInput.setText(detectedModels[0])
            binding.displayNameInput.setText(detectedModels[0])
            binding.modelIdInput.setSelection(detectedModels[0].length)
        }
    }

    private fun saveModel() {
        val modelIdInput = binding.modelIdInput.text?.toString()?.trim()
        val displayName = binding.displayNameInput.text?.toString()?.trim()
        val hasVisual = binding.visualSwitch.isChecked
        
        Log.d(TAG, "saveModel: id=$modelIdInput, providerId=$providerId, displayName=$displayName")
        
        if (modelIdInput.isNullOrEmpty()) {
            binding.modelIdInputLayout.error = "请输入模型 ID"
            return
        }
        
        if (displayName.isNullOrEmpty()) {
            binding.displayNameInputLayout.error = "请输入显示名称"
            return
        }
        
        binding.modelIdInputLayout.error = null
        binding.displayNameInputLayout.error = null
        
        val model = ModelEntity(
            id = modelIdInput,
            providerId = providerId,
            displayName = displayName,
            hasVisualCapability = hasVisual
        )
        
        modelRepository.addModel(model)
        
        loadExistingModels()
        
        Toast.makeText(this, "模型已保存", Toast.LENGTH_SHORT).show()
        
        binding.modelIdInput.text?.clear()
        binding.displayNameInput.text?.clear()
        binding.visualSwitch.isChecked = false
    }

    private fun editModel(model: ModelAdapterItem) {
    }

    private fun deleteModel(model: ModelAdapterItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除模型")
            .setMessage("确定要删除 ${model.displayName} 吗？")
            .setPositiveButton("删除") { _, _ ->
                modelRepository.deleteModel(model.id)
                loadExistingModels()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

data class ModelAdapterItem(
    val id: String,
    val providerId: Long,
    val displayName: String,
    val hasVisualCapability: Boolean
) {
    companion object {
        fun fromEntity(entity: ModelEntity): ModelAdapterItem {
            return ModelAdapterItem(
                id = entity.id,
                providerId = entity.providerId,
                displayName = entity.displayName,
                hasVisualCapability = entity.hasVisualCapability
            )
        }
    }
}
