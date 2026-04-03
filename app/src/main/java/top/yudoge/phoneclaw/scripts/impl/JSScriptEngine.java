package top.yudoge.phoneclaw.scripts.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import top.yudoge.phoneclaw.scripts.EvalHandle;
import top.yudoge.phoneclaw.scripts.ScriptEngine;

public class JSScriptEngine implements ScriptEngine {

    private final ExecutorService executor;
    private final Semaphore semaphore;

    public JSScriptEngine() {
        this(4);
    }

    public JSScriptEngine(int maxConcurrent) {
        this.executor = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "JS-Engine-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        this.semaphore = new Semaphore(maxConcurrent);
    }

    @Override
    public EvalHandle newEval(String scriptContent) {
        return new JSEvalHandle(executor, semaphore, scriptContent);
    }

}
