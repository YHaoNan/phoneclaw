package top.yudoge.phoneclaw.emu;

public class EmuGestureService {
    private static EmuGestureService instance;

    public static EmuGestureService getInstance() {
        if (instance == null) {
            instance = new EmuGestureService();
        }
        return instance;
    }

    public boolean click(int x, int y) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGestureClick(x, y, 100);
    }

    public boolean longClick(int x, int y, long durationMs) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGestureClick(x, y, durationMs);
    }

    public boolean swipe(int x1, int y1, int x2, int y2, long durationMs) {
        EmuAccessibilityService service = EmuAccessibilityService.getInstance();
        if (service == null) return false;
        return service.performGestureSwipe(x1, y1, x2, y2, durationMs);
    }
}
