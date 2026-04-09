package top.yudoge.phoneclaw.ui.settings.skill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ActivitySkillListBinding
import top.yudoge.phoneclaw.databinding.ItemSkillBinding
import top.yudoge.phoneclaw.llm.skills.FileBasedSkillRepository
import top.yudoge.phoneclaw.llm.skills.Skill
import top.yudoge.phoneclaw.llm.skills.SkillRepository
import top.yudoge.phoneclaw.llm.skills.SkillSynchronizer
import java.io.File

class SkillListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillListBinding
    private lateinit var skillRepository: SkillRepository
    private lateinit var adapter: SkillAdapter
    private var skills = listOf<Skill>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySkillListBinding.inflate(layoutInflater)
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
        
        val skillsDir = File(filesDir, "skills")
        val syncResult = SkillSynchronizer.syncSkillsFromAssets(this, skillsDir)
        if (syncResult.hasChanges) {
            android.util.Log.d("SkillListActivity", "Skills synced: ${syncResult.logSummary()}")
        }
        skillRepository = FileBasedSkillRepository(skillsDir)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadSkills()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = SkillAdapter(
            skills = skills,
            onSkillClick = { skill ->
                openSkillEditor(skill)
            },
            onDeleteClick = { skill ->
                showDeleteDialog(skill)
            }
        )

        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.skillsRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.addSkillFab.setOnClickListener {
            openSkillEditor(null)
        }
    }

    private fun loadSkills() {
        skills = skillRepository.loadSkills()
        adapter.updateSkills(skills)

        if (skills.isEmpty()) {
            binding.skillsRecyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.skillsRecyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    private fun openSkillEditor(skill: Skill?) {
        val intent = Intent(this, SkillEditActivity::class.java).apply {
            if (skill != null) {
                putExtra("skillName", skill.name)
                putExtra("skillDescription", skill.description)
                putExtra("skillContent", skill.content)
                putExtra("isEdit", true)
            } else {
                putExtra("isEdit", false)
            }
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(skill: Skill) {
        AlertDialog.Builder(this)
            .setTitle("删除技能")
            .setMessage("确定要删除技能 \"${skill.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                skillRepository.deleteSkill(skill)
                loadSkills()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadSkills()
    }
}

class SkillAdapter(
    private var skills: List<Skill>,
    private val onSkillClick: (Skill) -> Unit,
    private val onDeleteClick: (Skill) -> Unit
) : RecyclerView.Adapter<SkillAdapter.SkillViewHolder>() {

    class SkillViewHolder(
        private val binding: ItemSkillBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(skill: Skill, onSkillClick: (Skill) -> Unit, onDeleteClick: (Skill) -> Unit) {
            binding.skillNameText.text = skill.name
            binding.skillDescriptionText.text = skill.description

            binding.root.setOnClickListener {
                onSkillClick(skill)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(skill)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val binding = ItemSkillBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SkillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        holder.bind(skills[position], onSkillClick, onDeleteClick)
    }

    override fun getItemCount(): Int = skills.size

    fun updateSkills(newSkills: List<Skill>) {
        skills = newSkills
        notifyDataSetChanged()
    }
}
