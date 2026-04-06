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
            put("base_url", baseUrl)
            put("api_key", apiKey)
            put("chat_completion_url", binding.chatCompletionUrlInput.text?.toString()?.trim()?.ifEmpty { "/v1/chat/completions" })
            put("embeddings_url", binding.embeddingsUrlInput.text?.toString()?.trim()?.ifEmpty { "/v1/embeddings" })
            put("moderations_url", binding.moderationsUrlInput.text?.toString()?.trim())
            put("models_url", binding.modelsUrlInput.text?.toString()?.trim()?.ifEmpty { "/v1/models" })
            put("connect_timeout_millis", binding.connectTimeoutInput.text?.toString()?.toLongOrNull() ?: 5000)
            put("request_timeout_millis", binding.requestTimeoutInput.text?.toString()?.toLongOrNull() ?: 60000)
        }.toString()
    }

    override fun loadConfig(config: String) {
        try {
            val json = JSONObject(config)
            binding.baseUrlInput.setText(json.optString("base_url", "https://api.openai.com"))
            binding.apiKeyInput.setText(json.optString("api_key", ""))
            binding.chatCompletionUrlInput.setText(json.optString("chat_completion_url", "/v1/chat/completions"))
            binding.embeddingsUrlInput.setText(json.optString("embeddings_url", "/v1/embeddings"))
            binding.moderationsUrlInput.setText(json.optString("moderations_url", ""))
            binding.modelsUrlInput.setText(json.optString("models_url", "/v1/models"))
            binding.connectTimeoutInput.setText(json.optString("connect_timeout_millis", "5000"))
            binding.requestTimeoutInput.setText(json.optString("request_timeout_millis", "60000"))
        } catch (e: Exception) {
            setDefaultValues()
        }
    }

    private fun setDefaultValues() {
        binding.baseUrlInput.setText("https://api.openai.com")
        binding.chatCompletionUrlInput.setText("/v1/chat/completions")
        binding.embeddingsUrlInput.setText("/v1/embeddings")
        binding.modelsUrlInput.setText("/v1/models")
        binding.connectTimeoutInput.setText("5000")
        binding.requestTimeoutInput.setText("60000")
    }

    @OptIn(ExperimentalTime::class)
    override fun detectModels(callback: (List<String>) -> Unit, onError: (String) -> Unit) {
        detectModelsCallback = callback
        detectModelsErrorCallback = onError

        val baseUrl = binding.baseUrlInput.text?.toString()?.trim() ?: ""
        val apiKey = binding.apiKeyInput.text?.toString()?.trim() ?: ""
        val modelsPath = binding.modelsUrlInput.text?.toString()?.trim()?.ifEmpty { "/v1/models" } ?: "/v1/models"

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
