package top.yudoge.phoneclaw.scripts

import org.junit.Test
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.scripts.domain.EvalHandle
import top.yudoge.phoneclaw.scripts.domain.EvalListener
import top.yudoge.phoneclaw.scripts.domain.impl.LuaScriptEngine
import top.yudoge.phoneclaw.scripts.domain.objects.EvalResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TestExecuteLuaScript {

    data class P(val x: Int)

    @Test
    fun test() {

        val scriptEngine = LuaScriptEngine()

        val result = StringBuilder()
        val error = StringBuilder()
        val handle: EvalHandle = scriptEngine.newEval("print(\"FUCK\");return p.x")
        val latch = CountDownLatch(1)

        handle.inject("p", P(1))

        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) {
                for (line in lines) {
                    result.append(line).append("\n")
                }
            }

            override fun onFinished(evalId: String, evalResult: EvalResult) {
                if (!evalResult.success) {
                    error.append(evalResult.error)
                }
                latch.countDown()
                print(evalResult.evalResult)
            }
        }, 60000)

        latch.await(65000, TimeUnit.MILLISECONDS)


    }

}