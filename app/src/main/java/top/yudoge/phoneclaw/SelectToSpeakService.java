package top.yudoge.phoneclaw;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SelectToSpeakService extends AccessibilityService {

    private static final String TAG = "PhoneClaw";
    private static final String XHS_PACKAGE = "com.xingin.xhs";
    public static SelectToSpeakService instance = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean demoDone = false;

    public static SelectToSpeakService getService() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Accessibility service connected");
        
        handler.postDelayed(this::openXiaohongshu, 5000);
    }

    private void openXiaohongshu() {
        try {
            if (!isAppInstalled(XHS_PACKAGE)) {
                Log.d(TAG, "Xiaohongshu not installed");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("xhsdiscover://"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d(TAG, "Xiaohongshu opened");
        } catch (Exception e) {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(XHS_PACKAGE);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Log.d(TAG, "Xiaohongshu opened via launch intent");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to open Xiaohongshu", ex);
            }
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (demoDone) return;
        
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";

            if (XHS_PACKAGE.equals(packageName)) {
                demoDone = true;
                Log.i(TAG, "Xiaohongshu window opened");
                
                handler.postDelayed(() -> {
                    AccessibilityNodeInfo rootWindowNode = getRootWindowNode();
//                    if (rootWindowNode != null) {
//                        UINode uiNode = traversal(rootWindowNode);
//                        rootWindowNode.recycle();
//                        printUINodeJson(uiNode);
//                    }
                    List<AccessibilityNodeInfo> messages = rootWindowNode.findAccessibilityNodeInfosByText("消息");
                    for (AccessibilityNodeInfo message : messages) {
                        printUINodeJson(traversal(message));
                    }


                    messages.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                }, 2000);


            }
        }
    }

    private void printUINodeJson(UINode node) {
        try {
            JSONObject json = uiNodeToJson(node);
            String jsonStr = json.toString(2);
            
            Log.i(TAG, "=== XHS UI Tree JSON Start ===");
            int maxLineLength = 4000;
            for (int i = 0; i < jsonStr.length(); i += maxLineLength) {
                int end = Math.min(i + maxLineLength, jsonStr.length());
                Log.i(TAG, jsonStr.substring(i, end));
            }
            Log.i(TAG, "=== XHS UI Tree JSON End ===");
        } catch (Exception e) {
            Log.e(TAG, "Failed to print JSON", e);
        }
    }

    private JSONObject uiNodeToJson(UINode node) {
        JSONObject json = new JSONObject();
        if (node == null) return json;
        
        try {
            if (node.id != null) json.put("id", node.id);
            if (node.className != null) json.put("class", node.className);
            if (node.text != null) json.put("text", node.text);
            if (node.desc != null) json.put("desc", node.desc);
            if (node.hint != null) json.put("hint", node.hint);
            if (node.bounds != null) {
                JSONObject boundsJson = new JSONObject();
                boundsJson.put("x", node.bounds.left);
                boundsJson.put("y", node.bounds.top);
                boundsJson.put("w", node.bounds.width());
                boundsJson.put("h", node.bounds.height());
                json.put("bounds", boundsJson);
            }
            if (node.clickable) json.put("clickable", true);
            if (node.longClickable) json.put("longClickable", true);
            if (node.scrollable) json.put("scrollable", true);
            if (node.editable) json.put("editable", true);
            if (node.checkable) {
                json.put("checkable", true);
                json.put("checked", node.checked);
            }
            if (node.focusable) json.put("focusable", true);
            if (node.selected) json.put("selected", true);
            if (!node.enabled) json.put("enabled", false);
            
            if (node.children != null && !node.children.isEmpty()) {
                JSONArray childrenArray = new JSONArray();
                for (UINode child : node.children) {
                    childrenArray.put(uiNodeToJson(child));
                }
                json.put("children", childrenArray);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting UINode to JSON", e);
        }
        
        return json;
    }

    static class UINode {
        String id;
        String className;
        String text;
        String desc;
        String hint;
        Rect bounds;
        boolean clickable;
        boolean longClickable;
        boolean scrollable;
        boolean editable;
        boolean checkable;
        boolean checked;
        boolean focusable;
        boolean selected;
        boolean enabled = true;
        List<UINode> children;
    }

    private UINode traversal(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        UINode uiNode = new UINode();
        
        uiNode.id = node.getViewIdResourceName();
        uiNode.className = node.getClassName() != null ? node.getClassName().toString() : null;
        
        String fullClassName = uiNode.className != null ? uiNode.className : "";
        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fullClassName.length() - 1) {
            uiNode.className = fullClassName.substring(lastDot + 1);
        }
        
        uiNode.text = node.getText() != null ? node.getText().toString() : null;
        uiNode.desc = node.getContentDescription() != null ? node.getContentDescription().toString() : null;
        uiNode.hint = node.getHintText() != null ? node.getHintText().toString() : null;
        
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.width() > 0 && bounds.height() > 0) {
            uiNode.bounds = bounds;
        }
        
        uiNode.clickable = node.isClickable();
        uiNode.longClickable = node.isLongClickable();
        uiNode.scrollable = node.isScrollable();
        uiNode.editable = node.isEditable();
        uiNode.checkable = node.isCheckable();
        uiNode.checked = node.isChecked();
        uiNode.focusable = node.isFocusable();
        uiNode.selected = node.isSelected();
        uiNode.enabled = node.isEnabled();
        
        int childCount = node.getChildCount();
        if (childCount > 0) {
            uiNode.children = new ArrayList<>();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                if (childNode != null) {
                    UINode childUINode = traversal(childNode);
                    if (childUINode != null) {
                        uiNode.children.add(childUINode);
                    }
                    childNode.recycle();
                }
            }
        }
        
        return uiNode;
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        instance = null;
    }

    public AccessibilityNodeInfo scanWindowContent() {
        return getRootInActiveWindow();
    }

    public AccessibilityNodeInfo getRootWindowNode() {
        return findTargetWindowRoot();
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

    public boolean performClickAtPosition(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
            .build();
        return dispatchGesture(gesture, null, null);
    }

    public boolean performLongClickAtPosition(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 500))
            .build();
        return dispatchGesture(gesture, null, null);
    }

    public boolean performSwipeGesture(int x1, int y1, int x2, int y2, int duration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
            .build();
        return dispatchGesture(gesture, null, null);
    }
}
