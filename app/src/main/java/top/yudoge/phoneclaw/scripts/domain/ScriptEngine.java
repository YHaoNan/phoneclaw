package top.yudoge.phoneclaw.scripts.domain;

/**
 * 脚本执行引擎
 */
public interface ScriptEngine {


    /**
     * 新建一个脚本执行对象。
     *
     * 脚本不会立即执行，而是返回一个EvalHandle，可以通过它来执行脚本、跟踪脚本的执行状态等
     *
     * @param scriptContent 脚本
     * @return 脚本执行句柄
     */
    EvalHandle newEval(String scriptContent);


}
