package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.ModelEntity
import top.yudoge.phoneclaw.llm.domain.objects.Model

class ModelRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : ModelRepository {
    
    override fun getAll(): List<Model> {
        val models = mutableListOf<Model>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODELS,
            null, null, null, null, null, null
        )
        
        while (cursor.moveToNext()) {
            val entity = ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")) ?: "",
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            )
            models.add(entity.toDomain())
        }
        cursor.close()
        return models
    }
    
    override fun getById(id: String): Model? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODELS,
            null, "id = ?", arrayOf(id), null, null, null
        )
        
        var model: Model? = null
        if (cursor.moveToFirst()) {
            val entity = ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")) ?: "",
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            )
            model = entity.toDomain()
        }
        cursor.close()
        return model
    }
    
    override fun getByProviderId(providerId: Long): List<Model> {
        val models = mutableListOf<Model>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODELS,
            null, "provider_id = ?", arrayOf(providerId.toString()), null, null, null
        )
        
        while (cursor.moveToNext()) {
            val entity = ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")) ?: "",
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            )
            models.add(entity.toDomain())
        }
        cursor.close()
        return models
    }
    
    override fun insert(model: Model) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", model.id)
            put("provider_id", model.providerId)
            put("display_name", model.displayName)
            put("has_visual_capability", if (model.hasVisualCapability) 1 else 0)
        }
        db.insertWithOnConflict(PhoneClawDatabaseHelper.TABLE_MODELS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    override fun update(model: Model) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("provider_id", model.providerId)
            put("display_name", model.displayName)
            put("has_visual_capability", if (model.hasVisualCapability) 1 else 0)
        }
        db.update(PhoneClawDatabaseHelper.TABLE_MODELS, values, "id = ?", arrayOf(model.id))
    }
    
    override fun delete(id: String) {
        val db = dbHelper.writableDatabase
        db.delete(PhoneClawDatabaseHelper.TABLE_MODELS, "id = ?", arrayOf(id))
    }
}

private fun ModelEntity.toDomain() = Model(
    id = id,
    providerId = providerId,
    displayName = displayName,
    hasVisualCapability = hasVisualCapability
)
