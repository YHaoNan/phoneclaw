package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.TaskScriptEntity

class TaskScriptRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : TaskScriptRepository {

    override fun getAll(): List<TaskScriptEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_TASK_SCRIPTS,
            null,
            null,
            null,
            null,
            null,
            "created_at DESC"
        )
        val list = mutableListOf<TaskScriptEntity>()
        while (cursor.moveToNext()) {
            list += cursorToEntity(cursor)
        }
        cursor.close()
        return list
    }

    override fun getById(id: String): TaskScriptEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_TASK_SCRIPTS,
            null,
            "id = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        val entity = if (cursor.moveToFirst()) cursorToEntity(cursor) else null
        cursor.close()
        return entity
    }

    override fun getByName(name: String): TaskScriptEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_TASK_SCRIPTS,
            null,
            "name = ?",
            arrayOf(name),
            null,
            null,
            null
        )
        val entity = if (cursor.moveToFirst()) cursorToEntity(cursor) else null
        cursor.close()
        return entity
    }

    override fun insert(entity: TaskScriptEntity): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("summary", entity.summary)
            put("created_at", entity.createdAt)
            put("content", entity.content)
        }
        return db.insertWithOnConflict(
            PhoneClawDatabaseHelper.TABLE_TASK_SCRIPTS,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT
        ) != -1L
    }

    override fun update(entity: TaskScriptEntity): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", entity.name)
            put("summary", entity.summary)
            put("content", entity.content)
        }
        return db.update(
            PhoneClawDatabaseHelper.TABLE_TASK_SCRIPTS,
            values,
            "id = ?",
            arrayOf(entity.id)
        ) > 0
    }

    override fun delete(id: String): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete(PhoneClawDatabaseHelper.TABLE_TASK_SCRIPTS, "id = ?", arrayOf(id)) > 0
    }

    private fun cursorToEntity(cursor: android.database.Cursor): TaskScriptEntity {
        return TaskScriptEntity(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            summary = cursor.getString(cursor.getColumnIndexOrThrow("summary")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
        )
    }
}
