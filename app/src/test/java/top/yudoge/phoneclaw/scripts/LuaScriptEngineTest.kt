package top.yudoge.phoneclaw.scripts

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yudoge.phoneclaw.scripts.domain.EvalHandle
import top.yudoge.phoneclaw.scripts.domain.EvalListener
import top.yudoge.phoneclaw.scripts.domain.impl.LuaScriptEngine
import top.yudoge.phoneclaw.scripts.domain.objects.EvalResult
import top.yudoge.phoneclaw.scripts.domain.objects.ScriptState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class LuaScriptEngineTest {

    private lateinit var scriptEngine: LuaScriptEngine

    @Before
    fun setup() {
        scriptEngine = LuaScriptEngine()
    }

    // ========== 测试数据类 ==========

    data class Person(val name: String, val age: Int) {
        fun greet(): String = "Hello, I'm $name, $age years old"
        fun addYears(years: Int): Int = age + years
    }

    class Calculator {
        fun add(a: Int, b: Int): Int = a + b
        fun multiply(a: Double, b: Double): Double = a * b
        fun concat(s1: String, s2: String): String = s1 + s2
    }

    data class ComplexObject(
        val id: Long,
        val name: String,
        val active: Boolean,
        val score: Double
    ) {
        fun isEligible(): Boolean = active && score > 50.0
        fun formatInfo(): String = "[$id] $name - Score: $score"
    }

    // ========== 辅助方法 ==========

    private fun executeScript(
        script: String,
        injections: Map<String, Any> = emptyMap(),
        timeoutMs: Long = 5000
    ): TestResult {
        val handle = scriptEngine.newEval(script)
        injections.forEach { (name, obj) -> handle.inject(name, obj) }

        val resultRef = AtomicReference<EvalResult>()
        val logsRef = AtomicReference<List<String>>(emptyList())
        val latch = CountDownLatch(1)

        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) {
                logsRef.set(lines)
            }
            override fun onFinished(evalId: String, result: EvalResult) {
                resultRef.set(result)
                latch.countDown()
            }
        }, timeoutMs)

        val completed = latch.await(timeoutMs + 1000, TimeUnit.MILLISECONDS)
        return TestResult(
            completed = completed,
            result = resultRef.get(),
            logs = logsRef.get()
        )
    }

    data class TestResult(
        val completed: Boolean,
        val result: EvalResult?,
        val logs: List<String>
    )

    // ========== 1. 普通Lua脚本执行测试（无Java交互） ==========

    @Test
    fun `test simple print statement`() {
        val testResult = executeScript("print('Hello Lua')")
        
        assertTrue("Script should complete", testResult.completed)
        assertNotNull("Result should not be null", testResult.result)
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Log should contain Hello Lua", "Hello Lua", testResult.logs.firstOrNull())
    }

    @Test
    fun `test lua arithmetic operations`() {
        val testResult = executeScript("""
            local a = 10
            local b = 20
            local sum = a + b
            return sum
        """.trimIndent())
        
        assertTrue("Script should complete", testResult.completed)
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Result should be 30", "30", testResult.result.evalResult)
    }

    @Test
    fun `test lua string operations`() {
        val testResult = executeScript("""
            local s1 = "Hello"
            local s2 = "World"
            return s1 .. " " .. s2
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Result should be Hello World", "Hello World", testResult.result.evalResult)
    }

    @Test
    fun `test lua table operations`() {
        val testResult = executeScript("""
            local t = {10, 20, 30, 40, 50}
            local sum = 0
            for i, v in ipairs(t) do
                sum = sum + v
            end
            return sum
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Sum should be 150", "150", testResult.result.evalResult)
    }

    @Test
    fun `test lua function definition and call`() {
        val testResult = executeScript("""
            function factorial(n)
                if n <= 1 then return 1 end
                return n * factorial(n - 1)
            end
            return factorial(5)
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("5! should be 120", "120", testResult.result.evalResult)
    }

    @Test
    fun `test lua control flow if-else`() {
        val testResult = executeScript("""
            local x = 15
            if x > 10 then
                return "greater"
            else
                return "smaller"
            end
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return greater", "greater", testResult.result.evalResult)
    }

    @Test
    fun `test lua for loop`() {
        val testResult = executeScript("""
            local sum = 0
            for i = 1, 10 do
                sum = sum + i
            end
            return sum
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Sum 1-10 should be 55", "55", testResult.result.evalResult)
    }

    @Test
    fun `test lua while loop`() {
        val testResult = executeScript("""
            local i = 0
            local count = 0
            while i < 5 do
                count = count + 1
                i = i + 1
            end
            return count
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Count should be 5", "5", testResult.result.evalResult)
    }

    @Test
    fun `test lua math library`() {
        val testResult = executeScript("""
            local sqrtVal = math.sqrt(16)
            local powVal = math.pow(2, 8)
            return sqrtVal .. "," .. powVal
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Result should contain 4", testResult.result.evalResult.contains("4"))
        assertTrue("Result should contain 256", testResult.result.evalResult.contains("256"))
    }

    @Test
    fun `test lua string library`() {
        val testResult = executeScript("""
            local s = "hello world"
            local upper = string.upper(s)
            local sub = string.sub(s, 1, 5)
            return upper .. "|" .. sub
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Result should contain HELLO WORLD", testResult.result.evalResult.contains("HELLO WORLD"))
    }

    @Test
    fun `test lua returns nil`() {
        val testResult = executeScript("return nil")
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Result should be empty for nil", "", testResult.result.evalResult)
    }

    @Test
    fun `test lua returns boolean`() {
        val testResult = executeScript("return true")
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Result should be true", "true", testResult.result.evalResult)
    }

    @Test
    fun `test lua syntax error`() {
        val testResult = executeScript("if x then return 1")
        
        assertTrue("Script should complete", testResult.completed)
        assertFalse("Script should fail on syntax error", testResult.result!!.success)
        assertTrue("Error message should indicate syntax error", 
            testResult.result.error.isNotEmpty())
    }

    @Test
    fun `test lua runtime error`() {
        val testResult = executeScript("local x = nil; return x.field")
        
        assertTrue("Script should complete", testResult.completed)
        assertFalse("Script should fail on runtime error", testResult.result!!.success)
        assertTrue("Error message should not be empty", testResult.result.error.isNotEmpty())
    }

    // ========== 2. Java传入原生类型测试 ==========

    @Test
    fun `test inject integer`() {
        val testResult = executeScript("return injectedInt", mapOf("injectedInt" to 42))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return injected integer", "42", testResult.result.evalResult)
    }

    @Test
    fun `test inject long`() {
        val testResult = executeScript("return injectedLong", mapOf("injectedLong" to 9876543210L))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return injected long", "9876543210", testResult.result.evalResult)
    }

    @Test
    fun `test inject double`() {
        val testResult = executeScript("return injectedDouble", mapOf("injectedDouble" to 3.14159))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Should return injected double", 
            testResult.result.evalResult.startsWith("3.14"))
    }

    @Test
    fun `test inject float`() {
        val testResult = executeScript("return injectedFloat", mapOf("injectedFloat" to 2.5f))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Should return injected float", testResult.result.evalResult.startsWith("2.5"))
    }

    @Test
    fun `test inject boolean true`() {
        val testResult = executeScript("return injectedBool", mapOf("injectedBool" to true))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return true", "true", testResult.result.evalResult)
    }

    @Test
    fun `test inject boolean false`() {
        val testResult = executeScript("return injectedBool", mapOf("injectedBool" to false))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return false", "false", testResult.result.evalResult)
    }

    @Test
    fun `test inject string`() {
        val testResult = executeScript("return injectedStr", mapOf("injectedStr" to "Hello from Java"))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return injected string", "Hello from Java", testResult.result.evalResult)
    }

    @Test
    fun `test inject multiple primitive types`() {
        val testResult = executeScript("""
            return injectedInt .. "," .. injectedStr .. "," .. tostring(injectedBool)
        """.trimIndent(), mapOf(
            "injectedInt" to 100,
            "injectedStr" to "test",
            "injectedBool" to true
        ))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Should contain 100", testResult.result.evalResult.contains("100"))
        assertTrue("Should contain test", testResult.result.evalResult.contains("test"))
        assertTrue("Should contain true", testResult.result.evalResult.contains("true"))
    }

    @Test
    fun `test use injected integer in lua calculation`() {
        val testResult = executeScript("""
            local result = injectedInt * 2 + 10
            return result
        """.trimIndent(), mapOf("injectedInt" to 5))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should calculate 5*2+10=20", "20", testResult.result.evalResult)
    }

    @Test
    fun `test use injected string in lua concatenation`() {
        val testResult = executeScript("""
            return "Prefix: " .. injectedStr .. "!"
        """.trimIndent(), mapOf("injectedStr" to "World"))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should concatenate", "Prefix: World!", testResult.result.evalResult)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `test inject null value should handle gracefully`() {
        val handle = scriptEngine.newEval("return type(maybeNull)")
        handle.inject("maybeNull", null as Any)
        
        val resultRef = AtomicReference<EvalResult>()
        val latch = CountDownLatch(1)
        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) {}
            override fun onFinished(evalId: String, result: EvalResult) {
                resultRef.set(result)
                latch.countDown()
            }
        }, 5000)
        latch.await(6000, TimeUnit.MILLISECONDS)
        
        // null injection should be handled gracefully (either ignored or set to nil)
    }

    // ========== 3. Java传入对象测试 ==========

    @Test
    fun `test inject simple data class`() {
        val person = Person("Alice", 30)
        val testResult = executeScript("return person.name", mapOf("person" to person))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should access person name", "Alice", testResult.result.evalResult)
    }

    @Test
    fun `test inject data class and access multiple fields`() {
        val person = Person("Bob", 25)
        val testResult = executeScript("""
            return person.name .. " is " .. person.age .. " years old"
        """.trimIndent(), mapOf("person" to person))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should format person info", "Bob is 25 years old", testResult.result.evalResult)
    }

    @Test
    fun `test inject complex object with multiple fields`() {
        val obj = ComplexObject(123L, "TestObject", true, 85.5)
        val testResult = executeScript("""
            return obj.id .. "|" .. obj.name .. "|" .. tostring(obj.active) .. "|" .. obj.score
        """.trimIndent(), mapOf("obj" to obj))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Should contain id", testResult.result.evalResult.contains("123"))
        assertTrue("Should contain name", testResult.result.evalResult.contains("TestObject"))
    }

    @Test
    fun `test inject object with boolean field and use in condition`() {
        val obj = ComplexObject(1L, "Active", true, 90.0)
        val testResult = executeScript("""
            if obj.active then
                return "ACTIVE"
            else
                return "INACTIVE"
            end
        """.trimIndent(), mapOf("obj" to obj))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should be active", "ACTIVE", testResult.result.evalResult)
    }

    @Test
    fun `test inject multiple objects`() {
        val person1 = Person("Alice", 30)
        val person2 = Person("Bob", 25)
        val testResult = executeScript("""
            return p1.name .. " and " .. p2.name
        """.trimIndent(), mapOf("p1" to person1, "p2" to person2))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return both names", "Alice and Bob", testResult.result.evalResult)
    }

    @Test
    fun `test inject object and use in loop`() {
        val testResult = executeScript("""
            local names = {"Alice", "Bob", "Charlie"}
            local result = ""
            for i, name in ipairs(names) do
                result = result .. name
                if i < #names then result = result .. ", " end
            end
            return result
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should join names", "Alice, Bob, Charlie", testResult.result.evalResult)
    }

    // ========== 4. 调用Java对象方法测试 ==========

    @Test
    fun `test call java method with no arguments`() {
        val person = Person("Charlie", 35)
        val testResult = executeScript("return person:greet()", mapOf("person" to person))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should call greet method", "Hello, I'm Charlie, 35 years old", testResult.result.evalResult)
    }

    @Test
    fun `test call java method with integer argument`() {
        val person = Person("David", 20)
        val testResult = executeScript("return person:addYears(10)", mapOf("person" to person))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should call addYears method", "30", testResult.result.evalResult)
    }

    @Test
    fun `test call calculator add method`() {
        val calc = Calculator()
        val testResult = executeScript("return calc:add(15, 27)", mapOf("calc" to calc))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return sum", "42", testResult.result.evalResult)
    }

    @Test
    fun `test call calculator multiply method with doubles`() {
        val calc = Calculator()
        val testResult = executeScript("return calc:multiply(3.5, 2.0)", mapOf("calc" to calc))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Should return product", testResult.result.evalResult.startsWith("7"))
    }

    @Test
    fun `test call calculator concat method with strings`() {
        val calc = Calculator()
        val testResult = executeScript("return calc:concat('Hello', 'World')", mapOf("calc" to calc))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should return concatenated string", "HelloWorld", testResult.result.evalResult)
    }

    @Test
    fun `test call method returning boolean`() {
        val obj1 = ComplexObject(1L, "Test1", true, 80.0)
        val obj2 = ComplexObject(2L, "Test2", false, 90.0)
        val obj3 = ComplexObject(3L, "Test3", true, 40.0)
        
        val testResult1 = executeScript("return obj:isEligible()", mapOf("obj" to obj1))
        assertTrue("Active with score > 50 should be eligible", 
            testResult1.result!!.success && testResult1.result.evalResult == "true")
        
        val testResult2 = executeScript("return obj:isEligible()", mapOf("obj" to obj2))
        assertTrue("Inactive should not be eligible", 
            testResult2.result!!.success && testResult2.result.evalResult == "false")
        
        val testResult3 = executeScript("return obj:isEligible()", mapOf("obj" to obj3))
        assertTrue("Active with score < 50 should not be eligible", 
            testResult3.result!!.success && testResult3.result.evalResult == "false")
    }

    @Test
    fun `test call method and use result in lua logic`() {
        val calc = Calculator()
        val testResult = executeScript("""
            local sum = calc:add(10, 20)
            if sum > 25 then
                return "Large: " .. sum
            else
                return "Small: " .. sum
            end
        """.trimIndent(), mapOf("calc" to calc))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should use method result in condition", "Large: 30", testResult.result.evalResult)
    }

    @Test
    fun `test chain multiple method calls`() {
        val calc = Calculator()
        val testResult = executeScript("""
            local a = calc:add(5, 3)
            local b = calc:add(10, 20)
            return calc:add(a, b)
        """.trimIndent(), mapOf("calc" to calc))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should chain additions: 8 + 30 = 38", "38", testResult.result.evalResult)
    }

    @Test
    fun `test call method with injected value as argument`() {
        val calc = Calculator()
        val testResult = executeScript("""
            local result = calc:add(value, 100)
            return result
        """.trimIndent(), mapOf("calc" to calc, "value" to 50))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should use injected value as argument", "150", testResult.result.evalResult)
    }

    @Test
    fun `test call formatInfo method`() {
        val obj = ComplexObject(999L, "MyObject", true, 95.5)
        val testResult = executeScript("return obj:formatInfo()", mapOf("obj" to obj))
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertTrue("Should contain id", testResult.result.evalResult.contains("999"))
        assertTrue("Should contain name", testResult.result.evalResult.contains("MyObject"))
        assertTrue("Should contain score", testResult.result.evalResult.contains("95.5"))
    }

    // ========== 5. EvalHandle状态和行为测试 ==========

    @Test
    fun `test initial state is WaitToExecute`() {
        val handle = scriptEngine.newEval("return 1")
        assertEquals("Initial state should be WaitToExecute", 
            ScriptState.WaitToExecute, handle.state())
    }

    @Test
    fun `test cannot inject after eval called`() {
        val handle = scriptEngine.newEval("return 1")
        val latch = CountDownLatch(1)
        
        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) {}
            override fun onFinished(evalId: String, result: EvalResult) {
                latch.countDown()
            }
        }, 5000)
        
        Thread.sleep(100) // Ensure eval has been initiated
        
        try {
            handle.inject("test", "value")
            fail("Should throw IllegalStateException when injecting after eval")
        } catch (e: IllegalStateException) {
            // Expected
        }
        
        latch.await(6000, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `test cannot call eval twice`() {
        val handle = scriptEngine.newEval("return 1")
        val latch = CountDownLatch(1)
        
        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) {}
            override fun onFinished(evalId: String, result: EvalResult) {
                latch.countDown()
            }
        }, 5000)
        
        try {
            handle.eval(object : EvalListener {
                override fun onLogAppended(evalId: String, lines: List<String>) {}
                override fun onFinished(evalId: String, result: EvalResult) {}
            }, 5000)
            fail("Should throw IllegalStateException when calling eval twice")
        } catch (e: IllegalStateException) {
            // Expected
        }
        
        latch.await(6000, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `test inject with null name should throw`() {
        val handle = scriptEngine.newEval("return 1")
        
        try {
            @Suppress("UNCHECKED_CAST")
            val nullName = null as String
            handle.inject(nullName, "value")
            fail("Should throw IllegalArgumentException for null name")
        } catch (e: IllegalArgumentException) {
            // Expected
        } catch (e: NullPointerException) {
            // Also acceptable - Kotlin null safety
        }
    }

    @Test
    fun `test inject with empty name should throw`() {
        val handle = scriptEngine.newEval("return 1")
        
        try {
            handle.inject("", "value")
            fail("Should throw IllegalArgumentException for empty name")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `test newEval with null script should throw`() {
        try {
            scriptEngine.newEval(null as String?)
            fail("Should throw IllegalArgumentException for null script")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `test log function alias for print`() {
        val testResult = executeScript("log('Test log function')")
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should log via log function", "Test log function", testResult.logs.firstOrNull())
    }

    @Test
    fun `test multiple print statements`() {
        val testResult = executeScript("""
            print('Line 1')
            print('Line 2')
            print('Line 3')
        """.trimIndent())
        
        assertTrue("Script should succeed", testResult.result!!.success)
        assertEquals("Should have 3 log lines", 3, testResult.logs.size)
        assertEquals("First line", "Line 1", testResult.logs[0])
        assertEquals("Second line", "Line 2", testResult.logs[1])
        assertEquals("Third line", "Line 3", testResult.logs[2])
    }
}
