package top.yudoge.phoneclaw.tools;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.core.tool.ToolParamDefinition;
import top.yudoge.hanai.core.tool.Type;
import top.yudoge.phoneclaw.SelectToSpeakService;

public class GetByTextTool implements Tool {

    private static final Set<String> IGNORED_PACKAGES = new HashSet<String>() {{
        add("com.android.systemui");
        add("com.android.launcher");
        add("com.android.launcher3");
    }};

    public GetByTextTool() {}

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .identifier("get_by_text")
                .description("Find UI elements by text content and return them with surrounding context. Useful for locating buttons, labels, or any visible text on screen.")
                .params(java.util.Arrays.asList(
                        ToolParamDefinition.builder()
                                .name("text")
                                .description("The text to search for (partial match supported)")
                                .type(Type.String)
                                .required(true)
                                .build(),
                        ToolParamDefinition.builder()
                                .name("include_siblings")
                                .description("Whether to include sibling nodes for context (default true)")
                                .type(Type.Bool)
                                .required(false)
                                .build(),
                        ToolParamDefinition.builder()
                                .name("include_parent")
                                .description("Whether to include parent node for context (default true)")
                                .type(Type.Bool)
                                .required(false)
                                .build()
                ))
                .build();
    }

    @Override
    public ToolCallResult invoke(ToolCall toolCall) {
        String searchText = toolCall.getParam("text") != null ? toolCall.getParam("text").toString() : null;
        if (searchText == null || searchText.isEmpty()) {
            return ToolCallResult.error("Text parameter is required");
        }

        boolean includeSiblings = true;
        Object siblingsParam = toolCall.getParam("include_siblings");
        if (siblingsParam != null) {
            includeSiblings = Boolean.parseBoolean(siblingsParam.toString());
        }

        boolean includeParent = true;
        Object parentParam = toolCall.getParam("include_parent");
        if (parentParam != null) {
            includeParent = Boolean.parseBoolean(parentParam.toString());
        }

        SelectToSpeakService service = SelectToSpeakService.getService();
        if (service == null) {
            return ToolCallResult.error("Accessibility service not available. Please enable it in settings.");
        }

        AccessibilityNodeInfo rootNode = service.getRootWindowNode();
        if (rootNode == null) {
            return ToolCallResult.error("Cannot get window content. Make sure accessibility service is enabled.");
        }

        try {
            List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
            findNodesByText(rootNode, searchText.toLowerCase(), foundNodes);
            
            if (foundNodes.isEmpty()) {
                rootNode.recycle();
                return ToolCallResult.ok("{\"found\": false, \"message\": \"No elements found with text: " + searchText + "\"}");
            }

            JSONArray results = new JSONArray();
            for (AccessibilityNodeInfo node : foundNodes) {
                JSONObject nodeResult = buildNodeWithContext(node, includeSiblings, includeParent);
                if (nodeResult != null) {
                    results.put(nodeResult);
                }
            }

            // 回收所有找到的节点
            for (AccessibilityNodeInfo node : foundNodes) {
                node.recycle();
            }
            rootNode.recycle();

            JSONObject response = new JSONObject();
            response.put("found", true);
            response.put("count", foundNodes.size());
            response.put("matches", results);
            
            return ToolCallResult.ok(response.toString());
        } catch (Exception e) {
            rootNode.recycle();
            return ToolCallResult.error("Failed to search for text: " + e.getMessage());
        }
    }

    private void findNodesByText(AccessibilityNodeInfo node, String searchText, List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        CharSequence packageName = node.getPackageName();
        if (packageName != null && IGNORED_PACKAGES.contains(packageName.toString())) {
            return;
        }

        // 检查文本
        CharSequence text = node.getText();
        if (text != null && text.toString().toLowerCase().contains(searchText)) {
            results.add(AccessibilityNodeInfo.obtain(node));
        }

        // 检查内容描述
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.toString().toLowerCase().contains(searchText)) {
            // 避免重复添加（如果text也匹配了）
            boolean alreadyAdded = false;
            for (AccessibilityNodeInfo existing : results) {
                if (existing.equals(node)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                results.add(AccessibilityNodeInfo.obtain(node));
            }
        }

        // 递归搜索子节点
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesByText(child, searchText, results);
                child.recycle();
            }
        }
    }

    private JSONObject buildNodeWithContext(AccessibilityNodeInfo node, boolean includeSiblings, boolean includeParent) {
        try {
            JSONObject result = new JSONObject();
            
            // 目标节点信息
            result.put("target", buildNodeJson(node));
            
            // 父节点上下文
            if (includeParent) {
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null) {
                    JSONObject parentJson = buildNodeJson(parent);
                    if (parentJson != null) {
                        result.put("parent", parentJson);
                    }
                    parent.recycle();
                }
            }
            
            // 兄弟节点上下文
            if (includeSiblings) {
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null) {
                    JSONArray siblings = new JSONArray();
                    int siblingCount = parent.getChildCount();
                    int nodeIndex = -1;
                    
                    // 找到当前节点在父节点中的索引
                    for (int i = 0; i < siblingCount; i++) {
                        AccessibilityNodeInfo child = parent.getChild(i);
                        if (child != null) {
                            if (child.equals(node)) {
                                nodeIndex = i;
                            }
                            child.recycle();
                        }
                    }
                    
                    // 添加前后相邻的兄弟节点（最多各取2个）
                    int start = Math.max(0, nodeIndex - 2);
                    int end = Math.min(siblingCount - 1, nodeIndex + 2);
                    
                    for (int i = start; i <= end; i++) {
                        if (i == nodeIndex) continue; // 跳过目标节点本身
                        AccessibilityNodeInfo sibling = parent.getChild(i);
                        if (sibling != null) {
                            JSONObject siblingJson = buildNodeJson(sibling);
                            if (siblingJson != null) {
                                siblingJson.put("position", i < nodeIndex ? "before" : "after");
                                siblings.put(siblingJson);
                            }
                            sibling.recycle();
                        }
                    }
                    
                    if (siblings.length() > 0) {
                        result.put("siblings", siblings);
                    }
                    
                    parent.recycle();
                }
            }
            
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject buildNodeJson(AccessibilityNodeInfo node) {
        if (node == null) return null;

        try {
            JSONObject element = new JSONObject();
            
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

            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.width() > 0 && bounds.height() > 0) {
                JSONObject boundsJson = new JSONObject();
                boundsJson.put("x", bounds.left);
                boundsJson.put("y", bounds.top);
                boundsJson.put("w", bounds.width());
                boundsJson.put("h", bounds.height());
                element.put("bounds", boundsJson);
            }

            // 添加深度路径，方便定位
            element.put("depth", getNodeDepth(node));

            return element;
        } catch (Exception e) {
            return null;
        }
    }

    private int getNodeDepth(AccessibilityNodeInfo node) {
        int depth = 0;
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            depth++;
            AccessibilityNodeInfo grandParent = parent.getParent();
            parent.recycle();
            parent = grandParent;
        }
        return depth;
    }
}
