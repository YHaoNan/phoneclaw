package top.yudoge.phoneclaw.ui.settings.model

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlin.time.ExperimentalTime
import org.json.JSONObject
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.FragmentOpenaiConfigBinding
import top.yudoge.phoneclaw.llm.provider.openai.OpenAIModelConfig
import kotlinx.serialization.json.Json

class OpenAIConfigFragment : Fragment(), ProviderConfigFragment {

    private var _binding: FragmentOpenaiConfigBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var detectModelsCallback: ((List<String>) -> Unit)? = null
    private var detectModelsErrorCallback: ((String) -> Unit)? = null

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
        arguments?.getString("config")?.let { config ->
            if (config.isNotEmpty()) {
                loadConfig(config)
            } else {
                setDefaultValues()
            }
        } ?: setDefaultValues()
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

        return JSONObject().apply {
            put(OpenAIModelConfig.KEY_BASE_URL, baseUrl)
            put(OpenAIModelConfig.KEY_API_KEY, apiKey)
            put(OpenAIModelConfig.KEY_CHAT_COMPLETION_URL, binding.chatCompletionUrlInput.text?.toString()?.trim()?.ifEmpty { OpenAIModelConfig.DEFAULT_CHAT_COMPLETION_URL })
            put(OpenAIModelConfig.KEY_EMBEDDINGS_URL, binding.embeddingsUrlInput.text?.toString()?.trim()?.ifEmpty { OpenAIModelConfig.DEFAULT_EMBEDDINGS_URL })
            put(OpenAIModelConfig.KEY_MODERATIONS_URL, binding.moderationsUrlInput.text?.toString()?.trim())
            put(OpenAIModelConfig.KEY_MODELS_URL, binding.modelsUrlInput.text?.toString()?.trim()?.ifEmpty { OpenAIModelConfig.DEFAULT_MODELS_URL })
            put(OpenAIModelConfig.KEY_CONNECT_TIMEOUT, binding.connectTimeoutInput.text?.toString()?.toLongOrNull() ?: OpenAIModelConfig.DEFAULT_CONNECT_TIMEOUT)
            put(OpenAIModelConfig.KEY_REQUEST_TIMEOUT, binding.requestTimeoutInput.text?.toString()?.toLongOrNull() ?: OpenAIModelConfig.DEFAULT_REQUEST_TIMEOUT)
        }.toString()
    }

    override fun loadConfig(config: String) {
        try {
            val json = JSONObject(config)
            binding.baseUrlInput.setText(json.optString(OpenAIModelConfig.KEY_BASE_URL, OpenAIModelConfig.DEFAULT_BASE_URL))
            binding.apiKeyInput.setText(json.optString(OpenAIModelConfig.KEY_API_KEY, ""))
            binding.chatCompletionUrlInput.setText(json.optString(OpenAIModelConfig.KEY_CHAT_COMPLETION_URL, OpenAIModelConfig.DEFAULT_CHAT_COMPLETION_URL))
            binding.embeddingsUrlInput.setText(json.optString(OpenAIModelConfig.KEY_EMBEDDINGS_URL, OpenAIModelConfig.DEFAULT_EMBEDDINGS_URL))
            binding.moderationsUrlInput.setText(json.optString(OpenAIModelConfig.KEY_MODERATIONS_URL, ""))
            binding.modelsUrlInput.setText(json.optString(OpenAIModelConfig.KEY_MODELS_URL, OpenAIModelConfig.DEFAULT_MODELS_URL))
            binding.connectTimeoutInput.setText(json.optString(OpenAIModelConfig.KEY_CONNECT_TIMEOUT, OpenAIModelConfig.DEFAULT_CONNECT_TIMEOUT.toString()))
            binding.requestTimeoutInput.setText(json.optString(OpenAIModelConfig.KEY_REQUEST_TIMEOUT, OpenAIModelConfig.DEFAULT_REQUEST_TIMEOUT.toString()))
        } catch (e: Exception) {
            setDefaultValues()
        }
    }

    private fun setDefaultValues() {
        binding.baseUrlInput.setText(OpenAIModelConfig.DEFAULT_BASE_URL)
        binding.chatCompletionUrlInput.setText(OpenAIModelConfig.DEFAULT_CHAT_COMPLETION_URL)
        binding.embeddingsUrlInput.setText(OpenAIModelConfig.DEFAULT_EMBEDDINGS_URL)
        binding.modelsUrlInput.setText(OpenAIModelConfig.DEFAULT_MODELS_URL)
        binding.connectTimeoutInput.setText(OpenAIModelConfig.DEFAULT_CONNECT_TIMEOUT.toString())
        binding.requestTimeoutInput.setText(OpenAIModelConfig.DEFAULT_REQUEST_TIMEOUT.toString())
    }

    @OptIn(ExperimentalTime::class)
    override fun detectModels(callback: (List<String>) -> Unit, onError: (String) -> Unit) {
        detectModelsCallback = callback
        detectModelsErrorCallback = onError

        val baseUrl = binding.baseUrlInput.text?.toString()?.trim() ?: ""
        val apiKey = binding.apiKeyInput.text?.toString()?.trim() ?: ""
        val modelsPath = binding.modelsUrlInput.text?.toString()?.trim()?.ifEmpty { OpenAIModelConfig.DEFAULT_MODELS_URL } ?: OpenAIModelConfig.DEFAULT_MODELS_URL

        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            onError("请填写 Base URL 和 API Key")
            return
        }

        scope.launch {
            try {
                val models = withContext(Dispatchers.Default) {
                    detectModelsWithKoog(apiKey, baseUrl, modelsPath)
                }
                callback(models)
            } catch (e: Exception) {
                onError(e.message ?: "获取模型失败")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun detectModelsWithKoog(apiKey: String, baseUrl: String, modelsPath: String): List<String> {
        val settings = OpenAIClientSettings(
            baseUrl = baseUrl,
            modelsPath = modelsPath
        )
        
        val client = OpenAILLMClient(
            apiKey = apiKey,
            settings = settings
        )
        
        val models = client.models()
        return models.map { it.id }
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
