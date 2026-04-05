package top.yudoge.phoneclaw.emu;

import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

public class UIWindow {
    private AccessibilityWindowInfo windowInfo;
    private String packageName;
    private String activityName;
    private UITree root;
    private List<UITree> matchedNodes;

    public AccessibilityWindowInfo getWindowInfo() { return windowInfo; }
    public void setWindowInfo(AccessibilityWindowInfo windowInfo) { this.windowInfo = windowInfo; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public UITree getRoot() { return root; }
    public void setRoot(UITree root) { this.root = root; }

    public List<UITree> getMatchedNodes() { return matchedNodes; }
    public void setMatchedNodes(List<UITree> matchedNodes) { this.matchedNodes = matchedNodes; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UIWindow {");
        sb.append("package=").append(packageName != null ? packageName : "null");
        if (activityName != null) sb.append(", activity=").append(activityName);
        
        if (matchedNodes != null && !matchedNodes.isEmpty()) {
            sb.append(", matched=").append(matchedNodes.size()).append(" nodes}\n");
            for (int i = 0; i < matchedNodes.size(); i++) {
                sb.append("  [").append(i + 1).append("] ").append(matchedNodes.get(i).toString()).append("\n");
            }
        } else if (root != null) {
            sb.append("}\n");
            sb.append(root.toStringTree());
        } else {
            sb.append("}");
        }
        
        return sb.toString();
    }
}
