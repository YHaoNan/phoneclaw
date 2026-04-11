package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.SkillEntity
import top.yudoge.phoneclaw.llm.domain.objects.Skill
import top.yudoge.phoneclaw.llm.domain.objects.SkillSource
import top.yudoge.phoneclaw.llm.domain.objects.SkillWithContent
import java.io.File

class UserSkillRepository(
    private val context: Context,
    private val dbHelper: PhoneClawDatabaseHelper
) {
    private val skillsDir: File by lazy {
        File(context.filesDir, "skills").also { it.mkdirs() }
    }
    
    fun getAll(): List<Skill> {
        val skills = mutableListOf<Skill>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SKILL_INDEX,
            null, null, null, null, null, "updated_at DESC"
        )
        
        while (cursor.moveToNext()) {
            val entity = SkillEntity(
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                argumentHint = cursor.getString(cursor.getColumnIndexOrThrow("argument_hint")),
                disableModelInvocation = cursor.getInt(cursor.getColumnIndexOrThrow("disable_model_invocation")) == 1,
                userInvocable = cursor.getInt(cursor.getColumnIndexOrThrow("user_invocable")) == 1,
                allowedTools = cursor.getString(cursor.getColumnIndexOrThrow("allowed_tools")),
                context = cursor.getString(cursor.getColumnIndexOrThrow("context")),
                skillDir = cursor.getString(cursor.getColumnIndexOrThrow("skill_dir")),
                supportingFiles = cursor.getString(cursor.getColumnIndexOrThrow("supporting_files")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            )
            skills.add(entity.toDomain())
        }
        cursor.close()
        return skills
    }
    
    fun getByName(name: String): Skill? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SKILL_INDEX,
            null, "name = ?", arrayOf(name), null, null, null
        )
        
        var skill: Skill? = null
        if (cursor.moveToFirst()) {
            val entity = SkillEntity(
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                argumentHint = cursor.getString(cursor.getColumnIndexOrThrow("argument_hint")),
                disableModelInvocation = cursor.getInt(cursor.getColumnIndexOrThrow("disable_model_invocation")) == 1,
                userInvocable = cursor.getInt(cursor.getColumnIndexOrThrow("user_invocable")) == 1,
                allowedTools = cursor.getString(cursor.getColumnIndexOrThrow("allowed_tools")),
                context = cursor.getString(cursor.getColumnIndexOrThrow("context")),
                skillDir = cursor.getString(cursor.getColumnIndexOrThrow("skill_dir")),
                supportingFiles = cursor.getString(cursor.getColumnIndexOrThrow("supporting_files")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            )
            skill = entity.toDomain()
        }
        cursor.close()
        return skill
    }
    
    fun getContent(skill: Skill): SkillWithContent? {
        val skillDir = skill.skillDir ?: return null
        val contentFile = File(File(skillsDir, skillDir), "skill.md")
        if (!contentFile.exists()) return null
        
        return try {
            val content = contentFile.readText()
            SkillWithContent(skill, content)
        } catch (e: Exception) {
            null
        }
    }
    
    fun insert(skill: Skill, content: String): Boolean {
        val now = System.currentTimeMillis()
        val skillDirName = skill.name.replace(" ", "_")
        val skillDir = File(skillsDir, skillDirName)
        skillDir.mkdirs()
        
        val contentFile = File(skillDir, "skill.md")
        contentFile.writeText(content)
        
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", skill.name)
            put("description", skill.description)
            put("argument_hint", skill.argumentHint)
            put("disable_model_invocation", if (skill.disableModelInvocation) 1 else 0)
            put("user_invocable", if (skill.userInvocable) 1 else 0)
            put("allowed_tools", skill.allowedTools?.joinToString(","))
            put("context", skill.context)
            put("skill_dir", skillDirName)
            put("supporting_files", skill.supportingFiles.joinToString(","))
            put("created_at", now)
            put("updated_at", now)
        }
        
        return db.insertWithOnConflict(
            PhoneClawDatabaseHelper.TABLE_SKILL_INDEX,
            null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }
    
    fun update(skill: Skill, content: String?): Boolean {
        val now = System.currentTimeMillis()
        
        content?.let {
            val skillDir = skill.skillDir ?: return false
            val contentFile = File(File(skillsDir, skillDir), "skill.md")
            contentFile.writeText(it)
        }
        
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("description", skill.description)
            put("argument_hint", skill.argumentHint)
            put("disable_model_invocation", if (skill.disableModelInvocation) 1 else 0)
            put("user_invocable", if (skill.userInvocable) 1 else 0)
            put("allowed_tools", skill.allowedTools?.joinToString(","))
            put("context", skill.context)
            put("updated_at", now)
        }
        
        return db.update(PhoneClawDatabaseHelper.TABLE_SKILL_INDEX, values, "name = ?", arrayOf(skill.name)) > 0
    }
    
    fun delete(name: String): Boolean {
        val skill = getByName(name) ?: return false
        val skillDir = skill.skillDir?.let { File(skillsDir, it) }
        skillDir?.deleteRecursively()
        
        val db = dbHelper.writableDatabase
        return db.delete(PhoneClawDatabaseHelper.TABLE_SKILL_INDEX, "name = ?", arrayOf(name)) > 0
    }
}

private fun SkillEntity.toDomain() = Skill(
    name = name,
    description = description,
    argumentHint = argumentHint,
    disableModelInvocation = disableModelInvocation,
    userInvocable = userInvocable,
    allowedTools = allowedTools?.split(","),
    context = context,
    source = SkillSource.USER,
    skillDir = skillDir,
    supportingFiles = supportingFiles?.split(",") ?: emptyList()
)
