package top.yudoge.phoneclaw.tools;

import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.core.tool.ToolParamDefinition;
import top.yudoge.hanai.core.tool.Type;
import top.yudoge.phoneclaw.emu.EmuAccessibilityService;

public class GetScreenTool implements Tool {

    private static final Set<String> IGNORED_PACKAGES = new HashSet<String>() {{
        add("com.android.systemui");
        add("com.android.launcher");
        add("com.android.launcher3");
    }};

    public GetScreenTool() {}

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .identifier("get_screen")
                .description("Get the current screen content as a simplified JSON structure. Returns UI elements with their text, description, clickable status, and bounds.")
                .params(Arrays.asList(
                        ToolParamDefinition.builder()
                                .name("max_depth")
                                .description("Maximum depth to traverse the view hierarchy (default 10)")
                                .type(Type.Integer)
                                .required(false)
                                .build(),
                        ToolParamDefinition.builder()
                                .name("include_invisible")
                                .description("Whether to include invisible elements (default false)")
                                .type(Type.Bool)
                                .required(false)
                                .build()
                ))
                .build();
    }

    @Override
    public ToolCallResult invoke(ToolCall toolCall) {
        int maxDepth = 100;
        Boolean includeInvisible = false;

        Object maxDepthParam = toolCall.getParam("max_depth");
        if (maxDepthParam != null) {
            try {
                maxDepth = Integer.parseInt(maxDepthParam.toString());
            } catch (NumberFormatException ignored) {}
        }

        Object includeInvisibleParam = toolCall.getParam("include_invisible");
        if (includeInvisibleParam != null) {
            includeInvisible = Boolean.parseBoolean(includeInvisibleParam.toString());
        }

        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) {
            return ToolCallResult.error("Accessibility service not available. Please enable it in settings.");
        }

        AccessibilityNodeInfo rootNode = service.getTargetWindowRoot(null);
        if (rootNode == null) {
            return ToolCallResult.error("Cannot get window content. Make sure accessibility service is enabled.");
        }

        try {
            JSONObject result = buildScreenJson(rootNode, 0, maxDepth, includeInvisible);
            rootNode.recycle();
            return ToolCallResult.ok(result.toString());
        } catch (Exception e) {
            rootNode.recycle();
            return ToolCallResult.error("Failed to parse screen content: " + e.getMessage());
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
}
