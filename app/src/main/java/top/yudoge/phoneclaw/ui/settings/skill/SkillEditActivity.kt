package top.yudoge.phoneclaw.ui.settings.skill

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.databinding.ActivitySkillEditBinding
import top.yudoge.phoneclaw.llm.domain.SkillFacade
import top.yudoge.phoneclaw.llm.domain.objects.Skill
import top.yudoge.phoneclaw.llm.domain.objects.SkillSource

class SkillEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillEditBinding
    private var isEdit = false
    private var isBuiltIn = false
    private var originalSkillName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillEditBinding.inflate(layoutInflater)
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

        isEdit = intent.getBooleanExtra("isEdit", false)
        isBuiltIn = intent.getBooleanExtra("isBuiltIn", false)
        originalSkillName = if (isEdit) intent.getStringExtra("skillName") else null

        setupToolbar()
        loadSkillData()

        if (isBuiltIn) {
            setReadOnlyMode()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    if (isBuiltIn) {
                        Toast.makeText(this, "Built-in skills are read-only", Toast.LENGTH_SHORT).show()
                    } else {
                        saveSkill()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setReadOnlyMode() {
        binding.skillNameEdit.isEnabled = false
        binding.skillDescriptionEdit.isEnabled = false
        binding.skillContentEdit.isEnabled = false
        binding.readOnlyHint.visibility = View.VISIBLE

        binding.toolbar.menu.findItem(R.id.action_save)?.isVisible = false
    }

    private fun loadSkillData() {
        if (isEdit) {
            binding.skillNameEdit.setText(intent.getStringExtra("skillName"))
            binding.skillDescriptionEdit.setText(intent.getStringExtra("skillDescription"))
            binding.skillContentEdit.setText(intent.getStringExtra("skillContent"))
        }
    }

    private fun saveSkill() {
        val name = binding.skillNameEdit.text.toString().trim()
        val description = binding.skillDescriptionEdit.text.toString().trim()
        val content = binding.skillContentEdit.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please input skill name", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please input skill description", Toast.LENGTH_SHORT).show()
            return
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "Please input skill content", Toast.LENGTH_SHORT).show()
            return
        }

        val skill = Skill(
            name = name,
            description = description,
            source = SkillSource.USER
        )

        try {
            val facade = AppContainer.getInstance().skillFacade

            val success = if (isEdit) {
                saveEditedSkill(facade, skill, content)
            } else {
                val existing = facade.getSkillByName(name)
                if (existing != null) {
                    Toast.makeText(this, "Skill name already exists", Toast.LENGTH_SHORT).show()
                    return
                }
                facade.createUserSkill(skill, content)
            }

            if (success) {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEditedSkill(facade: SkillFacade, skill: Skill, content: String): Boolean {
        val originalName = originalSkillName ?: return false
        val original = facade.getSkillByName(originalName)
            ?.takeIf { it.source == SkillSource.USER }
            ?: return false

        if (originalName == skill.name) {
            return facade.updateUserSkill(skill.copy(skillDir = original.skillDir), content)
        }

        val conflict = facade.getSkillByName(skill.name)
        if (conflict != null) {
            Toast.makeText(this, "Skill name already exists", Toast.LENGTH_SHORT).show()
            return false
        }

        val created = facade.createUserSkill(skill, content)
        if (!created) {
            return false
        }

        val deleted = facade.deleteUserSkill(originalName)
        if (!deleted) {
            facade.deleteUserSkill(skill.name)
            return false
        }

        return true
    }
}
