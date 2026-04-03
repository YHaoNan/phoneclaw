package top.yudoge.phoneclaw.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "phone_claw.db";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_SESSIONS = "sessions";
    private static final String TABLE_MESSAGES = "messages";
    private static final String TABLE_MODELS = "models";
    private static final String TABLE_PREFS = "preferences";

    private static final String CREATE_SESSIONS_TABLE =
            "CREATE TABLE " + TABLE_SESSIONS + " (" +
            "id TEXT PRIMARY KEY, " +
            "title TEXT, " +
            "created_at INTEGER, " +
            "updated_at INTEGER, " +
            "model_id TEXT)";

    private static final String CREATE_MESSAGES_TABLE =
            "CREATE TABLE " + TABLE_MESSAGES + " (" +
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
            "FOREIGN KEY(session_id) REFERENCES sessions(id))";

    private static final String CREATE_MODELS_TABLE =
            "CREATE TABLE " + TABLE_MODELS + " (" +
            "id TEXT PRIMARY KEY, " +
            "name TEXT, " +
            "alias TEXT, " +
            "provider_type TEXT, " +
            "base_url TEXT, " +
            "api_key TEXT, " +
            "is_default INTEGER DEFAULT 0)";

    private static final String CREATE_PREFS_TABLE =
            "CREATE TABLE " + TABLE_PREFS + " (" +
            "key TEXT PRIMARY KEY, " +
            "value TEXT)";

    public SessionDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SESSIONS_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
        db.execSQL(CREATE_MODELS_TABLE);
        db.execSQL(CREATE_PREFS_TABLE);
        db.execSQL("CREATE INDEX idx_messages_session_id ON " + TABLE_MESSAGES + "(session_id)");
        
        db.execSQL("INSERT INTO " + TABLE_MODELS + " (id, name, alias, provider_type, base_url, api_key, is_default) VALUES ('default', 'Default', '默认', 'OPENAI_CHAT', 'https://api.openai.com/v1/chat/completions', '', 1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_MODELS_TABLE);
            db.execSQL("ALTER TABLE " + TABLE_SESSIONS + " ADD COLUMN model_id TEXT");
            db.execSQL("INSERT INTO " + TABLE_MODELS + " (id, name, alias, provider_type, base_url, api_key, is_default) VALUES ('default', 'Default', '默认', 'OPENAI_CHAT', 'https://api.openai.com/v1/chat/completions', '', 1)");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_MODELS + " ADD COLUMN alias TEXT");
            db.execSQL("UPDATE " + TABLE_MODELS + " SET alias = name WHERE alias IS NULL");
            db.execSQL(CREATE_PREFS_TABLE);
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_MODELS + " ADD COLUMN provider_type TEXT");
            db.execSQL("UPDATE " + TABLE_MODELS + " SET provider_type = 'OPENAI_CHAT' WHERE provider_type IS NULL");
        }
    }

    public String saveSession(String sessionId, String title, long createdAt, String modelId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", sessionId);
        values.put("title", title);
        values.put("created_at", createdAt);
        values.put("updated_at", System.currentTimeMillis());
        values.put("model_id", modelId);
        db.insertWithOnConflict(TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return sessionId;
    }

    public void updateSessionTitle(String sessionId, String title) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("updated_at", System.currentTimeMillis());
        db.update(TABLE_SESSIONS, values, "id = ?", new String[]{sessionId});
    }

    public void deleteSession(String sessionId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MESSAGES, "session_id = ?", new String[]{sessionId});
        db.delete(TABLE_SESSIONS, "id = ?", new String[]{sessionId});
    }

    public List<SessionRecord> getAllSessions() {
        List<SessionRecord> sessions = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SESSIONS, null, null, null, null, null, "updated_at DESC");
        while (cursor.moveToNext()) {
            SessionRecord record = new SessionRecord();
            record.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            record.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            record.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
            record.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
            int modelIdIdx = cursor.getColumnIndex("model_id");
            record.modelId = modelIdIdx >= 0 ? cursor.getString(modelIdIdx) : null;
            sessions.add(record);
        }
        cursor.close();
        return sessions;
    }

    public SessionRecord getSession(String sessionId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SESSIONS, null, "id = ?", new String[]{sessionId}, null, null, null);
        SessionRecord record = null;
        if (cursor.moveToFirst()) {
            record = new SessionRecord();
            record.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            record.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            record.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
            record.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
            int modelIdIdx = cursor.getColumnIndex("model_id");
            record.modelId = modelIdIdx >= 0 ? cursor.getString(modelIdIdx) : null;
        }
        cursor.close();
        return record;
    }

    public void saveMessage(String sessionId, MessageRecord message) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("session_id", sessionId);
        values.put("role", message.role);
        values.put("content", message.content);
        values.put("tool_name", message.toolName);
        values.put("tool_params", message.toolParams);
        values.put("tool_result", message.toolResult);
        values.put("tool_state", message.toolState);
        values.put("success", message.success ? 1 : 0);
        values.put("timestamp", message.timestamp);
        db.insert(TABLE_MESSAGES, null, values);
        
        db.execSQL("UPDATE " + TABLE_SESSIONS + " SET updated_at = ? WHERE id = ?",
                new Object[]{System.currentTimeMillis(), sessionId});
    }

    public void updateMessage(String sessionId, long timestamp, MessageRecord message) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("content", message.content);
        values.put("tool_name", message.toolName);
        values.put("tool_params", message.toolParams);
        values.put("tool_result", message.toolResult);
        values.put("tool_state", message.toolState);
        values.put("success", message.success ? 1 : 0);
        db.update(TABLE_MESSAGES, values, "session_id = ? AND timestamp = ?",
                new String[]{sessionId, String.valueOf(timestamp)});
    }

    public List<MessageRecord> getMessages(String sessionId) {
        List<MessageRecord> messages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, null, "session_id = ?",
                new String[]{sessionId}, null, null, "timestamp ASC");
        while (cursor.moveToNext()) {
            MessageRecord record = new MessageRecord();
            record.role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
            record.content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
            record.toolName = cursor.getString(cursor.getColumnIndexOrThrow("tool_name"));
            record.toolParams = cursor.getString(cursor.getColumnIndexOrThrow("tool_params"));
            record.toolResult = cursor.getString(cursor.getColumnIndexOrThrow("tool_result"));
            record.toolState = cursor.getString(cursor.getColumnIndexOrThrow("tool_state"));
            record.success = cursor.getInt(cursor.getColumnIndexOrThrow("success")) == 1;
            record.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
            messages.add(record);
        }
        cursor.close();
        return messages;
    }

    public void clearAllSessions() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
        db.delete(TABLE_SESSIONS, null, null);
    }

    public static class SessionRecord {
        public String id;
        public String title;
        public long createdAt;
        public long updatedAt;
        public String modelId;
    }

    public static class MessageRecord {
        public String role;
        public String content;
        public String toolName;
        public String toolParams;
        public String toolResult;
        public String toolState;
        public boolean success;
        public long timestamp;
    }

    public void saveModel(ModelConfig model) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", model.id);
        values.put("name", model.name);
        values.put("alias", model.alias != null ? model.alias : model.name);
        values.put("provider_type", model.providerType != null ? model.providerType : ModelConfig.PROVIDER_OPENAI_CHAT);
        values.put("base_url", model.baseUrl);
        values.put("api_key", model.apiKey);
        values.put("is_default", model.isDefault ? 1 : 0);
        db.insertWithOnConflict(TABLE_MODELS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteModel(String modelId) {
        if ("default".equals(modelId)) return;
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MODELS, "id = ?", new String[]{modelId});
    }

    public List<ModelConfig> getAllModels() {
        List<ModelConfig> models = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MODELS, null, null, null, null, null, "is_default DESC, name ASC");
        while (cursor.moveToNext()) {
            ModelConfig model = new ModelConfig();
            model.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            model.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            int aliasIdx = cursor.getColumnIndex("alias");
            model.alias = aliasIdx >= 0 && !cursor.isNull(aliasIdx) ? cursor.getString(aliasIdx) : model.name;
            int providerIdx = cursor.getColumnIndex("provider_type");
            model.providerType = providerIdx >= 0 && !cursor.isNull(providerIdx)
                    ? cursor.getString(providerIdx)
                    : ModelConfig.PROVIDER_OPENAI_CHAT;
            model.baseUrl = cursor.getString(cursor.getColumnIndexOrThrow("base_url"));
            model.apiKey = cursor.getString(cursor.getColumnIndexOrThrow("api_key"));
            model.isDefault = cursor.getInt(cursor.getColumnIndexOrThrow("is_default")) == 1;
            models.add(model);
        }
        cursor.close();
        return models;
    }

    public ModelConfig getModel(String modelId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MODELS, null, "id = ?", new String[]{modelId}, null, null, null);
        ModelConfig model = null;
        if (cursor.moveToFirst()) {
            model = new ModelConfig();
            model.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            model.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            int aliasIdx = cursor.getColumnIndex("alias");
            model.alias = aliasIdx >= 0 && !cursor.isNull(aliasIdx) ? cursor.getString(aliasIdx) : model.name;
            int providerIdx = cursor.getColumnIndex("provider_type");
            model.providerType = providerIdx >= 0 && !cursor.isNull(providerIdx)
                    ? cursor.getString(providerIdx)
                    : ModelConfig.PROVIDER_OPENAI_CHAT;
            model.baseUrl = cursor.getString(cursor.getColumnIndexOrThrow("base_url"));
            model.apiKey = cursor.getString(cursor.getColumnIndexOrThrow("api_key"));
            model.isDefault = cursor.getInt(cursor.getColumnIndexOrThrow("is_default")) == 1;
        }
        cursor.close();
        return model;
    }

    public ModelConfig getDefaultModel() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MODELS, null, "is_default = 1", null, null, null, null);
        ModelConfig model = null;
        if (cursor.moveToFirst()) {
            model = new ModelConfig();
            model.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            model.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            int aliasIdx = cursor.getColumnIndex("alias");
            model.alias = aliasIdx >= 0 && !cursor.isNull(aliasIdx) ? cursor.getString(aliasIdx) : model.name;
            int providerIdx = cursor.getColumnIndex("provider_type");
            model.providerType = providerIdx >= 0 && !cursor.isNull(providerIdx)
                    ? cursor.getString(providerIdx)
                    : ModelConfig.PROVIDER_OPENAI_CHAT;
            model.baseUrl = cursor.getString(cursor.getColumnIndexOrThrow("base_url"));
            model.apiKey = cursor.getString(cursor.getColumnIndexOrThrow("api_key"));
            model.isDefault = true;
        }
        cursor.close();
        return model;
    }

    public void setDefaultModel(String modelId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_MODELS + " SET is_default = 0");
        db.execSQL("UPDATE " + TABLE_MODELS + " SET is_default = 1 WHERE id = ?", new Object[]{modelId});
    }

    public void updateSessionModel(String sessionId, String modelId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("model_id", modelId);
        db.update(TABLE_SESSIONS, values, "id = ?", new String[]{sessionId});
    }

    public void saveSelectedModel(String modelId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key", "selected_model");
        values.put("value", modelId);
        db.insertWithOnConflict(TABLE_PREFS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getSelectedModel() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PREFS, null, "key = ?", new String[]{"selected_model"}, null, null, null);
        String value = null;
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndexOrThrow("value"));
        }
        cursor.close();
        return value;
    }

    public static class ModelConfig {
        public String id;
        public String name;
        public String alias;
        public String providerType;
        public String baseUrl;
        public String apiKey;
        public boolean isDefault;

        public static final String PROVIDER_OPENAI_CHAT = "OPENAI_CHAT";
        public static final String PROVIDER_OPENAI_RESPONSE = "OPENAI_RESPONSE";
        
        public ModelConfig() {}
        
        public ModelConfig(String id, String name, String baseUrl, String apiKey, boolean isDefault) {
            this.id = id;
            this.name = name;
            this.alias = name;
            this.providerType = PROVIDER_OPENAI_CHAT;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.isDefault = isDefault;
        }
        
        public ModelConfig(String id, String name, String alias, String baseUrl, String apiKey, boolean isDefault) {
            this.id = id;
            this.name = name;
            this.alias = alias != null ? alias : name;
            this.providerType = PROVIDER_OPENAI_CHAT;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.isDefault = isDefault;
        }
        
        public String getDisplayName() {
            return alias != null && !alias.isEmpty() ? alias : name;
        }
    }
}
