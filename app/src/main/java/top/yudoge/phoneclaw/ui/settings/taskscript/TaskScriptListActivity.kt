package top.yudoge.phoneclaw.ui.settings.taskscript

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.databinding.ActivityTaskScriptListBinding
import top.yudoge.phoneclaw.llm.domain.objects.TaskScript

class TaskScriptListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskScriptListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskScriptListBinding.inflate(layoutInflater)
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

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.taskScriptsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.addTaskScriptFab.setOnClickListener { openEditor(null) }
        loadScripts()
    }

    override fun onResume() {
        super.onResume()
        loadScripts()
    }

    private fun loadScripts() {
        val scripts = AppContainer.getInstance().taskScriptFacade.getAllScripts()
        binding.taskScriptsRecyclerView.adapter = TaskScriptAdapter(
            scripts = scripts,
            onClick = { openEditor(it) },
            onDelete = { showDeleteDialog(it) }
        )
        binding.emptyView.visibility = if (scripts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(script: TaskScript?) {
        startActivity(Intent(this, TaskScriptEditActivity::class.java).apply {
            putExtra("scriptId", script?.id)
            putExtra("isEdit", script != null)
        })
    }

    private fun showDeleteDialog(script: TaskScript) {
        AlertDialog.Builder(this)
            .setTitle("Delete script")
            .setMessage("Delete script '${script.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                AppContainer.getInstance().taskScriptFacade.deleteScript(script.id)
                loadScripts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
