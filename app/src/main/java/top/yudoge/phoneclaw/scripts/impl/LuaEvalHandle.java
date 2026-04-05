package top.yudoge.phoneclaw.scripts.impl;

import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import top.yudoge.phoneclaw.scripts.EvalHandle;
import top.yudoge.phoneclaw.scripts.EvalListener;
import top.yudoge.phoneclaw.scripts.EvalResult;
import top.yudoge.phoneclaw.scripts.ScriptState;

public class LuaEvalHandle implements EvalHandle {

    private static final String TAG = "LuaScript";

    private final ExecutorService executor;
    private final Semaphore semaphore;
    private final String scriptContent;
    private final Map<String, Object> injections = new HashMap<>();
    private final AtomicReference<ScriptState> state = new AtomicReference<>(ScriptState.WaitToExecute);
    private final AtomicBoolean evaluated = new AtomicBoolean(false);
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private volatile Thread executingThread;
    private String evalId;

    public LuaEvalHandle(ExecutorService executor, Semaphore semaphore, String scriptContent) {
        if (scriptContent == null) {
            throw new IllegalArgumentException("Script content cannot be null");
        }
        this.executor = executor;
        this.semaphore = semaphore;
        this.scriptContent = scriptContent;
    }

    public void inject(String name, Object obj) {
        if (evaluated.get()) {
            throw new IllegalStateException("Cannot inject after eval() has been called");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Injection name cannot be null or empty");
        }
        injections.put(name, obj);
    }

    @Override
    public void eval(EvalListener listener, long waitToExecuteTimeout) {
        if (!evaluated.compareAndSet(false, true)) {
            throw new IllegalStateException("eval() can only be called once");
        }

        evalId = UUID.randomUUID().toString();
        long effectiveTimeout = Math.max(0, waitToExecuteTimeout);

        try {
            executor.submit(() -> runScript(listener, effectiveTimeout));
        } catch (RejectedExecutionException e) {
            state.set(ScriptState.Failed);
            listener.onFinished(evalId, new EvalResult(false, "", "Executor rejected: " + e.getMessage()));
        }
    }

    private void runScript(EvalListener listener, long waitToExecuteTimeout) {
        executingThread = Thread.currentThread();
        Log.i(TAG, "等待执行槽, timeout=" + waitToExecuteTimeout + "ms");
        try {
            boolean acquired;
            try {
                acquired = semaphore.tryAcquire(waitToExecuteTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                state.set(ScriptState.Interrupted);
                Log.w(TAG, "等待执行槽时被中断");
                listener.onFinished(evalId, new EvalResult(false, "", "Interrupted while waiting for execution slot"));
                return;
            }

            if (!acquired) {
                state.set(ScriptState.Failed);
                Log.w(TAG, "等待执行槽超时");
                listener.onFinished(evalId, new EvalResult(false, "", "Timeout waiting for execution slot"));
                return;
            }

            Log.i(TAG, "获得执行槽，开始执行脚本");
            state.set(ScriptState.Executing);

            try {
                executeScript(listener);
            } finally {
                semaphore.release();
            }
        } finally {
            executingThread = null;
        }
    }

    private void executeScript(EvalListener listener) {
        List<String> logBuffer = new ArrayList<>();

        try {
            Log.i(TAG, "开始执行脚本, evalId=" + evalId);
            Globals globals = JsePlatform.standardGlobals();

            LuaValue printFunc = new PrintFunction(logBuffer);
            globals.set("print", printFunc);
            globals.set("log", printFunc);

            for (Map.Entry<String, Object> entry : injections.entrySet()) {
                try {
                    Object value = entry.getValue();
                    if (value != null) {
                        globals.set(entry.getKey(), CoerceJavaToLua.coerce(value));
                        Log.i(TAG, "注入对象: " + entry.getKey() + " -> " + value.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    String warnMsg = "Warning: Could not inject '" + entry.getKey() + "': " + e.getMessage();
                    logBuffer.add(warnMsg);
                    Log.w(TAG, warnMsg);
                }
            }

            Log.i(TAG, "脚本内容:\n" + scriptContent);
            LuaValue result = globals.load(scriptContent).call();
            Log.i(TAG, "脚本执行完成, 结果: " + (result != null && !result.isnil() ? result.toString() : "nil"));

            if (interrupted.get()) {
                state.set(ScriptState.Interrupted);
                listener.onFinished(evalId, new EvalResult(false, "", "Script was interrupted"));
            } else {
                String resultStr = result != null && !result.isnil() ? result.toString() : "";
                if (!logBuffer.isEmpty()) {
                    listener.onLogAppended(evalId, new ArrayList<>(logBuffer));
                }
                listener.onFinished(evalId, new EvalResult(true, resultStr, ""));
            }

        } catch (Exception e) {
            Log.e(TAG, "脚本执行异常: " + e.getMessage(), e);
            if (!logBuffer.isEmpty()) {
                listener.onLogAppended(evalId, new ArrayList<>(logBuffer));
            }
            if (interrupted.get()) {
                state.set(ScriptState.Interrupted);
                listener.onFinished(evalId, new EvalResult(false, "", "Script was interrupted"));
            } else {
                state.set(ScriptState.Failed);
                String errorMsg = e.getMessage();
                if (errorMsg == null) {
                    errorMsg = e.getClass().getName();
                }
                listener.onFinished(evalId, new EvalResult(false, "", errorMsg));
            }
        }
    }

    @Override
    public ScriptState state() {
        return state.get();
    }

    @Override
    public void interruptEval() {
        interrupted.set(true);
        Thread t = executingThread;
        if (t != null) {
            t.interrupt();
        }
    }

    private static class PrintFunction extends VarArgFunction {
        private static final String TAG = "LuaScript";
        private final List<String> buffer;

        PrintFunction(List<String> buffer) {
            this.buffer = buffer;
        }

        @Override
        public Varargs invoke(Varargs args) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= args.narg(); i++) {
                if (i > 1) sb.append("\t");
                sb.append(args.arg(i).toString());
            }
            String message = sb.toString();
            buffer.add(message);
            Log.i(TAG, message);
            return LuaValue.NIL;
        }
    }
}
