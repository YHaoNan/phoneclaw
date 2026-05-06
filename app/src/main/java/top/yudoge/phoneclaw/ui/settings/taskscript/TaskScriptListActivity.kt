package top.yudoge.phoneclaw.ui.settings.taskscript

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
