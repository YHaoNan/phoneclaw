package top.yudoge.phoneclaw.tools;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.core.tool.ToolParamDefinition;
import top.yudoge.hanai.core.tool.Type;

public class OpenIntentTool implements Tool {

    private final Context context;

    public OpenIntentTool(Context context) {
        this.context = context;
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .identifier("open_intent")
                .description("Open an app by package name or scheme. Supported apps: WeChat(com.tencent.mm), TikTok/Douyin(com.ss.android.ugc.aweme), QQ(com.tencent.mobileqq), etc.")
                .params(Arrays.asList(
                        ToolParamDefinition.builder()
                                .name("package_name")
                                .description("Package name of the app to open, e.g., 'com.ss.android.ugc.aweme' for Douyin/TikTok, 'com.tencent.mm' for WeChat")
                                .type(Type.String)
                                .required(true)
                                .build()
                ))
                .build();
    }

    @Override
    public ToolCallResult invoke(ToolCall toolCall) {
        String packageName = (String) toolCall.getParam("package_name");

        if (packageName == null || packageName.isEmpty()) {
            return ToolCallResult.error("package_name is required");
        }

        try {
            if (!isAppInstalled(packageName)) {
                return ToolCallResult.error("App not installed: " + packageName);
            }

            Intent intent = null;

            if ("com.ss.android.ugc.aweme".equals(packageName)) {
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("snssdk1128://"));
            } else {
                intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            }

            if (intent == null) {
                return ToolCallResult.error("Cannot create intent for: " + packageName);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("package_name", packageName);
            result.put("message", "App opened successfully");
            return ToolCallResult.ok(result.toString());

        } catch (ActivityNotFoundException e) {
            return ToolCallResult.error("Activity not found for: " + packageName);
        } catch (Exception e) {
            return ToolCallResult.error("Failed to open app: " + e.getMessage());
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
