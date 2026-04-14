package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import android.content.Context
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.SkillEntity
import top.yudoge.phoneclaw.llm.data.entity.SkillEntityWithContent
import java.io.File
import java.nio.charset.StandardCharsets

class UserSkillRepository(
    private val context: Context,
    private val dbHelper: PhoneClawDatabaseHelper
) : SkillRepository {
    companion object {
        private const val SKILL_FILE = "SKILL.md"
        private const val LEGACY_SKILL_FILE = "skill.md"
    }

    private val skillsDir: File by lazy {
        File(context.filesDir, "skills").also { it.mkdirs() }
    }
    
    override fun getAll(): List<SkillEntity> {
        val entities = mutableListOf<SkillEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SKILL_INDEX,
            null, null, null, null, null, "updated_at DESC"
        )
        
        while (cursor.moveToNext()) {
            entities.add(SkillEntity(
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
            ))
        }
        cursor.close()
        return entities
    }
    
    override fun getByName(name: String): SkillEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SKILL_INDEX,
            null, "name = ?", arrayOf(name), null, null, null
        )
        
        var entity: SkillEntity? = null
        if (cursor.moveToFirst()) {
            entity = SkillEntity(
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
        }
        cursor.close()
        return entity
    }
    
    override fun getContent(entity: SkillEntity): SkillEntityWithContent? {
        val skillDirName = resolveSkillDirName(entity)
        val contentFile = resolveSkillContentFile(File(skillsDir, skillDirName)) ?: return null
        
        return try {
            val content = contentFile.readText(StandardCharsets.UTF_8)
            SkillEntityWithContent(entity, content)
        } catch (_: Exception) {
            null
        }
    }
    
    override fun insert(entity: SkillEntity, content: String): Boolean {
        val now = System.currentTimeMillis()
        val skillDirName = resolveSkillDirName(entity)
        val skillDir = File(skillsDir, skillDirName)
        if (!skillDir.exists() && !skillDir.mkdirs()) {
            return false
        }
        
        if (!writeSkillContent(skillDir, content)) {
            return false
        }
        
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", entity.name)
            put("description", entity.description)
            put("argument_hint", entity.argumentHint)
            put("disable_model_invocation", if (entity.disableModelInvocation) 1 else 0)
            put("user_invocable", if (entity.userInvocable) 1 else 0)
            put("allowed_tools", entity.allowedTools)
            put("context", entity.context)
            put("skill_dir", skillDirName)
            put("supporting_files", entity.supportingFiles)
            put("created_at", now)
            put("updated_at", now)
        }
        
        return db.insertWithOnConflict(
            PhoneClawDatabaseHelper.TABLE_SKILL_INDEX,
            null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }
    
    override fun update(entity: SkillEntity, content: String?): Boolean {
        val now = System.currentTimeMillis()
        val skillDirName = resolveSkillDirName(entity)
        val skillDir = File(skillsDir, skillDirName)
        if (!skillDir.exists() && !skillDir.mkdirs()) {
            return false
        }
        
        content?.let {
            if (!writeSkillContent(skillDir, it)) {
                return false
            }
        }
        
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("description", entity.description)
            put("argument_hint", entity.argumentHint)
            put("disable_model_invocation", if (entity.disableModelInvocation) 1 else 0)
            put("user_invocable", if (entity.userInvocable) 1 else 0)
            put("allowed_tools", entity.allowedTools)
            put("context", entity.context)
            put("skill_dir", skillDirName)
            put("updated_at", now)
        }
        
        return db.update(PhoneClawDatabaseHelper.TABLE_SKILL_INDEX, values, "name = ?", arrayOf(entity.name)) > 0
    }
    
    override fun delete(name: String): Boolean {
        val entity = getByName(name) ?: return false
        val skillDir = entity.skillDir?.let { File(skillsDir, it) }
        skillDir?.deleteRecursively()
        
        val db = dbHelper.writableDatabase
        return db.delete(PhoneClawDatabaseHelper.TABLE_SKILL_INDEX, "name = ?", arrayOf(name)) > 0
    }

    private fun resolveSkillDirName(entity: SkillEntity): String {
        return entity.skillDir?.takeIf { it.isNotBlank() } ?: sanitizeDirName(entity.name)
    }

    private fun sanitizeDirName(name: String): String {
        return name.trim()
            .replace("\\s+".toRegex(), "_")
            .replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
    }

    private fun resolveSkillContentFile(skillDir: File): File? {
        val preferred = File(skillDir, SKILL_FILE)
        if (preferred.exists()) return preferred
        val legacy = File(skillDir, LEGACY_SKILL_FILE)
        if (legacy.exists()) return legacy
        return null
    }

    private fun writeSkillContent(skillDir: File, content: String): Boolean {
        return try {
            File(skillDir, SKILL_FILE).writeText(content, StandardCharsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }
}
