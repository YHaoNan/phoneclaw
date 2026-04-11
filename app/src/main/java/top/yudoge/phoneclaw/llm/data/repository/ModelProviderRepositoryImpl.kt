package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.ModelProviderEntity
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

class ModelProviderRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : ModelProviderRepository {
    
    override fun getAll(): List<ModelProvider> {
        val providers = mutableListOf<ModelProvider>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS,
            null, null, null, null, null, null
        )
        
        while (cursor.moveToNext()) {
            val entity = ModelProviderEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                apiType = cursor.getString(cursor.getColumnIndexOrThrow("api_type")),
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1,
                modelProviderConfig = cursor.getString(cursor.getColumnIndexOrThrow("model_provider_config")) ?: ""
            )
            providers.add(entity.toDomain())
        }
        cursor.close()
        return providers
    }
    
    override fun getById(id: Long): ModelProvider? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS,
            null, "id = ?", arrayOf(id.toString()), null, null, null
        )
        
        var provider: ModelProvider? = null
        if (cursor.moveToFirst()) {
            val entity = ModelProviderEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                apiType = cursor.getString(cursor.getColumnIndexOrThrow("api_type")),
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1,
                modelProviderConfig = cursor.getString(cursor.getColumnIndexOrThrow("model_provider_config")) ?: ""
            )
            provider = entity.toDomain()
        }
        cursor.close()
        return provider
    }
    
    override fun insert(modelProvider: ModelProvider): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", modelProvider.name)
            put("api_type", modelProvider.apiType)
            put("has_visual_capability", if (modelProvider.hasVisualCapability) 1 else 0)
            put("model_provider_config", modelProvider.config)
        }
        return db.insert(PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS, null, values)
    }
    
    override fun update(modelProvider: ModelProvider) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", modelProvider.name)
            put("api_type", modelProvider.apiType)
            put("has_visual_capability", if (modelProvider.hasVisualCapability) 1 else 0)
            put("model_provider_config", modelProvider.config)
        }
        db.update(PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS, values, "id = ?", arrayOf(modelProvider.id.toString()))
    }
    
    override fun delete(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS, "id = ?", arrayOf(id.toString()))
    }
}

private fun ModelProviderEntity.toDomain() = ModelProvider(
    id = id,
    name = name,
    apiType = apiType,
    hasVisualCapability = hasVisualCapability,
    config = modelProviderConfig
)
