package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.ModelEntity

class ModelRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : ModelRepository {
    
    override fun getAll(): List<ModelEntity> {
        val models = mutableListOf<ModelEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODELS,
            null, null, null, null, null, null
        )
        
        while (cursor.moveToNext()) {
            models.add(ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")) ?: "",
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            ))
        }
        cursor.close()
        return models
    }
    
    override fun getById(id: String): ModelEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODELS,
            null, "id = ?", arrayOf(id), null, null, null
        )
        
        var model: ModelEntity? = null
        if (cursor.moveToFirst()) {
            model = ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")) ?: "",
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            )
        }
        cursor.close()
        return model
    }
    
    override fun getByProviderId(providerId: Long): List<ModelEntity> {
        val models = mutableListOf<ModelEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODELS,
            null, "provider_id = ?", arrayOf(providerId.toString()), null, null, null
        )
        
        while (cursor.moveToNext()) {
            models.add(ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")) ?: "",
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            ))
        }
        cursor.close()
        return models
    }
    
    override fun insert(entity: ModelEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", entity.id)
            put("provider_id", entity.providerId)
            put("display_name", entity.displayName)
            put("has_visual_capability", if (entity.hasVisualCapability) 1 else 0)
        }
        db.insertWithOnConflict(PhoneClawDatabaseHelper.TABLE_MODELS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    override fun update(entity: ModelEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("provider_id", entity.providerId)
            put("display_name", entity.displayName)
            put("has_visual_capability", if (entity.hasVisualCapability) 1 else 0)
        }
        db.update(PhoneClawDatabaseHelper.TABLE_MODELS, values, "id = ?", arrayOf(entity.id))
    }
    
    override fun delete(id: String) {
        val db = dbHelper.writableDatabase
        db.delete(PhoneClawDatabaseHelper.TABLE_MODELS, "id = ?", arrayOf(id))
    }
}
