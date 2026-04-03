package top.yudoge.phoneclaw.llm.provider

import android.content.ContentValues
import android.content.Context
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class ModelProviderRepositoryImpl(context: Context) : ModelProviderRepository {

    private val dbHelper: PhoneClawDbHelper = PhoneClawDbHelper(context)

    companion object {
        private const val TABLE = "model_providers"
    }

    override fun addProvider(provider: ModelProviderEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", provider.name)
            put("api_type", provider.apiType.name)
            put("has_visual_capability", if (provider.hasVisualCapability) 1 else 0)
            put("model_provider_config", provider.modelProviderConfig)
        }
        db.insert(TABLE, null, values)
    }

    override fun deleteProvider(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    override fun listProvider(): List<ModelProviderEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(TABLE, null, null, null, null, null, "id ASC")
        val providers = mutableListOf<ModelProviderEntity>()
        while (cursor.moveToNext()) {
            providers.add(ModelProviderEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                apiType = APIType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("api_type"))),
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1,
                modelProviderConfig = cursor.getString(cursor.getColumnIndexOrThrow("model_provider_config"))
            ))
        }
        cursor.close()
        return providers
    }

    override fun updateProvider(provider: ModelProviderEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", provider.name)
            put("api_type", provider.apiType.name)
            put("has_visual_capability", if (provider.hasVisualCapability) 1 else 0)
            put("model_provider_config", provider.modelProviderConfig)
        }
        db.update(TABLE, values, "id = ?", arrayOf(provider.id.toString()))
    }
}
