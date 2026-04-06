package top.yudoge.phoneclaw.llm.provider

import android.content.ContentValues
import android.content.Context
import android.util.Log
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class ModelProviderRepositoryImpl(context: Context) : ModelProviderRepository {

    private val dbHelper: PhoneClawDbHelper = PhoneClawDbHelper(context)

    companion object {
        private const val TABLE = "model_providers"
        private const val TAG = "ModelProviderRepo"
    }

    override fun addProvider(provider: ModelProviderEntity): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", provider.name)
            put("api_type", provider.apiType.name)
            put("model_provider_config", provider.modelProviderConfig)
        }
        val id = db.insert(TABLE, null, values)
        Log.d(TAG, "addProvider: name=${provider.name}, id=$id")
        return id
    }

    override fun deleteProvider(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE, "id = ?", arrayOf(id.toString()))
        Log.d(TAG, "deleteProvider: id=$id")
    }

    override fun getProvider(id: Long): ModelProviderEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(TABLE, null, "id = ?", arrayOf(id.toString()), null, null, null)
        var provider: ModelProviderEntity? = null
        if (cursor.moveToFirst()) {
            provider = ModelProviderEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                apiType = APIType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("api_type"))),
                hasVisualCapability = false,
                modelProviderConfig = cursor.getString(cursor.getColumnIndexOrThrow("model_provider_config"))
            )
        }
        cursor.close()
        Log.d(TAG, "getProvider: id=$id, found=${provider != null}")
        return provider
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
                hasVisualCapability = false,
                modelProviderConfig = cursor.getString(cursor.getColumnIndexOrThrow("model_provider_config"))
            ))
        }
        cursor.close()
        Log.d(TAG, "listProvider: ${providers.size} providers")
        return providers
    }

    override fun updateProvider(provider: ModelProviderEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", provider.name)
            put("api_type", provider.apiType.name)
            put("model_provider_config", provider.modelProviderConfig)
        }
        db.update(TABLE, values, "id = ?", arrayOf(provider.id.toString()))
        Log.d(TAG, "updateProvider: id=${provider.id}")
    }
}
