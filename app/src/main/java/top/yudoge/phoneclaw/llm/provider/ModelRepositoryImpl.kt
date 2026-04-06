package top.yudoge.phoneclaw.llm.provider

import android.content.ContentValues
import android.content.Context
import android.util.Log
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class ModelRepositoryImpl(context: Context) : ModelRepository {

    private val dbHelper: PhoneClawDbHelper = PhoneClawDbHelper(context)

    companion object {
        private const val TABLE = "models"
        private const val TAG = "ModelRepository"
    }

    override fun addModel(model: ModelEntity) {
        Log.d(TAG, "addModel: id=${model.id}, providerId=${model.providerId}, displayName=${model.displayName}")
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", model.id)
            put("provider_id", model.providerId)
            put("display_name", model.displayName)
            put("has_visual_capability", if (model.hasVisualCapability) 1 else 0)
        }
        val result = db.insertWithOnConflict(TABLE, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        Log.d(TAG, "addModel result: $result")
    }

    override fun updateModel(model: ModelEntity) {
        Log.d(TAG, "updateModel: id=${model.id}, providerId=${model.providerId}, displayName=${model.displayName}")
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("provider_id", model.providerId)
            put("display_name", model.displayName)
            put("has_visual_capability", if (model.hasVisualCapability) 1 else 0)
        }
        db.update(TABLE, values, "id = ?", arrayOf(model.id))
    }

    override fun deleteModel(id: String) {
        Log.d(TAG, "deleteModel: id=$id")
        val db = dbHelper.writableDatabase
        db.delete(TABLE, "id = ?", arrayOf(id))
    }

    override fun getModelsByProvider(providerId: Long): List<ModelEntity> {
        Log.d(TAG, "getModelsByProvider: providerId=$providerId")
        val db = dbHelper.readableDatabase
        val cursor = db.query(TABLE, null, "provider_id = ?", arrayOf(providerId.toString()), null, null, "display_name ASC")
        val models = mutableListOf<ModelEntity>()
        while (cursor.moveToNext()) {
            models.add(ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")),
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            ))
        }
        cursor.close()
        Log.d(TAG, "getModelsByProvider result: ${models.size} models")
        return models
    }

    override fun getModel(id: String): ModelEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(TABLE, null, "id = ?", arrayOf(id), null, null, null)
        var model: ModelEntity? = null
        if (cursor.moveToFirst()) {
            model = ModelEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                providerId = cursor.getLong(cursor.getColumnIndexOrThrow("provider_id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")),
                hasVisualCapability = cursor.getInt(cursor.getColumnIndexOrThrow("has_visual_capability")) == 1
            )
        }
        cursor.close()
        return model
    }
}
