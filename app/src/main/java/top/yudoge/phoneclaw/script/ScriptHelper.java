package top.yudoge.phoneclaw.script;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

import top.yudoge.phoneclaw.SelectToSpeakService;

/**
 * Helper class for Groovy scripts to interact with accessibility service.
 * Usage in script:
 *   def helper = new top.yudoge.phoneclaw.script.ScriptHelper(service)
 *   helper.clickById("com.app:id/button")
 *   helper.click(100, 200)
 *   def nodes = helper.findByText("Hello")
 */
public class ScriptHelper {
    private SelectToSpeakService service;

    public ScriptHelper(SelectToSpeakService service) {
        this.service = service;
    }

    public AccessibilityNodeInfo getRootNode() {
        return service != null ? service.getRootWindowNode() : null;
    }

    public boolean clickById(String viewId) {
        if (service == null) return false;
        AccessibilityNodeInfo node = findNodeById(getRootNode(), viewId);
        if (node == null) return false;

        boolean success = performClick(node);
        node.recycle();
        return success;
    }

    public boolean click(int x, int y) {
        if (service == null) return false;
        return service.performClickAtPosition(x, y);
    }

    public boolean longClick(int x, int y) {
        if (service == null) return false;
        return service.performLongClickAtPosition(x, y);
    }

    public boolean swipe(int x1, int y1, int x2, int y2) {
        return swipe(x1, y1, x2, y2, 300);
    }

    public boolean swipe(int x1, int y1, int x2, int y2, int duration) {
        if (service == null) return false;
        return service.performSwipeGesture(x1, y1, x2, y2, duration);
    }

    public boolean scrollUp() {
        return scroll(true);
    }

    public boolean scrollDown() {
        return scroll(false);
    }

    private boolean scroll(boolean up) {
        if (service == null) return false;
        AccessibilityNodeInfo root = getRootNode();
        if (root == null) return false;

        AccessibilityNodeInfo scrollable = findScrollableNode(root);
        if (scrollable == null) {
            root.recycle();
            return false;
        }

        int action = up ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
        boolean success = scrollable.performAction(action);
        scrollable.recycle();
        root.recycle();
        return success;
    }

    public boolean inputText(String text) {
        if (service == null) return false;
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return false;

        AccessibilityNodeInfo editable = findFocusedEditable(root);
        if (editable == null) {
            root.recycle();
            return false;
        }

        android.os.Bundle args = new android.os.Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean success = editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        editable.recycle();
        root.recycle();
        return success;
    }

    public boolean pressBack() {
        if (service == null) return false;
        return service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
    }

    public boolean pressHome() {
        if (service == null) return false;
        return service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
    }

    public boolean pressRecents() {
        if (service == null) return false;
        return service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
    }

    public List<AccessibilityNodeInfo> findByText(String text) {
        List<AccessibilityNodeInfo> results = new ArrayList<>();
        AccessibilityNodeInfo root = getRootNode();
        if (root != null) {
            findNodesByText(root, text.toLowerCase(), results);
            root.recycle();
        }
        return results;
    }

    public AccessibilityNodeInfo findById(String viewId) {
        return findNodeById(getRootNode(), viewId);
    }

    public List<AccessibilityNodeInfo> findByDesc(String desc) {
        List<AccessibilityNodeInfo> results = new ArrayList<>();
        AccessibilityNodeInfo root = getRootNode();
        if (root != null) {
            findNodesByDesc(root, desc.toLowerCase(), results);
            root.recycle();
        }
        return results;
    }

    public String printNode(AccessibilityNodeInfo node) {
        if (node == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(getSimpleClassName(node)).append("]");
        if (node.getViewIdResourceName() != null) {
            sb.append(" id=").append(node.getViewIdResourceName());
        }
        if (node.getText() != null) {
            sb.append(" text=\"").append(node.getText()).append("\"");
        }
        if (node.getContentDescription() != null) {
            sb.append(" desc=\"").append(node.getContentDescription()).append("\"");
        }
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        sb.append(" bounds=").append(bounds.toShortString());
        return sb.toString();
    }

    public String printTree() {
        return printTree(10);
    }

    public String printTree(int maxDepth) {
        AccessibilityNodeInfo root = getRootNode();
        if (root == null) return "No root node";

        StringBuilder sb = new StringBuilder();
        buildTreeString(root, 0, maxDepth, sb);
        root.recycle();
        return sb.toString();
    }

    private void buildTreeString(AccessibilityNodeInfo node, int depth, int maxDepth, StringBuilder sb) {
        if (node == null || depth > maxDepth) return;

        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(printNode(node)).append("\n");

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                buildTreeString(child, depth + 1, maxDepth, sb);
                child.recycle();
            }
        }
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
            if (child != null) child.recycle();
        }

        return null;
    }

    private void findNodesByText(AccessibilityNodeInfo node, String text, List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().toLowerCase().contains(text)) {
            results.add(AccessibilityNodeInfo.obtain(node));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesByText(child, text, results);
                child.recycle();
            }
        }
    }

    private void findNodesByDesc(AccessibilityNodeInfo node, String desc, List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        CharSequence nodeDesc = node.getContentDescription();
        if (nodeDesc != null && nodeDesc.toString().toLowerCase().contains(desc)) {
            results.add(AccessibilityNodeInfo.obtain(node));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesByDesc(child, desc, results);
                child.recycle();
            }
        }
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

    private AccessibilityNodeInfo findFocusedEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (node.isEditable() && node.isFocused()) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findFocusedEditable(child);
            if (found != null) {
                return found;
            }
            if (child != null) child.recycle();
        }

        return null;
    }

    private String getSimpleClassName(AccessibilityNodeInfo node) {
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
}
