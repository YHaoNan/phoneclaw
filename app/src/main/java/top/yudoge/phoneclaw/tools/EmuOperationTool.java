package top.yudoge.phoneclaw.tools;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.core.tool.ToolParamDefinition;
import top.yudoge.hanai.core.tool.Type;
import top.yudoge.phoneclaw.SelectToSpeakService;

public class EmuOperationTool implements Tool {

    private static final int DEFAULT_WAIT_TIMEOUT = 5000;

    public EmuOperationTool() {}

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .identifier("emu_operation")
                .description("Execute UI operations. IMPORTANT: Always prefer click_id(view_id) over click(x,y) when you can obtain and distinguish unique view IDs. Only use coordinate-based click(x,y)/long_click(x,y) as a fallback when view IDs are not available or not unique. Use get_screen first to find view IDs and bounds. Operations: click_id(view_id) - PREFERRED, click(x,y) - FALLBACK ONLY, long_click(x,y) - FALLBACK ONLY, swipe(x1,y1,x2,y2,duration), scroll_up, scroll_down, wait_for_id(view_id, timeout_ms), wait(ms), input_text(text), press_back, press_home.")
                .params(Arrays.asList(
                        ToolParamDefinition.builder()
                                .name("operations")
                                .description("JSON array of operations. Example: [{\"action\":\"click_id\",\"view_id\":\"com.app:id/button\"}] or [{\"action\":\"click\",\"x\":100,\"y\":200}]")
                                .type(Type.String)
                                .required(true)
                                .build()
                ))
                .build();
    }

    @Override
    public ToolCallResult invoke(ToolCall toolCall) {
        String operationsJson = (String) toolCall.getParam("operations");

        if (operationsJson == null || operationsJson.isEmpty()) {
            return ToolCallResult.error("operations is required");
        }

        SelectToSpeakService service = SelectToSpeakService.getService();
        if (service == null) {
            return ToolCallResult.error("Accessibility service not available. Please enable it in settings.");
        }

        try {
            JSONArray operations = new JSONArray(operationsJson);
            List<String> results = new ArrayList<>();

            for (int i = 0; i < operations.length(); i++) {
                JSONObject op = operations.getJSONObject(i);
                String action = op.optString("action", "");
                String result = executeOperation(service, action, op);
                results.add(result);
                
                if (result.startsWith("ERROR")) {
                    JSONObject finalResult = new JSONObject();
                    finalResult.put("success", false);
                    finalResult.put("step", i + 1);
                    finalResult.put("error", result);
                    finalResult.put("completed_steps", new JSONArray(results.subList(0, i)));
                    return ToolCallResult.ok(finalResult.toString());
                }
            }

            JSONObject finalResult = new JSONObject();
            finalResult.put("success", true);
            finalResult.put("results", new JSONArray(results));
            return ToolCallResult.ok(finalResult.toString());

        } catch (Exception e) {
            return ToolCallResult.error("Failed to execute operations: " + e.getMessage());
        }
    }

    private String executeOperation(SelectToSpeakService service, String action, JSONObject op) {
        try {
            switch (action) {
                case "click_id":
                    return executeClickById(service, op);
                case "click":
                    return executeClick(service, op);
                case "long_click":
                    return executeLongClick(service, op);
                case "swipe":
                    return executeSwipe(service, op);
                case "scroll_up":
                    return executeScroll(service, true);
                case "scroll_down":
                    return executeScroll(service, false);
                case "wait_for_id":
                    return executeWaitForId(service, op);
                case "wait":
                    return executeWait(op);
                case "input_text":
                    return executeInputText(service, op);
                case "press_back":
                    return executePressBack(service);
                case "press_home":
                    return executePressHome(service);
                default:
                    return "ERROR: Unknown action: " + action;
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String executeClickById(SelectToSpeakService service, JSONObject op) {
        String viewId = op.optString("view_id", "");
        if (viewId.isEmpty()) {
            return "ERROR: view_id is required";
        }

        AccessibilityNodeInfo node = findNodeById(service.getRootWindowNode(), viewId);
        if (node == null) {
            return "ERROR: View not found: " + viewId;
        }

        boolean success = performClick(node);
        node.recycle();

        return success ? "OK: Clicked view: " + viewId : "ERROR: Click failed for view: " + viewId;
    }

    private String executeClick(SelectToSpeakService service, JSONObject op) {
        int x = op.optInt("x", -1);
        int y = op.optInt("y", -1);
        
        if (x < 0 || y < 0) {
            return "ERROR: Invalid coordinates for click. x and y must be non-negative";
        }

        boolean success = service.performClickAtPosition(x, y);
        return success ? "OK: Clicked at (" + x + ", " + y + ") " : "ERROR: Click failed";
    }

    private String executeLongClick(SelectToSpeakService service, JSONObject op) {
        int x = op.optInt("x", -1);
        int y = op.optInt("y", -1);

        if (x < 0 || y < 0) {
            return "ERROR: Invalid coordinates for long_click. x and y must be non-negative";
        }

        boolean success = service.performLongClickAtPosition(x, y);
        return success ? "OK: Long clicked at (" + x + ", " + y + ") " : "ERROR: Long click failed";
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;

        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (performClick(child)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }

        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            if (parent.isClickable()) {
                boolean success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                parent.recycle();
                return success;
            }
            AccessibilityNodeInfo grandParent = parent.getParent();
            parent.recycle();
            parent = grandParent;
        }

        return false;
    }

    private String executeSwipe(SelectToSpeakService service, JSONObject op) {
        int x1 = op.optInt("x1", -1);
        int y1 = op.optInt("y1", -1);
        int x2 = op.optInt("x2", -1);
        int y2 = op.optInt("y2", -1);

        if (x1 < 0 || y1 < 0 || x2 < 0 || y2 < 0) {
            return "ERROR: Invalid coordinates for swipe";
        }

        int duration = op.optInt("duration", 300);
        boolean success = service.performSwipeGesture(x1, y1, x2, y2, duration);
        return success ? "OK: Swiped from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")" : "ERROR: Swipe failed";
    }

    private String executeScroll(SelectToSpeakService service, boolean up) {
        AccessibilityNodeInfo rootNode = service.getRootWindowNode();
        if (rootNode == null) {
            return "ERROR: Cannot get root node";
        }

        AccessibilityNodeInfo scrollable = findScrollableNode(rootNode);
        if (scrollable == null) {
            rootNode.recycle();
            return "ERROR: No scrollable node found";
        }

        int action = up ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
        boolean success = scrollable.performAction(action);
        scrollable.recycle();
        rootNode.recycle();

        return success ? "OK: Scrolled " + (up ? "up" : "down") : "ERROR: Scroll failed";
    }

    private String executeWaitForId(SelectToSpeakService service, JSONObject op) {
        String viewId = op.optString("view_id", "");
        int timeout = op.optInt("timeout", DEFAULT_WAIT_TIMEOUT);

        if (viewId.isEmpty()) {
            return "ERROR: view_id is required";
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            AccessibilityNodeInfo node = findNodeById(service.getRootWindowNode(), viewId);
            if (node != null) {
                node.recycle();
                return "OK: Found view: " + viewId;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                return "ERROR: Wait interrupted";
            }
        }

        return "ERROR: Timeout waiting for view: " + viewId;
    }

    private String executeWait(JSONObject op) {
        int ms = op.optInt("ms", 1000);
        try {
            Thread.sleep(ms);
            return "OK: Waited " + ms + "ms";
        } catch (InterruptedException e) {
            return "ERROR: Wait interrupted";
        }
    }

    private String executeInputText(SelectToSpeakService service, JSONObject op) {
        String text = op.optString("text", "");
        if (text.isEmpty()) {
            return "ERROR: text is required";
        }

        AccessibilityNodeInfo focusNode = service.getRootInActiveWindow();
        if (focusNode == null) {
            return "ERROR: Cannot get focused node";
        }

        AccessibilityNodeInfo editableNode = findFocusedEditableNode(focusNode);
        if (editableNode == null) {
            focusNode.recycle();
            return "ERROR: No focused editable node found";
        }

        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean success = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        editableNode.recycle();
        focusNode.recycle();

        return success ? "OK: Input text: " + text : "ERROR: Input text failed";
    }

    private String executePressBack(SelectToSpeakService service) {
        boolean success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
        return success ? "OK: Pressed back" : "ERROR: Press back failed";
    }

    private String executePressHome(SelectToSpeakService service) {
        boolean success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
        return success ? "OK: Pressed home" : "ERROR: Press home failed";
    }

    private AccessibilityNodeInfo findNodeById(AccessibilityNodeInfo node, String viewId) {
        if (node == null) return null;

        String nodeId = node.getViewIdResourceName();
        if (nodeId != null && nodeId.equals(viewId)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findNodeById(child, viewId);
            if (found != null) {
                return found;
            }
            if (child != null && found == null) {
                child.recycle();
            }
        }

        return null;
    }

    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (node.isScrollable()) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findScrollableNode(child);
            if (found != null) {
                return found;
            }
            if (child != null) child.recycle();
        }

        return null;
    }

    private AccessibilityNodeInfo findFocusedEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (node.isEditable() && node.isFocused()) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findFocusedEditableNode(child);
            if (found != null) {
                return found;
            }
            if (child != null) child.recycle();
        }

        return null;
    }
}
