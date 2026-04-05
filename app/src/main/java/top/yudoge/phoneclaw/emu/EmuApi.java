package top.yudoge.phoneclaw.emu;

import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Unified API entry point for UI automation operations.
 * 
 * <p>This class provides a high-level interface for Android UI automation,
 * including window monitoring, screen reading, and gesture operations.
 * It coordinates between {@link EmuAccessibilityService}, {@link EmuVLMService},
 * and {@link EmuGestureService} to provide a cohesive automation experience.</p>
 * 
 * <h2>Usage in Lua Scripts</h2>
 * <p>The {@code emu} object is automatically injected into the Lua script context.
 * All methods are called using Lua's method syntax: {@code emu:methodName(args)}.</p>
 * 
 * <h2>Thread Safety</h2>
 * <p>Most methods are blocking operations that execute on the caller thread.
 * Avoid calling from the main UI thread in production code.</p>
 * 
 * @see EmuAccessibilityService
 * @see UIWindow
 * @see UITree
 */
public class EmuApi {
    private static final long POLL_INTERVAL_MS = 1000;

    /**
     * Waits for a specific window to open by polling window list.
     * 
     * <p>This method uses active polling (not event-based) to detect window changes,
     * checking every 1000ms until the target window appears or timeout occurs.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Wait for WeChat to open (package only)
     * local window = emu:waitWindowOpened("com.tencent.mm", nil, 5000)
     * 
     * -- Wait for specific activity
     * local window = emu:waitWindowOpened("com.tencent.mm", "MainActivity", 5000)
     * 
     * -- Wait for any window (not recommended)
     * local window = emu:waitWindowOpened(nil, nil, 5000)
     * }</pre>
     * 
     * @param packageName   The target window's package name (e.g., "com.tencent.mm").
     *                      If null, matches any package (use with caution).
     * @param activityName  The target activity name. If null, only package name is matched.
     * @param timeoutMS     Maximum wait time in milliseconds. Must be greater than 0.
     * @return The matched {@link AccessibilityWindowInfo}, or null if timeout or service unavailable.
     * @throws IllegalArgumentException if timeoutMS is less than or equal to 0
     */
    public AccessibilityWindowInfo waitWindowOpened(String packageName, String activityName, long timeoutMS) {
        if (timeoutMS <= 0) {
            throw new IllegalArgumentException("timeoutMS must be > 0");
        }

        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return null;

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMS) {
            AccessibilityWindowInfo window = service.findWindow(packageName, activityName);
            if (window != null) {
                return window;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    /**
     * Blocks execution for a specified duration.
     * 
     * <p>Useful for waiting for animations, network requests, or other async operations
     * to complete before proceeding with automation.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Wait 2 seconds for page load
     * emu:waitMS(2000)
     * 
     * -- Wait 500ms between operations
     * emu:clickByPos(100, 200)
     * emu:waitMS(500)
     * emu:clickByPos(300, 400)
     * }</pre>
     * 
     * @param timeoutMS Duration to wait in milliseconds. Can be 0 or negative (returns immediately).
     */
    public void waitMS(long timeoutMS) {
        try {
            Thread.sleep(timeoutMS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Gets the current screen UI structure without interaction filters.
     * 
     * <p>This is a convenience method that calls 
     * {@link #getCurrentWindowByAccessibilityService(int, String, String, boolean, boolean, boolean, boolean, boolean)}
     * with all interaction filters set to false.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Get full UI tree
     * local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
     * print(ui)
     * 
     * -- Get UI tree filtered by text
     * local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "登录")
     * local nodes = ui:getMatchedNodes()
     * }</pre>
     * 
     * @param maxDepth           Maximum traversal depth. Use 0 or negative for default (50).
     * @param windowPackageName  Target window package, or null for current active window.
     * @param filterPattern      Regex pattern to filter nodes by text/id/desc, or null for no filter.
     * @return {@link UIWindow} containing UI structure, or null if service unavailable.
     */
    public UIWindow getCurrentWindowByAccessibilityService(int maxDepth, String windowPackageName, String filterPattern) {
        return getCurrentWindowByAccessibilityService(maxDepth, windowPackageName, filterPattern, false, false, false, false, false);
    }

    /**
     * Gets the current screen UI structure with interaction capability filters.
     * 
     * <p>Retrieves UI nodes from the accessibility tree, optionally filtering by
     * text content and interaction capabilities. When filterPattern is provided,
     * returns a flat list of matching nodes; otherwise returns the full tree structure.</p>
     * 
     * <h3>Filtering Behavior:</h3>
     * <ul>
     *   <li>If filterPattern is null: Returns full UI tree as {@link UIWindow#getRoot()}</li>
     *   <li>If filterPattern is provided: Returns matched nodes as {@link UIWindow#getMatchedNodes()}</li>
     *   <li>Interaction filters are applied in AND logic (all true conditions must be met)</li>
     * </ul>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Find clickable buttons with "提交" text
     * local ui = emu:getCurrentWindowByAccessibilityService(
     *     50,              -- maxDepth
     *     nil,             -- windowPackageName (current window)
     *     "提交",          -- filterPattern
     *     true,            -- requireClickable
     *     false, false, false, false  -- other filters
     * )
     * 
     * if ui ~= nil then
     *     local nodes = ui:getMatchedNodes()
     *     if nodes ~= nil and nodes:size() > 0 then
     *         print("Found " .. nodes:size() .. " clickable '提交' buttons")
     *     end
     * end
     * }</pre>
     * 
     * @param maxDepth             Maximum traversal depth. Use 0 or negative for default (50).
     * @param windowPackageName    Target window package, or null for current active window.
     * @param filterPattern        Regex pattern to match against node's text, id, or contentDescription.
     *                             Use Java regex syntax. Pass null or empty string for no text filter.
     * @param requireClickable     If true, only include nodes where {@code isClickable() == true}.
     * @param requireLongClickable If true, only include nodes where {@code isLongClickable() == true}.
     * @param requireScrollable    If true, only include nodes where {@code isScrollable() == true}.
     * @param requireEditable      If true, only include nodes where {@code isEditable() == true}.
     * @param requireCheckable     If true, only include nodes where {@code isCheckable() == true}.
     * @return {@link UIWindow} containing UI structure or matched nodes, null if service unavailable.
     */
    public UIWindow getCurrentWindowByAccessibilityService(
            int maxDepth, 
            String windowPackageName, 
            String filterPattern,
            boolean requireClickable,
            boolean requireLongClickable,
            boolean requireScrollable,
            boolean requireEditable,
            boolean requireCheckable) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return null;

        AccessibilityNodeInfo rootNode = service.getTargetWindowRoot(windowPackageName);
        if (rootNode == null) return null;

        UIWindow uiWindow = new UIWindow();
        
        CharSequence pkgSeq = rootNode.getPackageName();
        uiWindow.setPackageName(pkgSeq != null ? pkgSeq.toString() : null);

        int effectiveDepth = maxDepth <= 0 ? 50 : maxDepth;

        if (filterPattern != null && !filterPattern.isEmpty()) {
            Pattern pattern = Pattern.compile(filterPattern);
            List<UITree> matchedNodes = service.findNodesByPatternWithFilter(
                rootNode, pattern, effectiveDepth, 0,
                requireClickable, requireLongClickable, requireScrollable, requireEditable, requireCheckable
            );
            uiWindow.setMatchedNodes(matchedNodes);
        } else {
            UITree root = service.buildUITree(rootNode, effectiveDepth, 0);
            uiWindow.setRoot(root);
        }

        rootNode.recycle();
        return uiWindow;
    }

    /**
     * Gets screen content using Visual Language Model analysis.
     * 
     * <p><b>Note:</b> This feature is not yet implemented and will always throw
     * {@link UnsupportedOperationException}.</p>
     * 
     * <h3>Planned Usage:</h3>
     * <pre>{@code
     * -- Analyze screen with natural language prompt
     * local ui = emu:getCurrentWindowByVLM("Find the login button")
     * }</pre>
     * 
     * @param hintPrompt Natural language description of what to find on screen.
     * @return Never returns normally.
     * @throws UnsupportedOperationException Always thrown as feature is not implemented.
     */
    public UIWindow getCurrentWindowByVLM(String hintPrompt) {
        return EmuVLMService.getInstance().analyzeScreen(hintPrompt);
    }

    /**
     * Performs a click on a node identified by its resource ID.
     * 
     * <p>Uses accessibility API to find and click the node. If the node itself
     * is not clickable, the method will traverse up to find a clickable ancestor.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Click by full resource ID
     * local success = emu:clickById("com.tencent.mm:id/login_button")
     * 
     * -- Check result
     * if success then
     *     print("Click successful")
     * else
     *     print("Click failed or node not found")
     * end
     * }</pre>
     * 
     * @param id The full resource ID of the target node (e.g., "com.example.app:id/button").
     * @return {@code true} if click was performed successfully, {@code false} if node not found,
     *         service unavailable, or click failed.
     */
    public Boolean clickById(String id) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;

        AccessibilityNodeInfo node = service.findNodeById(id);
        if (node == null) return false;

        boolean result = service.performClick(node);
        node.recycle();
        return result;
    }

    /**
     * Performs a tap at specified screen coordinates.
     * 
     * <p>Uses gesture injection API to simulate a touch event at the given position.
     * The tap duration is 100ms (standard tap).</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Tap at screen center
     * emu:clickByPos(540.0, 960.0)
     * 
     * -- Tap with coordinates from UI tree
     * local bounds = node:getBounds()
     * emu:clickByPos(bounds.left + bounds.width() / 2, bounds.top + bounds.height() / 2)
     * }</pre>
     * 
     * @param x X coordinate in screen pixels. Use Double for Lua number compatibility.
     * @param y Y coordinate in screen pixels. Use Double for Lua number compatibility.
     * @return {@code true} if gesture was dispatched successfully, {@code false} if
     *         service unavailable or API level below Android N (API 24).
     */
    public Boolean clickByPos(Double x, Double y) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGestureClick(x.intValue(), y.intValue(), 100);
    }

    /**
     * Performs a long press on a node identified by its resource ID.
     * 
     * <p>Simulates a long press gesture at the center of the target node.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Long press for 500ms (default)
     * emu:longClickById("com.example.app:id/item", 500)
     * 
     * -- Extra long press for context menu
     * emu:longClickById("com.example.app:id/message", 1000)
     * }</pre>
     * 
     * @param id            The full resource ID of the target node.
     * @param durationInMs  Press duration in milliseconds. Recommended: 500-1000ms.
     * @return {@code true} if gesture was performed, {@code false} if node not found or service unavailable.
     */
    public Boolean longClickById(String id, long durationInMs) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;

        AccessibilityNodeInfo node = service.findNodeById(id);
        if (node == null) return false;

        boolean result = service.performLongClick(node, durationInMs);
        node.recycle();
        return result;
    }

    /**
     * Performs a long press at specified screen coordinates.
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Long press at position
     * emu:longClickByPos(540.0, 960.0, 500)
     * }</pre>
     * 
     * @param x             X coordinate in screen pixels.
     * @param y             Y coordinate in screen pixels.
     * @param durationInMs  Press duration in milliseconds.
     * @return {@code true} if gesture dispatched, {@code false} if service unavailable or API unsupported.
     */
    public Boolean longClickByPos(Double x, Double y, long durationInMs) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGestureClick(x.intValue(), y.intValue(), durationInMs);
    }

    /**
     * Performs a swipe gesture from one point to another.
     * 
     * <p>Simulates a continuous touch moving from the start point to end point
     * over the specified duration.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Swipe up (scroll down) - typical list scroll
     * emu:swipe(540.0, 1500.0, 540.0, 500.0, 300)
     * 
     * -- Swipe left (page next)
     * emu:swipe(1000.0, 960.0, 100.0, 960.0, 300)
     * 
     * -- Slow swipe for sensitive gestures
     * emu:swipe(540.0, 1000.0, 540.0, 500.0, 1000)
     * }</pre>
     * 
     * @param fromX       Starting X coordinate in screen pixels.
     * @param fromY       Starting Y coordinate in screen pixels.
     * @param toX         Ending X coordinate in screen pixels.
     * @param toY         Ending Y coordinate in screen pixels.
     * @param durationInMS Swipe duration in milliseconds. Faster swipes (200-500ms) for scrolling,
     *                     slower swipes (500-1000ms) for precise gestures.
     * @return {@code true} if gesture dispatched, {@code false} if service unavailable or API unsupported.
     */
    public Boolean swipe(Double fromX, Double fromY, Double toX, Double toY, long durationInMS) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGestureSwipe(
                fromX.intValue(), fromY.intValue(),
                toX.intValue(), toY.intValue(),
                durationInMS
        );
    }

    /**
     * Opens an application by its package name.
     * 
     * <p>Launches the app using the system's default launch intent for the package.
     * This is equivalent to tapping the app icon in the launcher.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Open WeChat
     * emu:openApp("com.tencent.mm")
     * 
     * -- Open and wait for window
     * emu:openApp("com.tencent.mm")
     * local window = emu:waitWindowOpened("com.tencent.mm", nil, 5000)
     * }</pre>
     * 
     * @param packageName The application's package name (e.g., "com.tencent.mm" for WeChat).
     * @return {@code true} if launch intent was dispatched, {@code false} if package not found
     *         or service unavailable.
     */
    public Boolean openApp(String packageName) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.openApp(packageName);
    }

    /**
     * Simulates pressing the hardware Back button.
     * 
     * <p>Performs the global "back" navigation action. Equivalent to pressing
     * the back button or gesture navigation.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Go back
     * emu:back()
     * emu:waitMS(500)  -- Wait for animation
     * }</pre>
     * 
     * @return {@code true} if action performed, {@code false} if service unavailable.
     */
    public Boolean back() {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
    }

    /**
     * Simulates pressing the hardware Home button.
     * 
     * <p>Navigates to the home screen. Equivalent to pressing the home button
     * or using home gesture navigation.</p>
     * 
     * <h3>Lua Usage:</h3>
     * <pre>{@code
     * -- Go to home screen
     * emu:home()
     * }</pre>
     * 
     * @return {@code true} if action performed, {@code false} if service unavailable.
     */
    public Boolean home() {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
    }
}