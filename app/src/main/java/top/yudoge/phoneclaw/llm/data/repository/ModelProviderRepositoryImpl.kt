package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.ModelProviderEntity

class ModelProviderRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : ModelProviderRepository {
    
    override fun getAll(): List<ModelProviderEntity> {
        val providers = mutableListOf<ModelProviderEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS,
            null, null, null, null, null, null
        )
        
        while (cursor.moveToNext()) {
            providers.add(ModelProviderEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                providerType = cursor.getString(cursor.getColumnIndexOrThrow("provider_type")),
                modelProviderConfig = cursor.getString(cursor.getColumnIndexOrThrow("model_provider_config")) ?: ""
            ))
        }
        cursor.close()
        return providers
    }
    
    override fun getById(id: Long): ModelProviderEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS,
            null, "id = ?", arrayOf(id.toString()), null, null, null
        )
        
        var provider: ModelProviderEntity? = null
        if (cursor.moveToFirst()) {
            provider = ModelProviderEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                providerType = cursor.getString(cursor.getColumnIndexOrThrow("provider_type")),
                modelProviderConfig = cursor.getString(cursor.getColumnIndexOrThrow("model_provider_config")) ?: ""
            )
        }
        cursor.close()
        return provider
    }
    
    override fun insert(entity: ModelProviderEntity): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", entity.name)
            put("provider_type", entity.providerType)
            put("model_provider_config", entity.modelProviderConfig)
        }
        return db.insert(PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS, null, values)
    }
    
    override fun update(entity: ModelProviderEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", entity.name)
            put("provider_type", entity.providerType)
            put("model_provider_config", entity.modelProviderConfig)
        }
        db.update(PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS, values, "id = ?", arrayOf(entity.id.toString()))
    }
    
    override fun delete(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(PhoneClawDatabaseHelper.TABLE_MODEL_PROVIDERS, "id = ?", arrayOf(id.toString()))
    }
}
