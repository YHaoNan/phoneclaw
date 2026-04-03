package top.yudoge.phoneclaw.agent;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import top.yudoge.hanai.core.agent.AgentLoopEventListener;
import top.yudoge.hanai.core.agent.AgentUsage;
import top.yudoge.hanai.core.agent.SimpleChatModelAgent;
import top.yudoge.hanai.core.chat.ChatModel;
import top.yudoge.hanai.core.chat.Message;
import top.yudoge.hanai.core.memory.FixedCountMemory;
import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.openai.OpenAIChatModel;
import top.yudoge.hanai.openai.OpenAIResponseAPIChatModel;
import top.yudoge.phoneclaw.db.SessionDbHelper;
import top.yudoge.phoneclaw.tools.EmuOperationTool;
import top.yudoge.phoneclaw.tools.GetByTextTool;
import top.yudoge.phoneclaw.tools.GetScreenTool;
import top.yudoge.phoneclaw.tools.OpenIntentTool;

public class SessionManager {

    private static SessionManager instance;

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    private final Map<String, Session> sessions = new HashMap<>();
    private String activeSessionId;
    private Context context;
    private SessionDbHelper dbHelper;

    private SessionManager() {}

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new SessionDbHelper(context);
    }

    public Session createSession() {
        return createSession(null);
    }

    public Session createSession(String modelId) {
        String sessionId = UUID.randomUUID().toString();
        long createdAt = System.currentTimeMillis();
        
        SessionDbHelper.ModelConfig model = null;
        if (modelId != null && !modelId.isEmpty()) {
            model = dbHelper.getModel(modelId);
        }
        if (model == null) {
            model = dbHelper.getDefaultModel();
        }
        if (model == null) {
            model = new SessionDbHelper.ModelConfig("default", "Default", "", "", true);
        }
        
        dbHelper.saveSession(sessionId, "New Session", createdAt, model.id);
        
        Session session = new Session(sessionId, "New Session", createdAt, context, model, dbHelper);
        sessions.put(sessionId, session);
        activeSessionId = sessionId;
        return session;
    }

    public Session loadSession(String sessionId) {
        if (sessions.containsKey(sessionId)) {
            activeSessionId = sessionId;
            return sessions.get(sessionId);
        }
        
        SessionDbHelper.SessionRecord record = dbHelper.getSession(sessionId);
        if (record == null) return null;
        
        SessionDbHelper.ModelConfig model = record.modelId != null ? dbHelper.getModel(record.modelId) : dbHelper.getDefaultModel();
        if (model == null) {
            model = new SessionDbHelper.ModelConfig("default", "Default", "", "", true);
        }
        
        Session session = new Session(sessionId, record.title, record.createdAt, context, model, dbHelper);
        session.loadMessagesFromDb();
        sessions.put(sessionId, session);
        activeSessionId = sessionId;
        return session;
    }

    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public Session getActiveSession() {
        if (activeSessionId == null) return null;
        return sessions.get(activeSessionId);
    }

    public void setActiveSession(String sessionId) {
        activeSessionId = sessionId;
    }

    public void deleteSession(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session != null) {
            session.shutdown();
        }
        dbHelper.deleteSession(sessionId);
        if (sessionId.equals(activeSessionId)) {
            List<SessionDbHelper.SessionRecord> allSessions = dbHelper.getAllSessions();
            activeSessionId = allSessions.isEmpty() ? null : allSessions.get(0).id;
        }
    }

    public List<SessionInfo> getSessionList() {
        List<SessionInfo> list = new ArrayList<>();
        List<SessionDbHelper.SessionRecord> records = dbHelper.getAllSessions();
        for (SessionDbHelper.SessionRecord record : records) {
            list.add(new SessionInfo(record.id, record.title, record.createdAt, record.modelId));
        }
        return list;
    }

    public List<SessionDbHelper.ModelConfig> getModelList() {
        return dbHelper.getAllModels();
    }

    public void saveModel(SessionDbHelper.ModelConfig model) {
        dbHelper.saveModel(model);
    }

    public void deleteModel(String modelId) {
        dbHelper.deleteModel(modelId);
    }

    public void saveSelectedModel(String modelId) {
        dbHelper.saveSelectedModel(modelId);
    }

    public String getSelectedModel() {
        return dbHelper.getSelectedModel();
    }

    public void setDefaultModel(String modelId) {
        dbHelper.setDefaultModel(modelId);
    }

    public void shutdownAll() {
        for (Session session : sessions.values()) {
            session.shutdown();
        }
        sessions.clear();
        activeSessionId = null;
    }

    public static class SessionInfo {
        public final String id;
        public final String title;
        public final long createdAt;
        public final String modelId;

        public SessionInfo(String id, String title, long createdAt, String modelId) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
            this.modelId = modelId;
        }
    }

    public static class Session {
        private final String id;
        private String title;
        private final long createdAt;
        private SimpleChatModelAgent agent;
        private ChatModel chatModel;
        private FixedCountMemory memory;
        private final List<MessageRecord> messages = new ArrayList<>();
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final AtomicBoolean isRunning = new AtomicBoolean(false);
        private final SessionDbHelper dbHelper;
        private final Context context;
        private SessionDbHelper.ModelConfig modelConfig;
        private SessionListener listener;

        public Session(String id, String title, long createdAt, Context context, SessionDbHelper.ModelConfig modelConfig, SessionDbHelper dbHelper) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
            this.dbHelper = dbHelper;
            this.context = context;
            this.modelConfig = modelConfig;
            initAgent();
        }

        private void initAgent() {
            if (this.memory == null) {
                this.memory = new FixedCountMemory(1000);
            }
            String url = modelConfig.baseUrl != null && !modelConfig.baseUrl.isEmpty() 
                    ? modelConfig.baseUrl 
                    : "https://api.openai.com/v1/chat/completions";
            String model = modelConfig.name != null && !modelConfig.name.isEmpty() 
                    ? modelConfig.name 
                    : "gpt-4o-mini";
            android.util.Log.d("SessionManager", "initAgent: modelId=" + modelConfig.id + ", name=" + model + ", url=" + url + ", hasApiKey=" + (modelConfig.apiKey != null && !modelConfig.apiKey.isEmpty()));
            this.chatModel = createChatModel(modelConfig);

            this.agent = new SimpleChatModelAgent(memory);
            this.agent.registerChatModel(chatModel);
            this.agent.registerTool(new OpenIntentTool(context));
            this.agent.registerTool(new GetScreenTool());
            this.agent.registerTool(new GetByTextTool());
            this.agent.registerTool(new EmuOperationTool());
        }
        
        public void updateModel(SessionDbHelper.ModelConfig newModelConfig) {
            if (isRunning.get()) {
                return;
            }
            this.modelConfig = newModelConfig;
            dbHelper.updateSessionModel(id, newModelConfig.id);
            initAgent();
        }
        
        public String getModelId() {
            return modelConfig != null ? modelConfig.id : null;
        }

        public void loadMessagesFromDb() {
            List<SessionDbHelper.MessageRecord> records = dbHelper.getMessages(id);
            messages.clear();
            for (SessionDbHelper.MessageRecord record : records) {
                MessageRecord mr = new MessageRecord(
                        MessageRole.valueOf(record.role),
                        record.content != null ? record.content : "",
                        record.timestamp
                );
                mr.toolName = record.toolName;
                mr.toolParams = record.toolParams;
                mr.toolResult = record.toolResult;
                if (record.toolState != null) {
                    mr.toolState = ToolState.valueOf(record.toolState);
                }
                mr.success = record.success;
                messages.add(mr);
            }
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public long getCreatedAt() { return createdAt; }
        public List<MessageRecord> getMessages() { return messages; }
        public boolean isRunning() { return isRunning.get(); }

        public void setListener(SessionListener listener) {
            this.listener = listener;
        }

        public void stop() {
            if (isRunning.get()) {
                agent.stop();
            }
        }

        public void sendMessage(String content) {
            MessageRecord userRecord = new MessageRecord(MessageRole.USER, content, System.currentTimeMillis());
            messages.add(userRecord);
            dbHelper.saveMessage(id, toDbMessageRecord(userRecord));

            if (listener != null) {
                mainHandler.post(() -> listener.onMessageAdded(messages.size() - 1));
            }

            Message initialMessage = Message.user(content);
            isRunning.set(true);
            if (listener != null) {
                mainHandler.post(() -> listener.onSessionStarted());
            }
            
            executor.execute(() -> {
                try {
                    maybeGenerateSessionTitle(content);
                    agent.start(initialMessage, new AgentLoopEventListener() {
                        private String currentTurnId;
                        private StringBuilder currentContent;
                        private int messageIndex = -1;

                        @Override
                        public void onThinkingStart(String turnId) {
                            currentTurnId = turnId;
                            currentContent = new StringBuilder();
                            messageIndex = messages.size();
                            MessageRecord record = new MessageRecord(MessageRole.AI_THINKING, "", System.currentTimeMillis());
                            messages.add(record);
                            dbHelper.saveMessage(id, toDbMessageRecord(record));
                            if (listener != null) {
                                mainHandler.post(() -> listener.onMessageAdded(messageIndex));
                            }
                        }

                        @Override
                        public void onThinking(String turnId, String content) {
                            if (currentContent != null) {
                                currentContent.append(content);
                                if (messageIndex >= 0 && messageIndex < messages.size()) {
                                    MessageRecord record = messages.get(messageIndex);
                                    record.content = currentContent.toString();
                                    dbHelper.updateMessage(id, record.timestamp, toDbMessageRecord(record));
                                    if (listener != null) {
                                        mainHandler.post(() -> listener.onMessageUpdated(messageIndex));
                                    }
                                }
                            }
                        }

                        @Override
                        public void onThinkingEnd(String turnId, Long timeConsumed) {
                            if (messageIndex >= 0 && messageIndex < messages.size()) {
                                MessageRecord record = messages.get(messageIndex);
                                record.role = MessageRole.AI;
                                dbHelper.updateMessage(id, record.timestamp, toDbMessageRecord(record));
                                if (listener != null) {
                                    mainHandler.post(() -> listener.onMessageUpdated(messageIndex));
                                }
                            }
                        }

                        @Override
                        public void onToolCallingStart(String turnId, ToolDefinition definition, ToolCall toolCall) {
                            MessageRecord toolMessage = new MessageRecord(
                                    MessageRole.TOOL_CALL,
                                    "",
                                    System.currentTimeMillis()
                            );
                            toolMessage.toolName = definition.getIdentifier();
                            toolMessage.toolState = ToolState.PENDING;
                            
                            if (toolCall.getParams() != null && toolCall.getParams().getValues() != null) {
                                try {
                                    toolMessage.toolParams = new JSONObject(toolCall.getParams().getValues()).toString(2);
                                } catch (Exception e) {
                                    toolMessage.toolParams = toolCall.getParams().getValues().toString();
                                }
                            }
                            
                            messages.add(toolMessage);
                            dbHelper.saveMessage(id, toDbMessageRecord(toolMessage));
                            final int idx = messages.size() - 1;
                            if (listener != null) {
                                mainHandler.post(() -> listener.onMessageAdded(idx));
                            }
                        }

                        @Override
                        public void onToolCallingEnd(String turnId, ToolCall toolCall, ToolCallResult toolCallResult, Long timeConsumed) {
                            for (int i = messages.size() - 1; i >= 0; i--) {
                                MessageRecord record = messages.get(i);
                                if (record.role == MessageRole.TOOL_CALL && record.toolState == ToolState.PENDING) {
                                    if (toolCallResult != null) {
                                        record.success = toolCallResult.isSuccess();
                                        record.toolState = toolCallResult.isSuccess() ? ToolState.SUCCESS : ToolState.FAILED;
                                        record.toolResult = toolCallResult.getValue() != null 
                                                ? toolCallResult.getValue().toString() 
                                                : toolCallResult.getErrorMessage();
                                    } else {
                                        record.success = false;
                                        record.toolState = ToolState.FAILED;
                                        record.toolResult = "No result";
                                    }
                                    dbHelper.updateMessage(id, record.timestamp, toDbMessageRecord(record));
                                    final int idx = i;
                                    if (listener != null) {
                                        mainHandler.post(() -> listener.onMessageUpdated(idx));
                                    }
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onError(String reason, Exception e, Long timeConsumed) {
                            MessageRecord errorRecord = new MessageRecord(MessageRole.ERROR, reason, System.currentTimeMillis());
                            messages.add(errorRecord);
                            dbHelper.saveMessage(id, toDbMessageRecord(errorRecord));
                            if (listener != null) {
                                mainHandler.post(() -> {
                                    listener.onMessageAdded(messages.size() - 1);
                                    listener.onError(reason, e);
                                });
                            }
                        }

                        @Override
                        public void onComplete(Long timeConsumed) {}

                        @Override
                        public void onFinish(String turnId, AgentUsage usage, Long timeConsumed) {
                            isRunning.set(false);
                            if (listener != null) {
                                mainHandler.post(() -> listener.onSessionFinished());
                            }
                        }
                    });
                } catch (Exception e) {
                    isRunning.set(false);
                    if (listener != null) {
                        mainHandler.post(() -> listener.onError("Agent error: " + e.getMessage(), e));
                    }
                }
            });
        }

        private void maybeGenerateSessionTitle(String firstUserMessage) {
            if (!"New Session".equals(title)) {
                return;
            }

            int userCount = 0;
            for (MessageRecord record : messages) {
                if (record.role == MessageRole.USER) {
                    userCount++;
                }
            }
            if (userCount != 1) {
                return;
            }

            try {
                ChatModel titleModel = createChatModel(modelConfig);
                Message system = Message.system("Generate a short chat title (max 20 chars) for user request. Return title text only.");
                Message user = Message.user(firstUserMessage);
                String generated = titleModel.chat(Arrays.asList(system, user)).getMessage().getContent();
                String cleaned = cleanTitle(generated);
                if (!cleaned.isEmpty()) {
                    title = cleaned;
                    dbHelper.updateSessionTitle(id, cleaned);
                    if (listener != null) {
                        String finalTitle = cleaned;
                        mainHandler.post(() -> listener.onSessionTitleUpdated(finalTitle));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        private String cleanTitle(String raw) {
            if (raw == null) {
                return "";
            }
            String t = raw.replace("\n", " ").replace("\r", " ").trim();
            if (t.startsWith("\"") && t.endsWith("\"") && t.length() > 1) {
                t = t.substring(1, t.length() - 1).trim();
            }
            if (t.length() > 20) {
                t = t.substring(0, 20).trim();
            }
            return t;
        }

        private ChatModel createChatModel(SessionDbHelper.ModelConfig config) {
            String url = config.baseUrl != null && !config.baseUrl.isEmpty()
                    ? config.baseUrl
                    : "https://api.openai.com/v1/chat/completions";
            String model = config.name != null && !config.name.isEmpty()
                    ? config.name
                    : "gpt-4o-mini";
            String provider = config.providerType != null
                    ? config.providerType
                    : SessionDbHelper.ModelConfig.PROVIDER_OPENAI_CHAT;

            if (SessionDbHelper.ModelConfig.PROVIDER_OPENAI_RESPONSE.equals(provider)) {
                return new OpenAIResponseAPIChatModel(url, model, config.apiKey);
            }
            return new OpenAIChatModel(url, model, config.apiKey);
        }

        private SessionDbHelper.MessageRecord toDbMessageRecord(MessageRecord mr) {
            SessionDbHelper.MessageRecord record = new SessionDbHelper.MessageRecord();
            record.role = mr.role.name();
            record.content = mr.content;
            record.toolName = mr.toolName;
            record.toolParams = mr.toolParams;
            record.toolResult = mr.toolResult;
            record.toolState = mr.toolState != null ? mr.toolState.name() : null;
            record.success = mr.success;
            record.timestamp = mr.timestamp;
            return record;
        }

        public void shutdown() {
            if (agent != null) {
                agent.shutdown();
            }
            executor.shutdown();
        }
    }

    public enum MessageRole {
        USER, AI, AI_THINKING, TOOL_CALL, ERROR
    }

    public enum ToolState {
        PENDING, SUCCESS, FAILED
    }

    public static class MessageRecord {
        public final long timestamp;
        public MessageRole role;
        public String content;
        public String toolName;
        public String toolParams;
        public String toolResult;
        public ToolState toolState;
        public boolean success;

        public MessageRecord(MessageRole role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
            this.success = true;
        }
    }

    public interface SessionListener {
        void onSessionStarted();
        void onSessionTitleUpdated(String title);
        void onMessageAdded(int index);
        void onMessageUpdated(int index);
        void onSessionFinished();
        void onError(String reason, Exception e);
    }
}
