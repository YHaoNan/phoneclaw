package top.yudoge.phoneclaw.ui.settings.model

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.FragmentOpenaiConfigBinding
import top.yudoge.phoneclaw.llm.integration.openai.OpenAIModelConfig

class OpenAIConfigFragment : Fragment(), ProviderConfigFragment {

    private var _binding: FragmentOpenaiConfigBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val httpClient: OkHttpClient by lazy { OkHttpClient() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpenaiConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupResponseApiToggle()
        
        arguments?.getString("config")?.let { config ->
            if (config.isNotEmpty()) {
                loadConfig(config)
            } else {
                setDefaultValues()
            }
        } ?: setDefaultValues()
    }
    
    private fun setupResponseApiToggle() {
        binding.responseApiEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.responseUrlInputLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }

    override fun onNextStep(): Boolean {
        val baseUrl = binding.baseUrlInput.text?.toString()?.trim()
        val apiKey = binding.apiKeyInput.text?.toString()?.trim()

        if (baseUrl.isNullOrEmpty()) {
            binding.baseUrlInputLayout.error = "请输入 Base URL"
            return false
        }

        if (apiKey.isNullOrEmpty()) {
            binding.apiKeyInputLayout.error = "请输入 API Key"
            return false
        }

        binding.baseUrlInputLayout.error = null
        binding.apiKeyInputLayout.error = null
        return true
    }

    override fun getConfigJson(): String {
        val baseUrl = binding.baseUrlInput.text?.toString()?.trim() ?: ""
        val apiKey = binding.apiKeyInput.text?.toString()?.trim() ?: ""
        val responseApiEnabled = binding.responseApiEnabledSwitch.isChecked
        val responseUrl = binding.responseUrlInput.text?.toString()?.trim() ?: ""

        val config = OpenAIModelConfig(
            baseUrl = baseUrl,
            apiKey = apiKey,
            chatCompletionUrl = binding.chatCompletionUrlInput.text?.toString()?.trim()
                ?.ifEmpty { OpenAIModelConfig.DEFAULT_CHAT_COMPLETION_URL } ?: OpenAIModelConfig.DEFAULT_CHAT_COMPLETION_URL,
            modelsUrl = binding.modelsUrlInput.text?.toString()?.trim()
                ?.ifEmpty { OpenAIModelConfig.DEFAULT_MODELS_URL } ?: OpenAIModelConfig.DEFAULT_MODELS_URL,
            responseApiEnabled = responseApiEnabled,
            responseUrl = responseUrl.ifEmpty { OpenAIModelConfig.DEFAULT_RESPONSE_URL },
            connectTimeoutMillis = binding.connectTimeoutInput.text?.toString()?.toLongOrNull() 
                ?: OpenAIModelConfig.DEFAULT_CONNECT_TIMEOUT,
            requestTimeoutMillis = binding.requestTimeoutInput.text?.toString()?.toLongOrNull() 
                ?: OpenAIModelConfig.DEFAULT_REQUEST_TIMEOUT
        )
        
        return config.toJson()
    }

    override fun loadConfig(config: String) {
        try {
            val parsedConfig = OpenAIModelConfig.fromJson(config)
            
            binding.baseUrlInput.setText(parsedConfig.baseUrl)
            binding.apiKeyInput.setText(parsedConfig.apiKey ?: "")
            binding.chatCompletionUrlInput.setText(parsedConfig.chatCompletionUrl)
            binding.modelsUrlInput.setText(parsedConfig.modelsUrl)
            binding.responseApiEnabledSwitch.isChecked = parsedConfig.responseApiEnabled
            binding.responseUrlInput.setText(parsedConfig.responseUrl)
            binding.connectTimeoutInput.setText(parsedConfig.connectTimeoutMillis.toString())
            binding.requestTimeoutInput.setText(parsedConfig.requestTimeoutMillis.toString())
            
            binding.responseUrlInputLayout.visibility = if (parsedConfig.responseApiEnabled) View.VISIBLE else View.GONE
        } catch (_: Exception) {
            setDefaultValues()
        }
    }

    private fun setDefaultValues() {
        binding.baseUrlInput.setText(OpenAIModelConfig.DEFAULT_BASE_URL)
        binding.chatCompletionUrlInput.setText(OpenAIModelConfig.DEFAULT_CHAT_COMPLETION_URL)
        binding.modelsUrlInput.setText(OpenAIModelConfig.DEFAULT_MODELS_URL)
        binding.responseUrlInput.setText(OpenAIModelConfig.DEFAULT_RESPONSE_URL)
        binding.responseApiEnabledSwitch.isChecked = false
        binding.responseUrlInputLayout.visibility = View.GONE
        binding.connectTimeoutInput.setText(OpenAIModelConfig.DEFAULT_CONNECT_TIMEOUT.toString())
        binding.requestTimeoutInput.setText(OpenAIModelConfig.DEFAULT_REQUEST_TIMEOUT.toString())
    }

    override fun detectModels(callback: (List<String>) -> Unit, onError: (String) -> Unit) {
        val baseUrl = binding.baseUrlInput.text?.toString()?.trim() ?: ""
        val apiKey = binding.apiKeyInput.text?.toString()?.trim() ?: ""
        val modelsPath = binding.modelsUrlInput.text?.toString()?.trim()
            ?.ifEmpty { OpenAIModelConfig.DEFAULT_MODELS_URL }
            ?: OpenAIModelConfig.DEFAULT_MODELS_URL

        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            onError("请填写 Base URL 和 API Key")
            return
        }

        scope.launch {
            try {
                val models = withContext(Dispatchers.IO) {
                    detectModelsWithHttp(apiKey, baseUrl, modelsPath)
                }
                callback(models)
            } catch (e: Exception) {
                onError(e.message ?: "获取模型失败")
            }
        }
    }

    private fun detectModelsWithHttp(apiKey: String, baseUrl: String, modelsPath: String): List<String> {
        val modelsUrl = buildModelsUrl(baseUrl, modelsPath)
        val request = Request.Builder()
            .url(modelsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                throw IllegalStateException("请求失败(${response.code}): ${body.take(200)}")
            }

            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: return emptyList()
            val models = mutableListOf<String>()
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val id = item.optString("id", "").trim()
                if (id.isNotEmpty()) {
                    models += id
                }
            }
            return models
        }
    }

    private fun buildModelsUrl(baseUrl: String, modelsPath: String): String {
        val trimmedPath = modelsPath.trim()
        if (trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://")) {
            return trimmedPath
        }

        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        val normalizedPath = if (trimmedPath.startsWith('/')) trimmedPath else "/$trimmedPath"
        return "$normalizedBaseUrl$normalizedPath"
    }

    fun showDetectModelsDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.detect_models)
            .setMessage(R.string.detect_models_desc)
            .setPositiveButton("获取") { _, _ ->
                detectModels(
                    callback = { models ->
                        activity?.runOnUiThread {
                            (activity as? ProviderConfigActivity)?.onModelsDetected(models)
                        }
                    },
                    onError = { error ->
                        activity?.runOnUiThread {
                            Toast.makeText(context, getString(R.string.detect_failed, error), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("跳过") { _, _ ->
                (activity as? ProviderConfigActivity)?.onModelsDetected(emptyList())
            }
            .show()
    }
}
