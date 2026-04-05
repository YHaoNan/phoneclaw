package top.yudoge.phoneclaw.emu;

import android.graphics.Rect;
import java.util.List;

public class UITree {
    private String id;
    private String className;
    private String text;
    private String desc;
    private String hintText;
    private Rect bounds;
    private boolean clickable;
    private boolean longClickable;
    private boolean scrollable;
    private boolean editable;
    private boolean checkable;
    private boolean checked;
    private List<UITree> children;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public String getHintText() { return hintText; }
    public void setHintText(String hintText) { this.hintText = hintText; }

    public Rect getBounds() { return bounds; }
    public void setBounds(Rect bounds) { this.bounds = bounds; }

    public boolean isClickable() { return clickable; }
    public void setClickable(boolean clickable) { this.clickable = clickable; }

    public boolean isLongClickable() { return longClickable; }
    public void setLongClickable(boolean longClickable) { this.longClickable = longClickable; }

    public boolean isScrollable() { return scrollable; }
    public void setScrollable(boolean scrollable) { this.scrollable = scrollable; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public boolean isCheckable() { return checkable; }
    public void setCheckable(boolean checkable) { this.checkable = checkable; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public List<UITree> getChildren() { return children; }
    public void setChildren(List<UITree> children) { this.children = children; }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int depth) {
        StringBuilder sb = new StringBuilder();
        String indent = "";
        for (int i = 0; i < depth; i++) indent += "  ";

        sb.append(indent).append("[").append(className != null ? className : "View").append("]");
        
        if (id != null) sb.append(" id=").append(id);
        if (text != null) sb.append(" text=\"").append(text).append("\"");
        if (desc != null) sb.append(" desc=\"").append(desc).append("\"");
        if (hintText != null) sb.append(" hint=\"").append(hintText).append("\"");
        
        if (clickable) sb.append(" [clickable]");
        if (longClickable) sb.append(" [longClickable]");
        if (scrollable) sb.append(" [scrollable]");
        if (editable) sb.append(" [editable]");
        if (checkable) sb.append(" [checkable").append(checked ? "=checked]" : "]");
        
        if (bounds != null) {
            sb.append(" bounds=[").append(bounds.left).append(",").append(bounds.top)
              .append(" ").append(bounds.right).append(",").append(bounds.bottom).append("]");
        }

        if (children != null && !children.isEmpty()) {
            sb.append(" children=").append(children.size());
        }

        return sb.toString();
    }

    public String toStringTree() {
        return toStringTree(0);
    }

    public String toStringTree(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(toString(depth));
        
        if (children != null && !children.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < children.size(); i++) {
                UITree child = children.get(i);
                if (i > 0) sb.append("\n");
                sb.append(child.toStringTree(depth + 1));
            }
        }
        
        return sb.toString();
    }
}
