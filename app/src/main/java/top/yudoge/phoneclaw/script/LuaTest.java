package top.yudoge.phoneclaw.script;

import android.util.Log;

import top.yudoge.phoneclaw.scripts.EvalHandle;
import top.yudoge.phoneclaw.scripts.EvalListener;
import top.yudoge.phoneclaw.scripts.EvalResult;
import top.yudoge.phoneclaw.scripts.impl.LuaScriptEngine;
import top.yudoge.phoneclaw.emu.EmuApi;

public class LuaTest {

    private static final String TAG = "LuaTest";

    public static void runTests() {
        Log.i(TAG, "========== 开始 Lua 引擎测试 ==========");

        test1_PureLua();
        test2_WithInjection();
        test3_EmuApi();

        Log.i(TAG, "========== 测试完成 ==========");
    }

    private static void test1_PureLua() {
        Log.i(TAG, "--- 测试1: 纯Lua ---");
        LuaScriptEngine engine = new LuaScriptEngine(1);
        String script = "print('Hello from Lua')\nreturn 'OK'";

        EvalHandle handle = engine.newEval(script);
        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, java.util.List<String> lines) {
                for (String line : lines) {
                    Log.i(TAG, "[LOG] " + line);
                }
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                Log.i(TAG, "测试1完成: success=" + result.getSuccess() + ", result=" + result.getEvalResult() + ", error=" + result.getError());
            }
        }, 5000);
    }

    private static void test2_WithInjection() {
        Log.i(TAG, "--- 测试2: 带对象注入 ---");
        LuaScriptEngine engine = new LuaScriptEngine(1);
        String script = "print('emu = ' .. tostring(emu))\nreturn 'OK'";

        EvalHandle handle = engine.newEval(script);
        handle.inject("emu", new TestObject());

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, java.util.List<String> lines) {
                for (String line : lines) {
                    Log.i(TAG, "[LOG] " + line);
                }
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                Log.i(TAG, "测试2完成: success=" + result.getSuccess() + ", result=" + result.getEvalResult() + ", error=" + result.getError());
            }
        }, 5000);
    }

    private static void test3_EmuApi() {
        Log.i(TAG, "--- 测试3: EmuApi ---");
        LuaScriptEngine engine = new LuaScriptEngine(1);
        
        String script = "print('=== 测试EmuApi ===')\n" +
                "print('emu = ' .. tostring(emu))\n" +
                "print('调用 waitMS(1000)')\n" +
                "emu:waitMS(1000)\n" +
                "print('waitMS 返回')\n" +
                "return 'OK'";

        EvalHandle handle = engine.newEval(script);
        handle.inject("emu", new EmuApi());

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, java.util.List<String> lines) {
                for (String line : lines) {
                    Log.i(TAG, "[LOG] " + line);
                }
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                Log.i(TAG, "测试3完成: success=" + result.getSuccess() + ", result=" + result.getEvalResult() + ", error=" + result.getError());
            }
        }, 10000);
    }

    public static class TestObject {
        public String sayHello() {
            return "Hello from TestObject";
        }
    }
}
