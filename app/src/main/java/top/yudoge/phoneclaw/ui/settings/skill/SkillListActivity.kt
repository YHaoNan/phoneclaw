package top.yudoge.phoneclaw.ui.settings.skill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
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
import top.yudoge.phoneclaw.databinding.ItemSkillSectionBinding
import top.yudoge.phoneclaw.llm.skills.AssetSkillRepository
import top.yudoge.phoneclaw.llm.skills.CompositeSkillRepository
import top.yudoge.phoneclaw.llm.skills.FileBasedSkillRepository
import top.yudoge.phoneclaw.llm.skills.Skill
import java.io.File

class SkillListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillListBinding
    private lateinit var compositeRepository: CompositeSkillRepository
    private var builtInSkills = listOf<Skill>()
    private var userSkills = listOf<Skill>()

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
        
        val builtInRepo = AssetSkillRepository(this)
        val userSkillsDir = File(filesDir, "user_skills").apply { mkdirs() }
        val userRepo = FileBasedSkillRepository(userSkillsDir)
        compositeRepository = CompositeSkillRepository(builtInRepo, userRepo)

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
        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFab() {
        binding.addSkillFab.setOnClickListener {
            openSkillEditor(null)
        }
    }

    private fun loadSkills() {
        builtInSkills = compositeRepository.getBuiltInSkills()
        userSkills = compositeRepository.getUserSkills()
        
        val adapter = SkillSectionAdapter(
            builtInSkills = builtInSkills,
            userSkills = userSkills,
            onSkillClick = { skill -> openSkillEditor(skill) },
            onDeleteClick = { skill -> showDeleteDialog(skill) }
        )
        
        binding.skillsRecyclerView.adapter = adapter

        if (builtInSkills.isEmpty() && userSkills.isEmpty()) {
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
                putExtra("isBuiltIn", skill.isBuiltIn)
            } else {
                putExtra("isEdit", false)
                putExtra("isBuiltIn", false)
            }
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(skill: Skill) {
        if (skill.isBuiltIn) {
            AlertDialog.Builder(this)
                .setTitle("无法删除")
                .setMessage("内置技能 \"${skill.name}\" 无法删除")
                .setPositiveButton("确定", null)
                .show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("删除技能")
            .setMessage("确定要删除技能 \"${skill.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                compositeRepository.deleteSkill(skill)
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

class SkillSectionAdapter(
    private val builtInSkills: List<Skill>,
    private val userSkills: List<Skill>,
    private val onSkillClick: (Skill) -> Unit,
    private val onDeleteClick: (Skill) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_SKILL = 1
    }

    override fun getItemViewType(position: Int): Int {
        var currentPos = 0
        
        if (builtInSkills.isNotEmpty()) {
            if (position == currentPos) return TYPE_SECTION_HEADER
            currentPos++
            if (position < currentPos + builtInSkills.size) return TYPE_SKILL
            currentPos += builtInSkills.size
        }
        
        if (userSkills.isNotEmpty()) {
            if (position == currentPos) return TYPE_SECTION_HEADER
            currentPos++
            if (position < currentPos + userSkills.size) return TYPE_SKILL
        }
        
        return TYPE_SKILL
    }

    override fun getItemCount(): Int {
        var count = 0
        if (builtInSkills.isNotEmpty()) count += 1 + builtInSkills.size
        if (userSkills.isNotEmpty()) count += 1 + userSkills.size
        return count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SECTION_HEADER -> {
                val binding = ItemSkillSectionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SectionHeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemSkillBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SkillViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionHeaderViewHolder -> {
                val isBuiltInSection = position == 0 && builtInSkills.isNotEmpty()
                val title = if (isBuiltInSection) "内置技能" else "用户技能"
                val subtitle = if (isBuiltInSection) "来自应用内置，不可修改" else "用户自定义，可编辑删除"
                holder.bind(title, subtitle)
            }
            is SkillViewHolder -> {
                val skill = getSkillAtPosition(position)
                if (skill != null) {
                    holder.bind(skill, onSkillClick, onDeleteClick)
                }
            }
        }
    }

    private fun getSkillAtPosition(position: Int): Skill? {
        var currentPos = 0
        
        if (builtInSkills.isNotEmpty()) {
            if (position == currentPos) return null
            currentPos++
            if (position < currentPos + builtInSkills.size) {
                return builtInSkills[position - currentPos]
            }
            currentPos += builtInSkills.size
        }
        
        if (userSkills.isNotEmpty()) {
            if (position == currentPos) return null
            currentPos++
            if (position < currentPos + userSkills.size) {
                return userSkills[position - currentPos]
            }
        }
        
        return null
    }

    class SectionHeaderViewHolder(
        private val binding: ItemSkillSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String, subtitle: String) {
            binding.sectionTitle.text = title
            binding.sectionSubtitle.text = subtitle
        }
    }

    class SkillViewHolder(
        private val binding: ItemSkillBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(skill: Skill, onSkillClick: (Skill) -> Unit, onDeleteClick: (Skill) -> Unit) {
            binding.skillNameText.text = skill.name
            binding.skillDescriptionText.text = skill.description

            binding.root.setOnClickListener {
                onSkillClick(skill)
            }

            if (skill.isBuiltIn) {
                binding.deleteButton.visibility = View.GONE
                binding.builtInBadge.visibility = View.VISIBLE
            } else {
                binding.deleteButton.visibility = View.VISIBLE
                binding.builtInBadge.visibility = View.GONE
                binding.deleteButton.setOnClickListener {
                    onDeleteClick(skill)
                }
            }
        }
    }
}
