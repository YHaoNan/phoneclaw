package top.yudoge.phoneclaw.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PhoneClawDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    
    companion object {
        const val DATABASE_NAME = "phone_claw.db"
        const val DATABASE_VERSION = 1
        
        const val TABLE_SESSIONS = "sessions"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_MODELS = "models"
        const val TABLE_MODEL_PROVIDERS = "model_providers"
        const val TABLE_SKILL_INDEX = "skill_index"
        const val TABLE_PREFS = "preferences"
        
        private const val CREATE_SESSIONS_TABLE =
            "CREATE TABLE $TABLE_SESSIONS (" +
            "id TEXT PRIMARY KEY, " +
            "title TEXT, " +
            "created_at INTEGER, " +
            "updated_at INTEGER, " +
            "model_id TEXT)"
        
        private const val CREATE_MESSAGES_TABLE =
            "CREATE TABLE $TABLE_MESSAGES (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "session_id TEXT, " +
            "role TEXT, " +
            "content TEXT, " +
            "tool_name TEXT, " +
            "tool_params TEXT, " +
            "tool_result TEXT, " +
            "tool_state TEXT, " +
            "success INTEGER, " +
            "timestamp INTEGER, " +
            "FOREIGN KEY(session_id) REFERENCES sessions(id))"
        
        private const val CREATE_MODEL_PROVIDERS_TABLE =
            "CREATE TABLE $TABLE_MODEL_PROVIDERS (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "api_type TEXT NOT NULL, " +
            "has_visual_capability INTEGER DEFAULT 0, " +
            "model_provider_config TEXT)"
        
        private const val CREATE_MODELS_TABLE =
            "CREATE TABLE $TABLE_MODELS (" +
            "id TEXT PRIMARY KEY, " +
            "provider_id INTEGER, " +
            "display_name TEXT, " +
            "has_visual_capability INTEGER DEFAULT 0, " +
            "FOREIGN KEY(provider_id) REFERENCES model_providers(id))"
        
        private const val CREATE_SKILL_INDEX_TABLE =
            "CREATE TABLE $TABLE_SKILL_INDEX (" +
            "name TEXT PRIMARY KEY, " +
            "description TEXT NOT NULL, " +
            "argument_hint TEXT, " +
            "disable_model_invocation INTEGER DEFAULT 0, " +
            "user_invocable INTEGER DEFAULT 1, " +
            "allowed_tools TEXT, " +
            "context TEXT, " +
            "skill_dir TEXT NOT NULL, " +
            "supporting_files TEXT, " +
            "created_at INTEGER NOT NULL, " +
            "updated_at INTEGER NOT NULL)"
        
        private const val CREATE_PREFS_TABLE =
            "CREATE TABLE $TABLE_PREFS (" +
            "key TEXT PRIMARY KEY, " +
            "value TEXT)"
        
        @Volatile
        private var instance: PhoneClawDatabaseHelper? = null
        
        fun getInstance(context: Context): PhoneClawDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: PhoneClawDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SESSIONS_TABLE)
        db.execSQL(CREATE_MESSAGES_TABLE)
        db.execSQL(CREATE_MODEL_PROVIDERS_TABLE)
        db.execSQL(CREATE_MODELS_TABLE)
        db.execSQL(CREATE_SKILL_INDEX_TABLE)
        db.execSQL(CREATE_PREFS_TABLE)
        db.execSQL("CREATE INDEX idx_messages_session_id ON $TABLE_MESSAGES(session_id)")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // For new database, drop and recreate
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MODELS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MODEL_PROVIDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SKILL_INDEX")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PREFS")
        onCreate(db)
    }
}
