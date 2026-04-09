package top.yudoge.phoneclaw.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ActivityToolCallDetailBinding
import java.util.regex.Pattern

class ToolCallDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolCallDetailBinding
    private var toolName: String = ""
    private var params: String = ""
    private var result: String? = null
    private var state: String = "SUCCESS"

    private var isParamsFormatted = false
    private var isResultFormatted = false
    private var originalParams: String = ""
    private var originalResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolCallDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
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

        toolName = intent.getStringExtra("toolName") ?: ""
        params = intent.getStringExtra("params") ?: ""
        result = intent.getStringExtra("result")
        state = intent.getStringExtra("state") ?: "SUCCESS"

        originalParams = params
        originalResult = result ?: ""

        setupToolbar()
        setupContent()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.title = toolName
    }

    private fun setupContent() {
        binding.toolNameText.text = toolName

        when (state) {
            "RUNNING" -> {
                binding.statusChip.text = "执行中"
                binding.statusChip.setChipBackgroundColorResource(R.color.primary)
            }
            "SUCCESS" -> {
                binding.statusChip.text = "成功"
                binding.statusChip.setChipBackgroundColorResource(R.color.success)
            }
            "FAILED" -> {
                binding.statusChip.text = "失败"
                binding.statusChip.setChipBackgroundColorResource(R.color.error)
            }
        }

        binding.paramsText.setText(params)

        if (result != null) {
            binding.resultCard.visibility = android.view.View.VISIBLE
            binding.resultText.setText(result)
        } else {
            binding.resultCard.visibility = android.view.View.GONE
        }
    }

    private fun setupListeners() {
        binding.copyParamsButton.setOnClickListener {
            copyToClipboard(binding.paramsText.text.toString(), "工具参数")
        }

        binding.formatParamsButton.setOnClickListener {
            if (isParamsFormatted) {
                binding.paramsText.setText(originalParams)
                isParamsFormatted = false
            } else {
                val formatted = formatJson(originalParams)
                binding.paramsText.setText(formatted)
                isParamsFormatted = true
            }
        }

        binding.copyResultButton.setOnClickListener {
            copyToClipboard(binding.resultText.text.toString(), "工具结果")
        }

        binding.formatResultButton.setOnClickListener {
            if (isResultFormatted) {
                binding.resultText.setText(originalResult)
                isResultFormatted = false
            } else {
                val formatted = formatJson(originalResult)
                binding.resultText.setText(formatted)
                isResultFormatted = true
            }
        }
    }

    private fun formatJson(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            json.toString(2)
        } catch (e: Exception) {
            jsonString
        }
    }

    private fun copyToClipboard(text: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.root, "已复制到剪贴板", Snackbar.LENGTH_SHORT).show()
    }

    private var searchKeyword: String = ""

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tool_detail, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? androidx.appcompat.widget.SearchView
        
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchKeyword = newText ?: ""
                highlightSearchResults()
                return true
            }
        })
        
        return true
    }

    private fun highlightSearchResults() {
        if (searchKeyword.isEmpty()) {
            binding.paramsText.setText(if (isParamsFormatted) formatJson(originalParams) else originalParams)
            if (result != null) {
                binding.resultText.setText(if (isResultFormatted) formatJson(originalResult) else originalResult)
            }
            return
        }

        highlightText(binding.paramsText, searchKeyword)
        
        if (result != null) {
            highlightText(binding.resultText, searchKeyword)
        }
    }

    private fun highlightText(editText: EditText, keyword: String) {
        val text = editText.text.toString()
        val spannableString = SpannableString(text)
        
        try {
            val pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            
            while (matcher.find()) {
                spannableString.setSpan(
                    BackgroundColorSpan(Color.YELLOW),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            editText.setText(spannableString)
        } catch (e: Exception) {
        }
    }
}
