package top.yudoge.phoneclaw.scripts.impl;

import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

public class JSEvalHandle implements EvalHandle {

    private final ExecutorService executor;
    private final Semaphore semaphore;
    private final String scriptContent;
    private final Map<String, Object> injections = new ConcurrentHashMap<>();
    private final AtomicReference<ScriptState> state = new AtomicReference<>(ScriptState.WaitToExecute);
    private final AtomicBoolean evaluated = new AtomicBoolean(false);
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private volatile Thread executingThread;
    private String evalId;

    public JSEvalHandle(ExecutorService executor, Semaphore semaphore, String scriptContent) {
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
    public void eval(@NotNull EvalListener listener, long waitToExecuteTimeout) {
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
        try {
            boolean acquired;
            try {
                acquired = semaphore.tryAcquire(waitToExecuteTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                state.set(ScriptState.Interrupted);
                listener.onFinished(evalId, new EvalResult(false, "", "Interrupted while waiting for execution slot"));
                return;
            }

            if (!acquired) {
                state.set(ScriptState.Failed);
                listener.onFinished(evalId, new EvalResult(false, "", "Timeout waiting for execution slot"));
                return;
            }

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
        InterruptibleContextFactory factory = new InterruptibleContextFactory(interrupted);
        JsLogger logger = new JsLogger(listener, evalId);

        try {
            factory.call(cx -> {
                cx.setOptimizationLevel(-1);
                cx.setInstructionObserverThreshold(10000);

                Scriptable scope = cx.initStandardObjects();

                scope.put("__pc_logger", scope, Context.javaToJS(logger, scope));
                cx.evaluateString(scope,
                        "var console = { log: function() { __pc_logger.log(Array.prototype.slice.call(arguments).join(' ')); } };\n" +
                                "var print = console.log;",
                        "setup", 1, null);

                for (Map.Entry<String, Object> entry : injections.entrySet()) {
                    scope.put(entry.getKey(), scope, Context.javaToJS(entry.getValue(), scope));
                }

                Object result = cx.evaluateString(scope, scriptContent, "script", 1, null);

                logger.flush();

                if (interrupted.get()) {
                    state.set(ScriptState.Interrupted);
                    listener.onFinished(evalId, new EvalResult(false, "", "Script was interrupted"));
                } else {
                    String resultStr = result != null ? Context.toString(result) : "";
                    listener.onFinished(evalId, new EvalResult(true, resultStr, ""));
                }

                return null;
            });
        } catch (ScriptInterruptedException e) {
            logger.flush();
            state.set(ScriptState.Interrupted);
            listener.onFinished(evalId, new EvalResult(false, "", "Script was interrupted"));
        } catch (org.mozilla.javascript.JavaScriptException e) {
            logger.flush();
            state.set(ScriptState.Failed);
            listener.onFinished(evalId, new EvalResult(false, "", e.getMessage()));
        } catch (org.mozilla.javascript.EcmaError e) {
            logger.flush();
            state.set(ScriptState.Failed);
            listener.onFinished(evalId, new EvalResult(false, "", e.getMessage()));
        } catch (org.mozilla.javascript.EvaluatorException e) {
            logger.flush();
            state.set(ScriptState.Failed);
            listener.onFinished(evalId, new EvalResult(false, "", e.getMessage()));
        } catch (Exception e) {
            logger.flush();
            if (interrupted.get()) {
                state.set(ScriptState.Interrupted);
                listener.onFinished(evalId, new EvalResult(false, "", "Script was interrupted"));
            } else {
                state.set(ScriptState.Failed);
                listener.onFinished(evalId, new EvalResult(false, "", e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
            }
        }
    }

    @Override
    public @NotNull ScriptState state() {
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

    static class ScriptInterruptedException extends RuntimeException {
        ScriptInterruptedException() {
            super("Script interrupted");
        }
    }

    static class InterruptibleContextFactory extends ContextFactory {
        private final AtomicBoolean interrupted;

        InterruptibleContextFactory(AtomicBoolean interrupted) {
            this.interrupted = interrupted;
        }

        @Override
        protected void observeInstructionCount(Context cx, int count) {
            if (interrupted.get() || Thread.currentThread().isInterrupted()) {
                throw new ScriptInterruptedException();
            }
        }
    }

    public static class JsLogger {
        private final EvalListener listener;
        private final String evalId;
        private final List<String> buffer = new ArrayList<>();
        private static final int BATCH_SIZE = 50;

        public JsLogger(EvalListener listener, String evalId) {
            this.listener = listener;
            this.evalId = evalId;
        }

        public synchronized void log(String msg) {
            buffer.add(msg);
            if (buffer.size() >= BATCH_SIZE) {
                flushLocked();
            }
        }

        public synchronized void flush() {
            flushLocked();
        }

        private void flushLocked() {
            if (!buffer.isEmpty()) {
                List<String> snapshot = new ArrayList<>(buffer);
                buffer.clear();
                listener.onLogAppended(evalId, snapshot);
            }
        }
    }
}
