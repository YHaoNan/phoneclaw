package top.yudoge.phoneclaw.scripts.impl;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import top.yudoge.phoneclaw.scripts.EvalHandle;
import top.yudoge.phoneclaw.scripts.EvalListener;
import top.yudoge.phoneclaw.scripts.EvalResult;
import top.yudoge.phoneclaw.scripts.ScriptState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Unit tests for JSScriptEngine and JSEvalHandle.
 * Runs on JVM (not Android), tests Rhino-based JS execution.
 */
public class JSScriptEngineTest {

    private JSScriptEngine engine;

    @Before
    public void setUp() {
        engine = new JSScriptEngine(4);
    }

    @After
    public void tearDown() {
    }

    // ==================== Basic Computation ====================

    @Test
    public void testBasicArithmetic() throws Exception {
        EvalResult result = evalSync("1 + 2 * 3");
        assertTrue(result.getSuccess());
        assertEquals("7", result.getEvalResult());
    }

    @Test
    public void testStringConcatenation() throws Exception {
        EvalResult result = evalSync("'hello' + ' ' + 'world'");
        assertTrue(result.getSuccess());
        assertEquals("hello world", result.getEvalResult());
    }

    @Test
    public void testVariableDeclaration() throws Exception {
        EvalResult result = evalSync("var a = 10; var b = 20; a + b");
        assertTrue(result.getSuccess());
        assertEquals("30", result.getEvalResult());
    }

    @Test
    public void testFunctionDefinition() throws Exception {
        EvalResult result = evalSync(
                "function add(a, b) { return a + b; }\n" +
                        "add(3, 4)"
        );
        assertTrue(result.getSuccess());
        assertEquals("7", result.getEvalResult());
    }

    @Test
    public void testArrayOperations() throws Exception {
        EvalResult result = evalSync(
                "var arr = [1, 2, 3];\n" +
                        "arr.reduce(function(sum, v) { return sum + v; }, 0)"
        );
        assertTrue(result.getSuccess());
        assertEquals("6", result.getEvalResult());
    }

    @Test
    public void testObjectCreation() throws Exception {
        EvalResult result = evalSync(
                "var obj = { name: 'test', value: 42 };\n" +
                        "obj.name + ':' + obj.value"
        );
        assertTrue(result.getSuccess());
        assertEquals("test:42", result.getEvalResult());
    }

    // ==================== Console.log / print ====================

    @Test
    public void testConsoleLog() throws Exception {
        List<String> logs = new ArrayList<>();
        EvalResult result = evalSyncWithLogs("console.log('hello'); console.log('world'); 42", logs);
        if (!result.getSuccess()) {
            System.out.println("testConsoleLog FAILED: " + result.getError());
        }
        assertTrue(result.getSuccess());
        assertEquals("42", result.getEvalResult());
        assertEquals(2, logs.size());
        assertEquals("hello", logs.get(0));
        assertEquals("world", logs.get(1));
    }

    @Test
    public void testPrint() throws Exception {
        List<String> logs = new ArrayList<>();
        EvalResult result = evalSyncWithLogs("print('test output'); 1", logs);
        if (!result.getSuccess()) {
            System.out.println("testPrint FAILED: " + result.getError());
        }
        assertTrue(result.getSuccess());
        assertEquals(1, logs.size());
        assertEquals("test output", logs.get(0));
    }

    // ==================== Error Handling ====================

    @Test
    public void testSyntaxError() throws Exception {
        EvalResult result = evalSync("var a = {");
        assertFalse(result.getSuccess());
        assertNotNull(result.getError());
    }

    @Test
    public void testRuntimeError() throws Exception {
        EvalResult result = evalSync("undefinedFunction()");
        assertFalse(result.getSuccess());
        assertNotNull(result.getError());
    }

    @Test
    public void testNullScript() {
        try {
            engine.newEval(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    // ==================== Java Object Injection ====================

    @Test
    public void testInjectSimpleObject() throws Exception {
        Calculator calc = new Calculator();
        EvalResult result = evalSyncWithInjection("calc.add(3, 4)", "calc", calc);
        assertTrue(result.getSuccess());
        assertEquals("7", result.getEvalResult());
    }

    @Test
    public void testInjectObjectReadProperty() throws Exception {
        Person person = new Person();
        person.setName("Alice");
        person.setAge(30);

        EvalResult result = evalSyncWithInjection(
                "person.getName() + ' is ' + person.getAge() + ' years old'",
                "person", person
        );
        assertTrue(result.getSuccess());
        assertEquals("Alice is 30 years old", result.getEvalResult());
    }

    @Test
    public void testInjectObjectWithMultipleMethods() throws Exception {
        Calculator calc = new Calculator();
        EvalResult result = evalSyncWithInjection(
                "var a = calc.add(10, 20);\n" +
                        "var b = calc.multiply(a, 2);\n" +
                        "calc.subtract(b, 5)",
                "calc", calc
        );
        assertTrue(result.getSuccess());
        assertEquals("55", result.getEvalResult());
    }

    @Test
    public void testJSObjectPassedToJavaMethod() throws Exception {
        DataCollector collector = new DataCollector();
        EvalResult result = evalSyncWithInjection(
                "var data = { id: 123, name: 'test', active: true };\n" +
                        "collector.collect(data);\n" +
                        "collector.getCount()",
                "collector", collector
        );
        assertTrue(result.getSuccess());
        assertEquals("1", result.getEvalResult());
        assertEquals(1, collector.getCount());
    }

    @Test
    public void testJSStringPassedToJavaMethod() throws Exception {
        StringAccumulator acc = new StringAccumulator();
        EvalResult result = evalSyncWithInjection(
                "acc.append('hello');\n" +
                        "acc.append(' ');\n" +
                        "acc.append('world');\n" +
                        "acc.getResult()",
                "acc", acc
        );
        assertTrue(result.getSuccess());
        assertEquals("hello world", result.getEvalResult());
    }

    @Test
    public void testJSNumberPassedToJavaMethod() throws Exception {
        NumberProcessor proc = new NumberProcessor();
        EvalResult result = evalSyncWithInjection(
                "proc.process(42);\n" +
                        "proc.process(8);\n" +
                        "proc.getMax()",
                "proc", proc
        );
        assertTrue(result.getSuccess());
        assertEquals("42", result.getEvalResult());
    }

    @Test
    public void testMultipleInjections() throws Exception {
        Calculator calc = new Calculator();
        StringAccumulator acc = new StringAccumulator();

        EvalHandle handle = engine.newEval(
                "var sum = calc.add(100, 200);\n" +
                        "acc.append('sum=' + sum);\n" +
                        "acc.getResult()"
        );
        handle.inject("calc", calc);
        handle.inject("acc", acc);

        EvalResult result = evalSync(handle);
        assertTrue(result.getSuccess());
        assertEquals("sum=300", result.getEvalResult());
    }

    @Test
    public void testInjectCannotInjectAfterEval() throws Exception {
        EvalHandle handle = engine.newEval("1 + 1");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EvalResult> resultRef = new AtomicReference<>();

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                resultRef.set(result);
                latch.countDown();
            }
        }, 5000);

        try {
            handle.inject("x", "value");
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }

        latch.await(5, TimeUnit.SECONDS);
        assertTrue(resultRef.get().getSuccess());
    }

    @Test
    public void testInjectCannotCallEvalTwice() throws Exception {
        EvalHandle handle = engine.newEval("1");
        CountDownLatch latch = new CountDownLatch(1);

        EvalListener listener = new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                latch.countDown();
            }
        };

        handle.eval(listener, 5000);
        latch.await(5, TimeUnit.SECONDS);

        try {
            handle.eval(listener, 5000);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    // ==================== Interrupt ====================

    @Test
    public void testInterruptInfiniteLoop() throws Exception {
        EvalHandle handle = engine.newEval(
                "while (true) { var x = 1; }"
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EvalResult> resultRef = new AtomicReference<>();

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                resultRef.set(result);
                latch.countDown();
            }
        }, 10000);

        // Wait a bit for the script to start executing
        Thread.sleep(200);
        assertEquals(ScriptState.Executing, handle.state());

        handle.interruptEval();

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue("Script should have finished after interrupt", finished);
        EvalResult result = resultRef.get();
        assertFalse(result.getSuccess());
        assertNotNull(result.getError());
        assertTrue("Error should mention interrupt",
                result.getError().toLowerCase().contains("interrupt"));
        assertEquals(ScriptState.Interrupted, handle.state());
    }

    @Test
    public void testInterruptLongRunningScript() throws Exception {
        EvalHandle handle = engine.newEval(
                "var sum = 0;\n" +
                        "for (var i = 0; i < 1000000000; i++) {\n" +
                        "    sum += i;\n" +
                        "}\n" +
                        "sum;"
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EvalResult> resultRef = new AtomicReference<>();

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                resultRef.set(result);
                latch.countDown();
            }
        }, 10000);

        Thread.sleep(200);
        handle.interruptEval();

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue("Script should finish after interrupt", finished);
        EvalResult result = resultRef.get();
        assertFalse(result.getSuccess());
        assertTrue(result.getError().toLowerCase().contains("interrupt"));
    }

    @Test
    public void testInterruptBeforeExecution() throws Exception {
        EvalHandle handle = engine.newEval("1 + 1");

        handle.interruptEval();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EvalResult> resultRef = new AtomicReference<>();

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                resultRef.set(result);
                latch.countDown();
            }
        }, 5000);

        latch.await(5, TimeUnit.SECONDS);
        EvalResult result = resultRef.get();
        assertFalse(result.getSuccess());
        assertTrue(result.getError().toLowerCase().contains("interrupt"));
    }

    // ==================== Concurrency Limit ====================

    @Test
    public void testConcurrencyLimit() throws Exception {
        JSScriptEngine limitedEngine = new JSScriptEngine(2);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        CountDownLatch allFinished = new CountDownLatch(5);
        CountDownLatch allStarted = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            EvalHandle handle = limitedEngine.newEval(
                    "java.lang.Thread.sleep(500);\n" +
                            "var current = concurrentCounter.incrementAndGet();\n" +
                            "if (current > maxConcurrent.get()) maxConcurrent.set(current);\n" +
                            "java.lang.Thread.sleep(500);\n" +
                            "concurrentCounter.decrementAndGet();"
            );
            handle.inject("concurrentCounter", new AtomicInteger(0));
            handle.inject("maxConcurrent", maxConcurrent);

            handle.eval(new EvalListener() {
                @Override
                public void onLogAppended(String evalId, List<String> lines) {
                }

                @Override
                public void onFinished(String evalId, EvalResult result) {
                    allFinished.countDown();
                }
            }, 30000);
            allStarted.countDown();
        }

        allStarted.await(5, TimeUnit.SECONDS);
        boolean finished = allFinished.await(30, TimeUnit.SECONDS);
        assertTrue("All scripts should finish", finished);

        // With limit of 2, max concurrent should never exceed 2
        assertTrue("Max concurrent scripts should not exceed limit of 2, was: " + maxConcurrent.get(),
                maxConcurrent.get() <= 2);
    }

    @Test
    public void testWaitTimeout() throws Exception {
        JSScriptEngine limitedEngine = new JSScriptEngine(1);

        // Occupy the only slot
        CountDownLatch slotOccupied = new CountDownLatch(1);
        CountDownLatch firstFinished = new CountDownLatch(1);
        EvalHandle handle1 = limitedEngine.newEval(
                "slotOccupied.countDown();\n" +
                        "java.lang.Thread.sleep(5000);\n" +
                        "1"
        );
        handle1.inject("slotOccupied", slotOccupied);
        handle1.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                firstFinished.countDown();
            }
        }, 10000);

        slotOccupied.await(5, TimeUnit.SECONDS);

        // Second script with short timeout should fail
        EvalHandle handle2 = limitedEngine.newEval("2");
        CountDownLatch secondFinished = new CountDownLatch(1);
        AtomicReference<EvalResult> resultRef = new AtomicReference<>();

        handle2.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                resultRef.set(result);
                secondFinished.countDown();
            }
        }, 500); // 500ms timeout

        boolean finished = secondFinished.await(5, TimeUnit.SECONDS);
        assertTrue(finished);
        EvalResult result = resultRef.get();
        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("Timeout"));
        assertEquals(ScriptState.Failed, handle2.state());
    }

    // ==================== State Transitions ====================

    @Test
    public void testStateWaitToExecute() {
        EvalHandle handle = engine.newEval("1");
        assertEquals(ScriptState.WaitToExecute, handle.state());
    }

    @Test
    public void testStateExecuting() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        EvalHandle handle = engine.newEval(
                "started.countDown(); java.lang.Thread.sleep(2000); 1"
        );
        handle.inject("started", started);

        CountDownLatch finished = new CountDownLatch(1);
        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                finished.countDown();
            }
        }, 10000);

        started.await(5, TimeUnit.SECONDS);
        assertEquals(ScriptState.Executing, handle.state());

        finished.await(5, TimeUnit.SECONDS);
    }

    // ==================== Helper Classes ====================

    public static class Calculator {
        public int add(int a, int b) {
            return a + b;
        }

        public int subtract(int a, int b) {
            return a - b;
        }

        public int multiply(int a, int b) {
            return a * b;
        }

        public double divide(double a, double b) {
            return a / b;
        }
    }

    public static class Person {
        private String name;
        private int age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    public static class DataCollector {
        private final List<Object> collected = new ArrayList<>();

        public void collect(Object data) {
            collected.add(data);
        }

        public int getCount() {
            return collected.size();
        }
    }

    public static class StringAccumulator {
        private final StringBuilder sb = new StringBuilder();

        public void append(String s) {
            sb.append(s);
        }

        public String getResult() {
            return sb.toString();
        }
    }

    public static class NumberProcessor {
        private double max = Double.NEGATIVE_INFINITY;

        public void process(double value) {
            if (value > max) max = value;
        }

        public double getMax() {
            return max;
        }
    }

    // ==================== Helper Methods ====================

    private EvalResult evalSync(String script) throws Exception {
        return evalSync(engine.newEval(script));
    }

    private EvalResult evalSyncWithInjection(String script, String name, Object obj) throws Exception {
        EvalHandle handle = engine.newEval(script);
        handle.inject(name, obj);
        return evalSync(handle);
    }

    private EvalResult evalSyncWithLogs(String script, List<String> logs) throws Exception {
        EvalHandle handle = engine.newEval(script);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EvalResult> resultRef = new AtomicReference<>();

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
                synchronized (logs) {
                    logs.addAll(lines);
                }
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                resultRef.set(result);
                latch.countDown();
            }
        }, 10000);

        latch.await(10, TimeUnit.SECONDS);
        return resultRef.get();
    }

    private EvalResult evalSync(EvalHandle handle) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EvalResult> resultRef = new AtomicReference<>();

        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                resultRef.set(result);
                latch.countDown();
            }
        }, 10000);

        latch.await(10, TimeUnit.SECONDS);
        return resultRef.get();
    }
}
