package top.yudoge.phoneclaw.ui.settings.taskscript

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.databinding.ActivityTaskScriptEditBinding

class TaskScriptEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskScriptEditBinding
    private var isEdit = false
    private var scriptId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskScriptEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isEdit = intent.getBooleanExtra("isEdit", false)
        scriptId = intent.getStringExtra("scriptId")

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_save) {
                save()
                true
            } else {
                false
            }
        }

        if (isEdit) {
            loadScript()
        } else {
            binding.readOnlyHint.visibility = View.GONE
        }
    }

    private fun loadScript() {
        val id = scriptId ?: return
        val script = AppContainer.getInstance().taskScriptFacade.getScriptById(id) ?: return
        binding.scriptNameEdit.setText(script.name)
        binding.scriptSummaryEdit.setText(script.summary)
        binding.scriptContentEdit.setText(script.codeContent)
    }

    private fun save() {
        val name = binding.scriptNameEdit.text.toString().trim()
        val summary = binding.scriptSummaryEdit.text.toString().trim()
        val code = binding.scriptContentEdit.text.toString()
        val facade = AppContainer.getInstance().taskScriptFacade

        try {
            val success = if (isEdit) {
                val id = scriptId ?: return
                facade.updateScript(id, name, summary, code)
            } else {
                facade.createScript(name, summary, code)
            }
            if (!success) {
                Toast.makeText(this, "Save failed (name may conflict).", Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, e.message ?: "Validation failed", Toast.LENGTH_SHORT).show()
        }
    }
}
