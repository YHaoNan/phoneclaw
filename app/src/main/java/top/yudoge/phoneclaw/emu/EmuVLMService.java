package top.yudoge.phoneclaw.emu;

public class EmuVLMService {
    private static EmuVLMService instance;

    public static EmuVLMService getInstance() {
        if (instance == null) {
            instance = new EmuVLMService();
        }
        return instance;
    }

    public UIWindow analyzeScreen(String hintPrompt) {
        throw new UnsupportedOperationException("VLM service not implemented");
    }
}
