package top.yudoge.phoneclaw.emu;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EmuAccessibilityService extends AccessibilityService {
    private static final String TAG = "EmuAccessibilityService";
    private static EmuAccessibilityService instance;

    public static EmuAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "EmuAccessibilityService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "EmuAccessibilityService destroyed");
    }

    public AccessibilityWindowInfo findWindow(String packageName, String activityName) {
        List<AccessibilityWindowInfo> windows = getWindows();
        for (AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;

            CharSequence pkgSeq = root.getPackageName();
            String pkg = pkgSeq != null ? pkgSeq.toString() : null;

            boolean packageMatch = packageName == null || packageName.equals(pkg);
            boolean activityMatch = activityName == null;

            if (!activityMatch && root.getPackageName() != null) {
                String windowActivity = extractActivityName(root);
                activityMatch = activityName.equals(windowActivity);
            }

            if (packageMatch && activityMatch) {
                return window;
            }
            root.recycle();
        }
        return null;
    }

    private String extractActivityName(AccessibilityNodeInfo node) {
        if (node == null) return null;
        String id = node.getViewIdResourceName();
        if (id != null && id.contains("/")) {
            return id.substring(0, id.indexOf("/"));
        }
        return null;
    }

    public UITree buildUITree(AccessibilityNodeInfo node, int maxDepth, int currentDepth) {
        if (node == null) return null;
        if (maxDepth > 0 && currentDepth >= maxDepth) return null;

        UITree uiTree = new UITree();

        uiTree.setId(node.getViewIdResourceName());
        
        String className = node.getClassName() != null ? node.getClassName().toString() : null;
        if (className != null) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < className.length() - 1) {
                className = className.substring(lastDot + 1);
            }
        }
        uiTree.setClassName(className);

        uiTree.setText(node.getText() != null ? node.getText().toString() : null);
        uiTree.setDesc(node.getContentDescription() != null ? node.getContentDescription().toString() : null);
        uiTree.setHintText(node.getHintText() != null ? node.getHintText().toString() : null);

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.width() > 0 && bounds.height() > 0) {
            uiTree.setBounds(bounds);
        }

        uiTree.setClickable(node.isClickable());
        uiTree.setLongClickable(node.isLongClickable());
        uiTree.setScrollable(node.isScrollable());
        uiTree.setEditable(node.isEditable());
        uiTree.setCheckable(node.isCheckable());
        uiTree.setChecked(node.isChecked());

        int childCount = node.getChildCount();
        if (childCount > 0) {
            List<UITree> children = new ArrayList<>();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                if (childNode != null) {
                    UITree childTree = buildUITree(childNode, maxDepth, currentDepth + 1);
                    if (childTree != null) {
                        children.add(childTree);
                    }
                    childNode.recycle();
                }
            }
            if (!children.isEmpty()) {
                uiTree.setChildren(children);
            }
        }

        return uiTree;
    }

    public List<UITree> findNodesByPattern(AccessibilityNodeInfo node, Pattern pattern, int maxDepth, int currentDepth) {
        List<UITree> results = new ArrayList<>();
        if (node == null) return results;
        if (maxDepth > 0 && currentDepth >= maxDepth) return results;

        String id = node.getViewIdResourceName();
        String text = node.getText() != null ? node.getText().toString() : null;
        String desc = node.getContentDescription() != null ? node.getContentDescription().toString() : null;

        boolean matches = false;
        if (id != null && pattern.matcher(id).find()) matches = true;
        if (!matches && text != null && pattern.matcher(text).find()) matches = true;
        if (!matches && desc != null && pattern.matcher(desc).find()) matches = true;

        if (matches) {
            UITree uiTree = buildUITree(node, 0, 0);
            if (uiTree != null) {
                results.add(uiTree);
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            if (childNode != null) {
                results.addAll(findNodesByPattern(childNode, pattern, maxDepth, currentDepth + 1));
                childNode.recycle();
            }
        }

        return results;
    }

    public List<UITree> findNodesByPatternWithFilter(
            AccessibilityNodeInfo node, 
            Pattern pattern, 
            int maxDepth, 
            int currentDepth,
            boolean requireClickable,
            boolean requireLongClickable,
            boolean requireScrollable,
            boolean requireEditable,
            boolean requireCheckable) {
        List<UITree> results = new ArrayList<>();
        if (node == null) return results;
        if (maxDepth > 0 && currentDepth >= maxDepth) return results;

        String id = node.getViewIdResourceName();
        String text = node.getText() != null ? node.getText().toString() : null;
        String desc = node.getContentDescription() != null ? node.getContentDescription().toString() : null;

        boolean patternMatches = false;
        if (id != null && pattern.matcher(id).find()) patternMatches = true;
        if (!patternMatches && text != null && pattern.matcher(text).find()) patternMatches = true;
        if (!patternMatches && desc != null && pattern.matcher(desc).find()) patternMatches = true;

        boolean interactionMatches = true;
        if (requireClickable && !node.isClickable()) interactionMatches = false;
        if (requireLongClickable && !node.isLongClickable()) interactionMatches = false;
        if (requireScrollable && !node.isScrollable()) interactionMatches = false;
        if (requireEditable && !node.isEditable()) interactionMatches = false;
        if (requireCheckable && !node.isCheckable()) interactionMatches = false;

        if (patternMatches && interactionMatches) {
            UITree uiTree = buildUITree(node, 0, 0);
            if (uiTree != null) {
                results.add(uiTree);
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            if (childNode != null) {
                results.addAll(findNodesByPatternWithFilter(
                    childNode, pattern, maxDepth, currentDepth + 1,
                    requireClickable, requireLongClickable, requireScrollable, requireEditable, requireCheckable
                ));
                childNode.recycle();
            }
        }

        return results;
    }

    public AccessibilityNodeInfo findNodeById(String viewId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        return findNodeByIdRecursive(root, viewId);
    }

    private AccessibilityNodeInfo findNodeByIdRecursive(AccessibilityNodeInfo node, String viewId) {
        if (node == null) return null;

        String nodeId = node.getViewIdResourceName();
        if (nodeId != null && nodeId.equals(viewId)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findNodeByIdRecursive(child, viewId);
            if (found != null) {
                return found;
            }
            if (child != null && found == null) {
                child.recycle();
            }
        }

        return null;
    }

    public boolean performClick(AccessibilityNodeInfo node) {
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

    public boolean performLongClick(AccessibilityNodeInfo node, long durationMs) {
        if (node == null) return false;
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return performGestureClick(bounds.centerX(), bounds.centerY(), durationMs);
    }

    public boolean performGestureClick(int x, int y, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMs))
                .build();
        return dispatchGesture(gesture, null, null);
    }

    public boolean performGestureSwipe(int x1, int y1, int x2, int y2, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMs))
                .build();
        return dispatchGesture(gesture, null, null);
    }

    public AccessibilityNodeInfo getTargetWindowRoot(String packageName) {
        if (packageName == null) {
            return findTargetWindowRoot();
        }

        try {
            List<AccessibilityWindowInfo> windows = this.getWindows();

            for (int i = windows.size() - 1; i >= 0; i--) {
                AccessibilityWindowInfo window = windows.get(i);
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;

                CharSequence pkgSeq = root.getPackageName();
                String pkg = pkgSeq != null ? pkgSeq.toString() : null;

                if (packageName.equals(pkg)) {
                    return root;
                }
                root.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "getTargetWindowRoot error", e);
        }
        return null;
    }

    private AccessibilityNodeInfo findTargetWindowRoot() {
        try {
            String myPackage = this.getPackageName();
            List<AccessibilityWindowInfo> windows = this.getWindows();

            AccessibilityNodeInfo appWindowCandidate = null;

            for (int i = windows.size() - 1; i >= 0; i--) {
                AccessibilityWindowInfo window = windows.get(i);
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;

                CharSequence pkgSeq = root.getPackageName();
                String pkg = pkgSeq != null ? pkgSeq.toString() : null;

                if (pkg != null &&
                    !pkg.equals(myPackage) &&
                    !pkg.equals("com.android.systemui") &&
                    !pkg.equals("com.android.launcher") &&
                    !pkg.equals("com.android.launcher3")) {

                    if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        return root;
                    }

                    if (appWindowCandidate == null) {
                        appWindowCandidate = root;
                    } else {
                        root.recycle();
                    }
                } else {
                    root.recycle();
                }
            }

            if (appWindowCandidate != null) {
                return appWindowCandidate;
            }

            Log.d(TAG, "No target window found");
        } catch (Exception e) {
            Log.e(TAG, "findTargetWindowRoot error", e);
        }
        return null;
    }

    public boolean openApp(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open app: " + packageName, e);
        }
        return false;
    }

    public boolean inputText(AccessibilityNodeInfo node, String text) {
        if (node == null || text == null) return false;

        // Try ACTION_SET_TEXT first (API 21+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (node.isEditable()) {
                android.os.Bundle arguments = new android.os.Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                boolean success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                if (success) {
                    Log.d(TAG, "Input text via ACTION_SET_TEXT: " + text);
                    return true;
                }
            }
        }

        // Fallback: Use clipboard paste
        return inputTextViaClipboard(node, text);
    }

    public boolean inputTextById(String viewId, String text) {
        AccessibilityNodeInfo node = findNodeById(viewId);
        if (node == null) return false;

        boolean result = inputText(node, text);
        node.recycle();
        return result;
    }

    private boolean inputTextViaClipboard(AccessibilityNodeInfo node, String text) {
        try {
            // Copy text to clipboard
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("text", text);
            clipboard.setPrimaryClip(clip);

            // Focus the node
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            
            // Wait a bit for focus
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Paste
            boolean success = node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            Log.d(TAG, "Input text via clipboard paste: " + text + ", success=" + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to input text via clipboard", e);
            return false;
        }
    }

    public boolean inputTextByPos(int x, int y, String text) {
        // Click to focus
        if (!performGestureClick(x, y, 100)) {
            return false;
        }

        // Wait for focus
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        // Find the focused editable node
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        AccessibilityNodeInfo focusedNode = findFocusedEditableNode(root);
        root.recycle();

        if (focusedNode == null) {
            Log.w(TAG, "No focused editable node found at position");
            return false;
        }

        boolean result = inputText(focusedNode, text);
        focusedNode.recycle();
        return result;
    }

    private AccessibilityNodeInfo findFocusedEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (node.isEditable() && node.isFocused()) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo found = findFocusedEditableNode(child);
                if (found != null) {
                    if (child != found) {
                        child.recycle();
                    }
                    return found;
                }
                child.recycle();
            }
        }

        return null;
    }
}
