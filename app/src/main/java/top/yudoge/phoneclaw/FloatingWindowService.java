package top.yudoge.phoneclaw;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import top.yudoge.phoneclaw.agent.SessionManager;
import top.yudoge.phoneclaw.db.PhoneClawDbHelper;
import top.yudoge.phoneclaw.tools.GetScreenTool;

public class FloatingWindowService extends Service {

    @Nullable
    private static FloatingWindowService instance;
    public static boolean isRunning = false;

    private WindowManager mWindowManager;
    private View mFloatingViewCollapsed;
    private View mFloatingViewExpanded;
    private View mFloatingViewFullscreen;
    private WindowManager.LayoutParams collapsedParams;
    private WindowManager.LayoutParams expandedParams;
    private WindowManager.LayoutParams fullscreenParams;

    private ImageView floatingIcon;
    private TextView floatingStatusText;

    private ListView conversationListView;
    private EditText messageInput;
    private ImageButton voiceInputButton;
    private Button sendButton;
    private ImageButton newSessionButton;
    private ImageButton collapseButton;
    private ListView sessionListView;
    private ListView modelListView;

    private final List<SessionManager.MessageRecord> currentMessages = new ArrayList<>();
    private ChatMessageAdapter messageAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean userScrolled = false;

    private SessionManager.Session activeSession;
    private List<SessionManager.SessionInfo> sessionList = new ArrayList<>();
    private SessionListAdapter sessionAdapter;
    private android.widget.ArrayAdapter<PhoneClawDbHelper.ModelConfig> modelAdapter;

    private TextView tabChat, tabSessions, tabModels;
    private int currentTab = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        isRunning = true;

        SessionManager.getInstance().initialize(this);

        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.Theme_PhoneClaw);
        mFloatingViewCollapsed = LayoutInflater.from(themedContext).inflate(R.layout.layout_floating_collapsed, null);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        collapsedParams = getCollapsedLayoutParams();

        mWindowManager.addView(mFloatingViewCollapsed, collapsedParams);

        floatingIcon = mFloatingViewCollapsed.findViewById(R.id.floating_icon);
        floatingStatusText = mFloatingViewCollapsed.findViewById(R.id.floating_status_text);

        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        mFloatingViewCollapsed.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = collapsedParams.x;
                        initialY = collapsedParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            isDragging = true;
                        }
                        if (isDragging) {
                            collapsedParams.x = initialX + (int) dx;
                            collapsedParams.y = initialY + (int) dy;
                            mWindowManager.updateViewLayout(mFloatingViewCollapsed, collapsedParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            showExpandedView();
                        }
                        return true;
                }
                return false;
            }
        });

        messageAdapter = new ChatMessageAdapter(this, currentMessages);
        sessionAdapter = new SessionListAdapter(this, sessionList);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        isRunning = false;
        SessionManager.getInstance().shutdownAll();
        if (mFloatingViewCollapsed != null && mFloatingViewCollapsed.getParent() != null) {
            mWindowManager.removeView(mFloatingViewCollapsed);
        }
        if (mFloatingViewExpanded != null && mFloatingViewExpanded.getParent() != null) {
            mWindowManager.removeView(mFloatingViewExpanded);
        }
        if (mFloatingViewFullscreen != null && mFloatingViewFullscreen.getParent() != null) {
            mWindowManager.removeView(mFloatingViewFullscreen);
        }
    }

    private void showCollapsedView() {
        if (mFloatingViewExpanded != null && mFloatingViewExpanded.getParent() != null) {
            mWindowManager.removeView(mFloatingViewExpanded);
            mFloatingViewExpanded = null;
        }
        if (mFloatingViewFullscreen != null && mFloatingViewFullscreen.getParent() != null) {
            mWindowManager.removeView(mFloatingViewFullscreen);
            mFloatingViewFullscreen = null;
        }
        if (mFloatingViewCollapsed != null && mFloatingViewCollapsed.getParent() == null) {
            mWindowManager.addView(mFloatingViewCollapsed, collapsedParams);
        }
    }

    private void showExpandedView() {
        if (mFloatingViewCollapsed != null && mFloatingViewCollapsed.getParent() != null) {
            mWindowManager.removeView(mFloatingViewCollapsed);
        }
        if (mFloatingViewFullscreen != null && mFloatingViewFullscreen.getParent() != null) {
            mWindowManager.removeView(mFloatingViewFullscreen);
            mFloatingViewFullscreen = null;
        }
        if (mFloatingViewExpanded == null) {
            ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.Theme_PhoneClaw);
            mFloatingViewExpanded = LayoutInflater.from(themedContext).inflate(R.layout.layout_floating_expanded, null);
            expandedParams = getExpandedLayoutParams();
            mWindowManager.addView(mFloatingViewExpanded, expandedParams);

            conversationListView = mFloatingViewExpanded.findViewById(R.id.conversation_list_view);
            messageInput = mFloatingViewExpanded.findViewById(R.id.message_input);
            voiceInputButton = mFloatingViewExpanded.findViewById(R.id.voice_input_button);
            sendButton = mFloatingViewExpanded.findViewById(R.id.send_button);
            newSessionButton = mFloatingViewExpanded.findViewById(R.id.new_session_button);
            collapseButton = mFloatingViewExpanded.findViewById(R.id.collapse_button);
            ImageButton screenshotButton = mFloatingViewExpanded.findViewById(R.id.screenshot_button);
            ImageButton exportButton = mFloatingViewExpanded.findViewById(R.id.export_button);

            conversationListView.setAdapter(messageAdapter);
            
            sessionListView = mFloatingViewExpanded.findViewById(R.id.session_list_view);
            sessionListView.setAdapter(sessionAdapter);
            updateSessionList();
            setupSessionListView();
            
            modelListView = mFloatingViewExpanded.findViewById(R.id.model_list_view);
            setupModelListView();
            
            modelSpinner = mFloatingViewExpanded.findViewById(R.id.model_spinner);
            updateChatModelSpinner();
            
            // 设置半屏模式下tab内容的默认高度
            View tabContent = mFloatingViewExpanded.findViewById(R.id.tab_content);
            if (tabContent != null) {
                tabContent.setMinimumHeight((int) (220 * getResources().getDisplayMetrics().density));
            }
            
            setupTabs();
            
            conversationListView.setOnScrollListener(new ListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
                    if (scrollState == android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                        userScrolled = true;
                    }
                }
                @Override
                public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
            });

            if (activeSession != null && activeSession.isRunning()) {
                setSendButtonRunning(true);
            } else {
                setSendButtonRunning(false);
            }

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (activeSession != null && activeSession.isRunning()) {
                        activeSession.stop();
                    } else {
                        String message = messageInput.getText().toString().trim();
                        if (!message.isEmpty()) {
                            sendMessage(message);
                            messageInput.setText("");
                        }
                    }
                }
            });

            voiceInputButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(FloatingWindowService.this, "Voice input not yet implemented", Toast.LENGTH_SHORT).show();
                }
            });

            newSessionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewSession();
                }
            });

            collapseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCollapsedView();
                }
            });

            // 截图按钮 - 短按抓取当前屏幕内容，长按调用GetScreenTool
            setupScreenshotButton(screenshotButton);

            // 导出按钮 - 导出对话为Markdown并分享
            exportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportChatToMarkdown();
                }
            });

            // 添加双击顶部栏切换到全屏
            final long[] lastClickTime = {0};
            View dragHandle = mFloatingViewExpanded.findViewById(R.id.floating_drag_handle);
            final int expandedTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
            dragHandle.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = expandedParams.x;
                            initialY = expandedParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isDragging = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - initialTouchX;
                            float dy = event.getRawY() - initialTouchY;
                            if (!isDragging && (Math.abs(dx) > expandedTouchSlop || Math.abs(dy) > expandedTouchSlop)) {
                                isDragging = true;
                            }
                            if (isDragging) {
                                expandedParams.x = initialX + (int) dx;
                                expandedParams.y = initialY + (int) dy;
                                mWindowManager.updateViewLayout(mFloatingViewExpanded, expandedParams);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!isDragging) {
                                // 检测双击
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastClickTime[0] < 300) {
                                    // 双击切换到全屏
                                    showFullscreenView();
                                }
                                lastClickTime[0] = currentTime;
                            }
                            return true;
                    }
                    return false;
                }
            });

        }
    }

    private void showFullscreenView() {
        if (mFloatingViewCollapsed != null && mFloatingViewCollapsed.getParent() != null) {
            mWindowManager.removeView(mFloatingViewCollapsed);
        }
        if (mFloatingViewExpanded != null && mFloatingViewExpanded.getParent() != null) {
            mWindowManager.removeView(mFloatingViewExpanded);
            mFloatingViewExpanded = null;
        }
        if (mFloatingViewFullscreen == null) {
            ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.Theme_PhoneClaw);
            mFloatingViewFullscreen = LayoutInflater.from(themedContext).inflate(R.layout.layout_floating_expanded, null);
            fullscreenParams = getFullscreenLayoutParams();
            mWindowManager.addView(mFloatingViewFullscreen, fullscreenParams);

            conversationListView = mFloatingViewFullscreen.findViewById(R.id.conversation_list_view);
            messageInput = mFloatingViewFullscreen.findViewById(R.id.message_input);
            voiceInputButton = mFloatingViewFullscreen.findViewById(R.id.voice_input_button);
            sendButton = mFloatingViewFullscreen.findViewById(R.id.send_button);
            newSessionButton = mFloatingViewFullscreen.findViewById(R.id.new_session_button);
            collapseButton = mFloatingViewFullscreen.findViewById(R.id.collapse_button);
            ImageButton screenshotButton = mFloatingViewFullscreen.findViewById(R.id.screenshot_button);
            ImageButton exportButton = mFloatingViewFullscreen.findViewById(R.id.export_button);

            conversationListView.setAdapter(messageAdapter);
            
            sessionListView = mFloatingViewFullscreen.findViewById(R.id.session_list_view);
            sessionListView.setAdapter(sessionAdapter);
            updateSessionList();
            setupSessionListView();
            
            modelListView = mFloatingViewFullscreen.findViewById(R.id.model_list_view);
            setupModelListView();
            
            modelSpinner = mFloatingViewFullscreen.findViewById(R.id.model_spinner);
            updateChatModelSpinner();
            
            setupTabs();

            // 全屏状态下的叉按钮恢复到极小
            collapseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCollapsedView();
                }
            });

            // 全屏状态下的标题栏双击缩小
            // 将整个标题区域设为可点击
            View headerContainer = mFloatingViewFullscreen.findViewById(R.id.floating_expanded_root);
            if (headerContainer != null) {
                // 找到标题行的LinearLayout
                LinearLayout headerRow = (LinearLayout) ((LinearLayout) headerContainer).getChildAt(1);
                if (headerRow != null) {
                    final long[] lastClickTime = {0};
                    headerRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastClickTime[0] < 300) {
                                // 双击缩小
                                showCollapsedView();
                            }
                            lastClickTime[0] = currentTime;
                        }
                    });
                }
            }

            // 截图按钮 - 短按抓取当前屏幕内容，长按调用GetScreenTool
            setupScreenshotButton(screenshotButton);

            // 导出按钮 - 导出对话为Markdown并分享
            exportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportChatToMarkdown();
                }
            });
        }
    }

    private static final Set<String> IGNORED_PACKAGES = new HashSet<String>() {{
        add("com.android.systemui");
        add("com.android.launcher");
        add("com.android.launcher3");
    }};

    private static final String SCREENSHOT_TAG = "PhoneClawScreen";

    private void setupScreenshotButton(ImageButton screenshotButton) {
        final long[] longClickTime = {0};
        final boolean[] isLongClick = {false};

        screenshotButton.setOnClickListener(v -> {
            if (!isLongClick[0]) {
                // 短按 - 抓取当前屏幕内容，以无障碍节点形式输出到日志
                captureScreenContent();
            }
            isLongClick[0] = false;
        });

        screenshotButton.setOnLongClickListener(v -> {
            isLongClick[0] = true;
            // 长按 - 调用GetScreenTool
            captureScreenWithTool();
            return true;
        });
    }

    private void captureScreenContent() {
        SelectToSpeakService service = SelectToSpeakService.getService();
        if (service == null) {
            Log.e(SCREENSHOT_TAG, "Accessibility service not available");
            return;
        }

        AccessibilityNodeInfo rootNode = service.getRootWindowNode();
        if (rootNode == null) {
            Log.e(SCREENSHOT_TAG, "Cannot get window content");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Screen Content (Accessibility Nodes) ===\n");
            buildNodeTree(sb, rootNode, 0, 100);
            rootNode.recycle();
            
            // 一次性输出所有内容
            Log.i(SCREENSHOT_TAG, sb.toString());
        } catch (Exception e) {
            rootNode.recycle();
            Log.e(SCREENSHOT_TAG, "Failed to capture screen: " + e.getMessage());
        }
    }

    private void buildNodeTree(StringBuilder sb, AccessibilityNodeInfo node, int depth, int maxDepth) {
        if (node == null || depth > maxDepth) {
            return;
        }

        CharSequence packageName = node.getPackageName();
        if (packageName != null && IGNORED_PACKAGES.contains(packageName.toString())) {
            return;
        }

        // 添加缩进
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }

        // 构建节点信息
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        int lastDot = className.lastIndexOf('.');
        String simpleClassName = lastDot >= 0 ? className.substring(lastDot + 1) : className;

        sb.append("[").append(simpleClassName.isEmpty() ? "View" : simpleClassName).append("]");

        String viewId = node.getViewIdResourceName();
        if (viewId != null && !viewId.isEmpty()) {
            sb.append(" id=").append(viewId);
        }

        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            String textStr = text.toString().replace("\n", "\\n");
            if (textStr.length() > 50) {
                textStr = textStr.substring(0, 47) + "...";
            }
            sb.append(" text=\"").append(textStr).append("\"");
        }

        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.length() > 0) {
            String descStr = contentDesc.toString().replace("\n", "\\n");
            if (descStr.length() > 50) {
                descStr = descStr.substring(0, 47) + "...";
            }
            sb.append(" desc=\"").append(descStr).append("\"");
        }

        if (node.isClickable()) {
            sb.append(" [clickable]");
        }
        if (node.isFocusable()) {
            sb.append(" [focusable]");
        }
        if (!node.isEnabled()) {
            sb.append(" [disabled]");
        }
        if (node.isCheckable()) {
            sb.append(" [checkable");
            if (node.isChecked()) {
                sb.append("=checked]");
            } else {
                sb.append("]");
            }
        }
        if (node.isScrollable()) {
            sb.append(" [scrollable]");
        }
        if (node.isEditable()) {
            sb.append(" [editable]");
        }

        android.graphics.Rect bounds = new android.graphics.Rect();
        node.getBoundsInScreen(bounds);
        sb.append(" bounds=[").append(bounds.left).append(",").append(bounds.top)
          .append(" ").append(bounds.right).append(",").append(bounds.bottom).append("]");

        sb.append("\n");

        // 递归处理子节点
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                buildNodeTree(sb, child, depth + 1, maxDepth);
                child.recycle();
            }
        }
    }

    private void captureScreenWithTool() {
        SelectToSpeakService service = SelectToSpeakService.getService();
        if (service == null) {
            Log.e(SCREENSHOT_TAG, "Accessibility service not available");
            return;
        }

        AccessibilityNodeInfo rootNode = service.getRootWindowNode();
        if (rootNode == null) {
            Log.e(SCREENSHOT_TAG, "Cannot get window content");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Screen Content (GetScreenTool JSON) ===\n");
            JSONObject result = buildScreenJson(rootNode, 0, 100, false);
            rootNode.recycle();
            
            // 格式化JSON输出
            String jsonStr = result.toString(2);
            sb.append(jsonStr);
            
            // 一次性输出所有内容
            Log.i(SCREENSHOT_TAG, sb.toString());
        } catch (Exception e) {
            rootNode.recycle();
            Log.e(SCREENSHOT_TAG, "Failed to capture screen: " + e.getMessage());
        }
    }

    private JSONObject buildScreenJson(AccessibilityNodeInfo node, int depth, int maxDepth, boolean includeInvisible) {
        if (node == null || depth > maxDepth) {
            return null;
        }

        if (!includeInvisible && !node.isVisibleToUser()) {
            return null;
        }

        CharSequence packageName = node.getPackageName();
        if (packageName != null && IGNORED_PACKAGES.contains(packageName.toString())) {
            return null;
        }

        JSONObject element = new JSONObject();
        
        try {
            String className = node.getClassName() != null ? node.getClassName().toString() : "";
            int lastDot = className.lastIndexOf('.');
            String simpleClassName = lastDot >= 0 ? className.substring(lastDot + 1) : className;
            element.put("type", simpleClassName.isEmpty() ? "View" : simpleClassName);

            String viewId = node.getViewIdResourceName();
            if (viewId != null && !viewId.isEmpty()) {
                element.put("id", viewId);
            }


            CharSequence text = node.getText();
            if (text != null && text.length() > 0) {
                element.put("text", text.toString());
            }

            CharSequence contentDesc = node.getContentDescription();
            if (contentDesc != null && contentDesc.length() > 0) {
                element.put("desc", contentDesc.toString());
            }

            if (node.isClickable()) {
                element.put("clickable", true);
            }

            if (node.isFocusable()) {
                element.put("focusable", true);
            }

            if (!node.isEnabled()) {
                element.put("enabled", false);
            }

            if (node.isScrollable()) {
                element.put("scrollable", true);
            }

            if (node.isEditable()) {
                element.put("editable", true);
            }

            if (node.isCheckable()) {
                element.put("checkable", true);
                element.put("checked", node.isChecked());
            }

            android.graphics.Rect bounds = new android.graphics.Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.width() > 0 && bounds.height() > 0) {
                JSONObject boundsJson = new JSONObject();
                boundsJson.put("x", bounds.left);
                boundsJson.put("y", bounds.top);
                boundsJson.put("w", bounds.width());
                boundsJson.put("h", bounds.height());
                element.put("bounds", boundsJson);
            }

            int childCount = node.getChildCount();
            if (childCount > 0) {
                JSONArray children = new JSONArray();
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) {
                        JSONObject childJson = buildScreenJson(child, depth + 1, maxDepth, includeInvisible);
                        if (childJson != null && childJson.length() > 0) {
                            children.put(childJson);
                        }
                        child.recycle();
                    }
                }
                if (children.length() > 0) {
                    element.put("children", children);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return element;
    }

    private void exportChatToMarkdown() {
        if (activeSession == null) {
            Toast.makeText(this, "No active session to export", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SessionManager.MessageRecord> messages = activeSession.getMessages();
        if (messages.isEmpty()) {
            Toast.makeText(this, "No messages to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 构建Markdown内容
            StringBuilder md = new StringBuilder();
            
            // 添加标题
            String title = activeSession.getTitle();
            if (title == null || title.isEmpty()) {
                title = "Chat Export";
            }
            md.append("# ").append(title).append("\n\n");
            
            // 添加导出时间
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            md.append("> Exported at: ").append(dateFormat.format(new Date())).append("\n\n");
            md.append("---\n\n");

            // 遍历消息
            for (SessionManager.MessageRecord record : messages) {
                switch (record.role) {
                    case USER:
                        md.append("## 🧑 User\n\n");
                        md.append(escapeMarkdown(record.content)).append("\n\n");
                        break;

                    case AI:
                        md.append("## 🤖 Assistant\n\n");
                        md.append(escapeMarkdown(record.content)).append("\n\n");
                        break;

                    case AI_THINKING:
                        md.append("## 🤖 Assistant (Thinking)\n\n");
                        md.append("> ").append(escapeMarkdown(record.content).replace("\n", "\n> ")).append("\n\n");
                        break;

                    case TOOL_CALL:
                        md.append("## 🔧 Tool Call\n\n");
                        if (record.toolName != null) {
                            md.append("**Tool:** `").append(record.toolName).append("`\n\n");
                        }
                        if (record.toolParams != null && !record.toolParams.isEmpty()) {
                            md.append("**Parameters:**\n```json\n");
                            try {
                                JSONObject params = new JSONObject(record.toolParams);
                                md.append(params.toString(2));
                            } catch (Exception e) {
                                md.append(record.toolParams);
                            }
                            md.append("\n```\n\n");
                        }
                        if (record.toolResult != null && !record.toolResult.isEmpty()) {
                            String statusIcon = record.toolState == SessionManager.ToolState.SUCCESS ? "✅" : 
                                              (record.toolState == SessionManager.ToolState.FAILED ? "❌" : "⏳");
                            md.append("**Result** ").append(statusIcon).append(":\n```\n");
                            String result = record.toolResult;
                            if (result.length() > 4000) {
                                result = result.substring(0, 4000) + "\n... (truncated)";
                            }
                            md.append(escapeCodeBlock(result)).append("\n```\n\n");
                        }
                        break;

                    case ERROR:
                        md.append("## ❌ Error\n\n");
                        md.append(escapeMarkdown(record.content)).append("\n\n");
                        break;
                }
            }

            // 添加页脚
            md.append("---\n\n");
            md.append("*Exported from PhoneClaw*\n");

            // 保存文件
            String fileName = "chat_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".md";
            java.io.File exportsDir = new java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            java.io.File file = new java.io.File(exportsDir, fileName);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(md.toString());
            writer.close();

            // 获取文件URI
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            // 创建分享Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/markdown");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            Intent chooser = Intent.createChooser(shareIntent, "Export Chat");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(chooser);
            
            Toast.makeText(this, "Chat exported successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to export: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        // 简单的Markdown转义，保留可读性
        return text;
    }

    private String escapeCodeBlock(String text) {
        if (text == null) return "";
        // 移除可能破坏代码块的字符
        return text.replace("```", "` ` `");
    }

    private void createNewSession() {
        String modelId = getSelectedModelId();
        createNewSessionWithModel(modelId);
    }

    private void updateSessionList() {
        sessionList.clear();
        sessionList.addAll(SessionManager.getInstance().getSessionList());
        if (sessionAdapter != null) {
            sessionAdapter.notifyDataSetChanged();
        }
    }

    private void sendMessage(String content) {
        if (activeSession == null) {
            createNewSession();
        }
        updateStatusText("Thinking...");
        activeSession.sendMessage(content);
    }

    private void updateStatusText(String status) {
        if (floatingStatusText != null) {
            mainHandler.post(() -> floatingStatusText.setText(status));
        }
    }

    private void setSendButtonRunning(boolean running) {
        if (sendButton != null) {
            mainHandler.post(() -> {
                if (running) {
                    sendButton.setText("停止");
                    sendButton.setBackgroundResource(R.drawable.stop_button_bg);
                } else {
                    sendButton.setText("发送");
                    sendButton.setBackgroundResource(R.drawable.send_button_bg);
                }
            });
        }
    }

    public static @Nullable FloatingWindowService getInstance() {
        return instance;
    }

    private void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    private void scrollToBottom() {
        if (conversationListView != null && messageAdapter.getCount() > 0 && !userScrolled) {
            conversationListView.setSelection(messageAdapter.getCount() - 1);
        }
    }
    
    private void resetUserScroll() {
        userScrolled = false;
    }

    private WindowManager.LayoutParams getCollapsedLayoutParams() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        return params;
    }

    private WindowManager.LayoutParams getExpandedLayoutParams() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return params;
    }

    private WindowManager.LayoutParams getFullscreenLayoutParams() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.FILL;
        params.x = 0;
        params.y = 0;
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return params;
    }

    private android.widget.Spinner modelSpinner;
    
    private void setupTabs() {
        View rootView = mFloatingViewExpanded != null ? mFloatingViewExpanded : mFloatingViewFullscreen;
        if (rootView == null) return;

        tabChat = rootView.findViewById(R.id.tab_chat);
        tabSessions = rootView.findViewById(R.id.tab_sessions);
        tabModels = rootView.findViewById(R.id.tab_models);
        
        View.OnClickListener tabClickListener = v -> {
            int newTab = 0;
            if (v == tabSessions) newTab = 1;
            else if (v == tabModels) newTab = 2;
            switchTab(newTab);
        };
        
        tabChat.setOnClickListener(tabClickListener);
        tabSessions.setOnClickListener(tabClickListener);
        tabModels.setOnClickListener(tabClickListener);
        
        String savedModelId = SessionManager.getInstance().getSelectedModel();
        if (savedModelId != null) {
            restoreSelectedModel(savedModelId);
        }
        
        switchTab(0);
    }
    
    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        
        tabChat.setSelected(tabIndex == 0);
        tabSessions.setSelected(tabIndex == 1);
        tabModels.setSelected(tabIndex == 2);
        
        tabChat.setTextColor(tabIndex == 0 ? getResources().getColor(R.color.floating_accent) : getResources().getColor(R.color.floating_text_secondary));
        tabSessions.setTextColor(tabIndex == 1 ? getResources().getColor(R.color.floating_accent) : getResources().getColor(R.color.floating_text_secondary));
        tabModels.setTextColor(tabIndex == 2 ? getResources().getColor(R.color.floating_accent) : getResources().getColor(R.color.floating_text_secondary));
        
        conversationListView.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        sessionListView.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        modelListView.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        
        View rootView = mFloatingViewExpanded != null ? mFloatingViewExpanded : mFloatingViewFullscreen;
        if (rootView != null) {
            View inputContainer = rootView.findViewById(R.id.input_container);
            if (inputContainer != null) {
                inputContainer.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    private void setupModelListView() {
        List<PhoneClawDbHelper.ModelConfig> models = SessionManager.getInstance().getModelList();
        modelAdapter = new android.widget.ArrayAdapter<PhoneClawDbHelper.ModelConfig>(this, android.R.layout.simple_list_item_2, android.R.id.text1, models) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                PhoneClawDbHelper.ModelConfig model = getItem(position);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                text1.setText(model.getDisplayName() + (model.isDefault ? " *" : ""));
                text2.setText(providerLabel(model.providerType) + " | " + model.name);
                text2.setTextSize(11);
                return view;
            }
        };
        modelListView.setAdapter(modelAdapter);
        
        Button addModelBtn = new Button(this);
        addModelBtn.setText("Add Model");
        addModelBtn.setOnClickListener(v -> {
            showModelConfigDialog(null, () -> {
                modelAdapter.clear();
                modelAdapter.addAll(SessionManager.getInstance().getModelList());
                modelAdapter.notifyDataSetChanged();
                updateChatModelSpinner();
            });
        });
        modelListView.addFooterView(addModelBtn);
        
        modelListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= modelAdapter.getCount()) return;
            PhoneClawDbHelper.ModelConfig model = modelAdapter.getItem(position);
            showModelConfigDialog(model, () -> {
                modelAdapter.clear();
                modelAdapter.addAll(SessionManager.getInstance().getModelList());
                modelAdapter.notifyDataSetChanged();
                updateChatModelSpinner();
            });
        });

        modelListView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= modelAdapter.getCount()) return true;
            PhoneClawDbHelper.ModelConfig model = modelAdapter.getItem(position);
            if (model == null) return true;
            if ("default".equals(model.id)) {
                Toast.makeText(this, "Default model cannot be deleted", Toast.LENGTH_SHORT).show();
                return true;
            }

            showConfirmDialog(
                    "Delete Model",
                    "Delete model '" + model.getDisplayName() + "'?",
                    () -> {
                        SessionManager.getInstance().deleteModel(model.id);
                        modelAdapter.clear();
                        modelAdapter.addAll(SessionManager.getInstance().getModelList());
                        modelAdapter.notifyDataSetChanged();
                        updateChatModelSpinner();
                    }
            );
            return true;
        });
    }

    private void updateChatModelSpinner() {
        if (modelSpinner == null) return;
        List<PhoneClawDbHelper.ModelConfig> models = SessionManager.getInstance().getModelList();
        android.widget.ArrayAdapter<PhoneClawDbHelper.ModelConfig> adapter = new android.widget.ArrayAdapter<PhoneClawDbHelper.ModelConfig>(this, android.R.layout.simple_spinner_item, models) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                PhoneClawDbHelper.ModelConfig model = getItem(position);
                String displayName = model.getDisplayName();
                if (displayName.length() > 12) {
                    displayName = displayName.substring(0, 10) + "..";
                }
                view.setText(displayName);
                view.setTextSize(12);
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                PhoneClawDbHelper.ModelConfig model = getItem(position);
                view.setText(model.getDisplayName() + (model.isDefault ? " *" : ""));
                view.setTextSize(12);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        
        modelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                PhoneClawDbHelper.ModelConfig selected = adapter.getItem(position);
                if (selected != null) {
                    SessionManager.getInstance().saveSelectedModel(selected.id);
                    if (activeSession != null && !activeSession.isRunning()) {
                        if (!selected.id.equals(activeSession.getModelId())) {
                            activeSession.updateModel(selected);
                        }
                    }
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void restoreSelectedModel(String modelId) {
        if (modelSpinner == null || modelSpinner.getAdapter() == null) return;
        android.widget.ArrayAdapter<PhoneClawDbHelper.ModelConfig> adapter = (android.widget.ArrayAdapter<PhoneClawDbHelper.ModelConfig>) modelSpinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            PhoneClawDbHelper.ModelConfig model = adapter.getItem(i);
            if (model != null && modelId.equals(model.id)) {
                modelSpinner.setSelection(i);
                break;
            }
        }
    }

    private void setupSessionListView() {
        sessionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= sessionList.size()) return;
            SessionManager.SessionInfo info = sessionList.get(position);
            activeSession = SessionManager.getInstance().loadSession(info.id);
            if (activeSession != null) {
                currentMessages.clear();
                currentMessages.addAll(activeSession.getMessages());
                messageAdapter.notifyDataSetChanged();
                
                activeSession.setListener(new SessionManager.SessionListener() {
                    @Override
                    public void onSessionStarted() {
                        runOnMain(() -> setSendButtonRunning(true));
                    }

                    @Override
                    public void onSessionTitleUpdated(String title) {
                        runOnMain(() -> updateSessionList());
                    }

                    @Override
                    public void onMessageAdded(int index) {
                        runOnMain(() -> {
                            resetUserScroll();
                            currentMessages.clear();
                            currentMessages.addAll(activeSession.getMessages());
                            messageAdapter.notifyDataSetChanged();
                            scrollToBottom();
                        });
                    }

                    @Override
                    public void onMessageUpdated(int index) {
                        runOnMain(() -> messageAdapter.notifyDataSetChanged());
                    }

                    @Override
                    public void onSessionFinished() {
                        runOnMain(() -> {
                            updateStatusText("Ready");
                            setSendButtonRunning(false);
                        });
                    }

                    @Override
                    public void onError(String reason, Exception e) {
                        runOnMain(() -> {
                            updateStatusText("Error");
                            setSendButtonRunning(false);
                            Toast.makeText(FloatingWindowService.this, reason, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
                
                if (activeSession.isRunning()) {
                    setSendButtonRunning(true);
                } else {
                    setSendButtonRunning(false);
                }
            }
            
            switchTab(0);
        });

        sessionListView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= sessionList.size()) return true;
            SessionManager.SessionInfo info = sessionList.get(position);
            showConfirmDialog(
                    "Delete Session",
                    "Delete session '" + (info.title != null ? info.title : "New Session") + "'?",
                    () -> {
                        boolean deletingActive = activeSession != null && info.id.equals(activeSession.getId());
                        SessionManager.getInstance().deleteSession(info.id);
                        updateSessionList();
                        if (deletingActive) {
                            activeSession = null;
                            currentMessages.clear();
                            messageAdapter.notifyDataSetChanged();
                            setSendButtonRunning(false);
                            updateStatusText("Ready");
                        }
                    }
            );
            return true;
        });
        
        Button newSessionBtn = new Button(this);
        newSessionBtn.setText("New Session");
        newSessionBtn.setOnClickListener(v -> {
            String modelId = getSelectedModelId();
            createNewSessionWithModel(modelId);
            switchTab(0);
        });
        sessionListView.addFooterView(newSessionBtn);
    }

    private void showConfirmDialog(String title, String message, Runnable onConfirm) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 20);

        TextView msg = new TextView(this);
        msg.setText(message);
        msg.setTextColor(getResources().getColor(R.color.floating_text_primary));
        msg.setTextSize(14);
        layout.addView(msg);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 20, 0, 0);

        Button confirmBtn = new Button(this);
        confirmBtn.setText("Delete");
        confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        btnRow.addView(confirmBtn);

        Button cancelBtn = new Button(this);
        cancelBtn.setText("Cancel");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(cancelBtn);

        layout.addView(btnRow);
        dialog.setContentView(layout);
        dialog.setTitle(title);
        dialog.show();
    }

    private String getSelectedModelId() {
        if (modelSpinner != null && modelSpinner.getSelectedItem() != null) {
            Object item = modelSpinner.getSelectedItem();
            if (item instanceof PhoneClawDbHelper.ModelConfig) {
                PhoneClawDbHelper.ModelConfig config = (PhoneClawDbHelper.ModelConfig) item;
                return config.id;
            }
        }
        return null;
    }

    private String providerLabel(String providerType) {
        if (PhoneClawDbHelper.ModelConfig.PROVIDER_OPENAI_RESPONSE.equals(providerType)) {
            return "OpenAI(Response)";
        }
        return "OpenAI(Chat)";
    }

    private void showModelConfigDialog(PhoneClawDbHelper.ModelConfig existingModel, Runnable onSaved) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.setContentView(createModelConfigDialogView(existingModel, dialog, onSaved));
        dialog.setTitle(existingModel == null ? "Add Model" : "Edit Model");
        dialog.show();
    }

    private View createModelConfigDialogView(PhoneClawDbHelper.ModelConfig existingModel, android.app.Dialog dialog, Runnable onSaved) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 20);

        final EditText aliasInput = new EditText(this);
        aliasInput.setHint("显示名称 (如: GPT4)");
        if (existingModel != null && existingModel.alias != null) aliasInput.setText(existingModel.alias);
        layout.addView(aliasInput);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Model Name (e.g., gpt-4o, qwen-plus)");
        if (existingModel != null) nameInput.setText(existingModel.name);
        layout.addView(nameInput);

        final android.widget.Spinner providerSpinner = new android.widget.Spinner(this);
        final String[] providerLabels = new String[]{"OpenAI(Chat)", "OpenAI(Response)"};
        final String[] providerValues = new String[]{
                PhoneClawDbHelper.ModelConfig.PROVIDER_OPENAI_CHAT,
                PhoneClawDbHelper.ModelConfig.PROVIDER_OPENAI_RESPONSE
        };
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                providerLabels
        );
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        providerSpinner.setAdapter(providerAdapter);
        String currentProvider = existingModel != null && existingModel.providerType != null
                ? existingModel.providerType
                : PhoneClawDbHelper.ModelConfig.PROVIDER_OPENAI_CHAT;
        providerSpinner.setSelection(
                PhoneClawDbHelper.ModelConfig.PROVIDER_OPENAI_RESPONSE.equals(currentProvider) ? 1 : 0
        );
        layout.addView(providerSpinner);

        final EditText urlInput = new EditText(this);
        urlInput.setHint("Base URL");
        if (existingModel != null) urlInput.setText(existingModel.baseUrl);
        layout.addView(urlInput);

        final EditText keyInput = new EditText(this);
        keyInput.setHint("API Key");
        keyInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        if (existingModel != null) keyInput.setText(existingModel.apiKey);
        layout.addView(keyInput);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 20, 0, 0);

        Button saveBtn = new Button(this);
        saveBtn.setText("Save");
        saveBtn.setOnClickListener(v -> {
            String alias = aliasInput.getText().toString().trim();
            String name = nameInput.getText().toString().trim();
            String providerType = providerValues[providerSpinner.getSelectedItemPosition()];
            String url = urlInput.getText().toString().trim();
            String key = keyInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            PhoneClawDbHelper.ModelConfig model = new PhoneClawDbHelper.ModelConfig();
            model.id = existingModel != null ? existingModel.id : java.util.UUID.randomUUID().toString();
            model.name = name;
            model.alias = alias.isEmpty() ? name : alias;
            model.providerType = providerType;
            model.baseUrl = url;
            model.apiKey = key;
            model.isDefault = existingModel != null && existingModel.isDefault;

            SessionManager.getInstance().saveModel(model);
            Toast.makeText(this, "Model saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            onSaved.run();
        });
        btnRow.addView(saveBtn);

        Button cancelBtn = new Button(this);
        cancelBtn.setText("Cancel");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(cancelBtn);

        if (existingModel != null && !"default".equals(existingModel.id)) {
            Button deleteBtn = new Button(this);
            deleteBtn.setText("Delete");
            deleteBtn.setOnClickListener(v -> {
                SessionManager.getInstance().deleteModel(existingModel.id);
                Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                onSaved.run();
            });
            btnRow.addView(deleteBtn);
        }

        layout.addView(btnRow);
        return layout;
    }

    private void createNewSessionWithModel(String modelId) {
        activeSession = SessionManager.getInstance().createSession(modelId);
        currentMessages.clear();
        messageAdapter.notifyDataSetChanged();
        updateSessionList();
        
        activeSession.setListener(new SessionManager.SessionListener() {
            @Override
            public void onSessionStarted() {
                runOnMain(() -> setSendButtonRunning(true));
            }

            @Override
            public void onSessionTitleUpdated(String title) {
                runOnMain(() -> updateSessionList());
            }

            @Override
            public void onMessageAdded(int index) {
                runOnMain(() -> {
                    resetUserScroll();
                    currentMessages.clear();
                    currentMessages.addAll(activeSession.getMessages());
                    messageAdapter.notifyDataSetChanged();
                    scrollToBottom();
                });
            }

            @Override
            public void onMessageUpdated(int index) {
                runOnMain(() -> messageAdapter.notifyDataSetChanged());
            }

            @Override
            public void onSessionFinished() {
                runOnMain(() -> {
                    updateStatusText("Ready");
                    setSendButtonRunning(false);
                });
            }

            @Override
            public void onError(String reason, Exception e) {
                runOnMain(() -> {
                    updateStatusText("Error");
                    setSendButtonRunning(false);
                    Toast.makeText(FloatingWindowService.this, reason, Toast.LENGTH_SHORT).show();
                });
            }
        });

        if (sessionAdapter != null) {
            sessionAdapter.notifyDataSetChanged();
        }
    }

    private static class ChatMessageAdapter extends ArrayAdapter<SessionManager.MessageRecord> {
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        ChatMessageAdapter(@Nullable android.content.Context context, @Nullable List<SessionManager.MessageRecord> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, @Nullable View convertView, @Nullable android.view.ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.conversation_item, parent, false);
            }

            SessionManager.MessageRecord record = getItem(position);
            if (record == null) return view;

            TextView roleLabel = view.findViewById(R.id.message_role_label);
            TextView messageText = view.findViewById(R.id.message_text);
            TextView toolParamsText = view.findViewById(R.id.tool_params_text);
            TextView toolResultText = view.findViewById(R.id.tool_result_text);
            TextView toolStatusIcon = view.findViewById(R.id.tool_status_icon);
            ProgressBar toolProgress = view.findViewById(R.id.tool_progress);
            View toolDivider = view.findViewById(R.id.tool_divider);
            LinearLayout toolHeader = view.findViewById(R.id.tool_header);
            TextView toolNameText = view.findViewById(R.id.tool_name_text);
            ImageView expandIcon = view.findViewById(R.id.expand_icon);
            LinearLayout container = view.findViewById(R.id.message_container);

            int bgColor;
            int textColor = Color.parseColor("#FF1E293B");
            String roleText = null;

            switch (record.role) {
                case USER:
                    bgColor = Color.parseColor("#FF0EA5E9");
                    textColor = Color.WHITE;
                    roleText = "You";
                    break;
                case AI:
                    bgColor = Color.parseColor("#FFF1F5F9");
                    roleText = "AI";
                    break;
                case AI_THINKING:
                    bgColor = Color.parseColor("#FFE0F2FE");
                    roleText = "AI (thinking...)";
                    break;
                case TOOL_CALL:
                    if (record.toolState == SessionManager.ToolState.PENDING) {
                        bgColor = Color.parseColor("#FFFEF3C7");
                    } else if (record.toolState == SessionManager.ToolState.SUCCESS) {
                        bgColor = Color.parseColor("#FFD1FAE5");
                    } else {
                        bgColor = Color.parseColor("#FFFEE2E2");
                    }
                    break;
                case ERROR:
                    bgColor = Color.parseColor("#FFFEE2E2");
                    roleText = "Error";
                    break;
                default:
                    bgColor = Color.parseColor("#FFF1F5F9");
            }

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(24);
            bg.setColor(bgColor);
            container.setBackground(bg);

            final String copyContent;
            if (record.role == SessionManager.MessageRole.TOOL_CALL) {
                StringBuilder sb = new StringBuilder();
                sb.append("Tool: ").append(record.toolName != null ? record.toolName : "").append("\n");
                if (record.toolParams != null) sb.append("Params:\n").append(record.toolParams).append("\n");
                if (record.toolResult != null) sb.append("Result:\n").append(record.toolResult);
                copyContent = sb.toString();
                
                roleLabel.setVisibility(View.GONE);
                toolHeader.setVisibility(View.VISIBLE);
                toolNameText.setText("🔧 " + (record.toolName != null ? record.toolName : "Tool"));
                toolNameText.setTextColor(textColor);

                if (record.toolState == SessionManager.ToolState.PENDING) {
                    toolProgress.setVisibility(View.VISIBLE);
                    toolStatusIcon.setVisibility(View.GONE);
                } else {
                    toolProgress.setVisibility(View.GONE);
                    toolStatusIcon.setVisibility(View.VISIBLE);
                    if (record.toolState == SessionManager.ToolState.SUCCESS) {
                        toolStatusIcon.setText("✓");
                        toolStatusIcon.setTextColor(Color.parseColor("#FF059669"));
                    } else {
                        toolStatusIcon.setText("✗");
                        toolStatusIcon.setTextColor(Color.parseColor("#FFDC2626"));
                    }
                }

                if (record.toolParams != null && !record.toolParams.isEmpty()) {
                    toolParamsText.setText(record.toolParams);
                    toolParamsText.setVisibility(View.GONE);
                    expandIcon.setVisibility(View.VISIBLE);
                    expandIcon.setRotation(0);
                    
                    final boolean[] isExpanded = {false};
                    toolHeader.setOnClickListener(v -> {
                        isExpanded[0] = !isExpanded[0];
                        toolParamsText.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
                        toolResultText.setVisibility(isExpanded[0] && record.toolResult != null ? View.VISIBLE : View.GONE);
                        toolDivider.setVisibility(isExpanded[0] && record.toolResult != null ? View.VISIBLE : View.GONE);
                        expandIcon.setRotation(isExpanded[0] ? 180 : 0);
                    });
                } else {
                    toolParamsText.setVisibility(View.GONE);
                    expandIcon.setVisibility(View.GONE);
                }

                if (record.toolResult != null) {
                    toolResultText.setText(record.toolResult);
                    toolDivider.setVisibility(View.GONE);
                    toolResultText.setVisibility(View.GONE);
                } else {
                    toolDivider.setVisibility(View.GONE);
                    toolResultText.setVisibility(View.GONE);
                }

                messageText.setVisibility(View.GONE);
            } else {
                copyContent = record.content != null ? record.content : "";
                
                toolHeader.setVisibility(View.GONE);
                toolParamsText.setVisibility(View.GONE);
                toolResultText.setVisibility(View.GONE);
                toolDivider.setVisibility(View.GONE);
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(record.content);
                messageText.setTextColor(textColor);

                if (roleText != null) {
                    roleLabel.setVisibility(View.VISIBLE);
                    roleLabel.setText(roleText);
                    roleLabel.setTextColor(textColor);
                    roleLabel.setAlpha(0.7f);
                } else {
                    roleLabel.setVisibility(View.GONE);
                }
            }

            container.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("message", copyContent);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            });

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) container.getLayoutParams();
            if (record.role == SessionManager.MessageRole.USER) {
                params.gravity = Gravity.END;
            } else {
                params.gravity = Gravity.START;
            }
            container.setLayoutParams(params);

            return view;
        }
    }

    private static class SessionListAdapter extends ArrayAdapter<SessionManager.SessionInfo> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        SessionListAdapter(@Nullable android.content.Context context, @Nullable List<SessionManager.SessionInfo> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, @Nullable View convertView, @Nullable android.view.ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.session_list_item, parent, false);
            }

            SessionManager.SessionInfo info = getItem(position);
            if (info == null) return view;

            TextView titleText = view.findViewById(R.id.session_title_text);
            TextView timeText = view.findViewById(R.id.session_time_text);

            titleText.setText(info.title != null ? info.title : "New Session");
            timeText.setText(dateFormat.format(new Date(info.createdAt)));

            return view;
        }
    }
}
