package top.yudoge.phoneclaw.scripts.domain;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import top.yudoge.phoneclaw.scripts.domain.impl.LuaScriptEngine;
import top.yudoge.phoneclaw.scripts.domain.objects.EvalResult;

public class ScriptServer {

    private static final String TAG = "ScriptServer";

    private final ScriptEngine engine;
    private final Map<String, Object> globalInjections = new ConcurrentHashMap<>();
    private final List<InjectionProvider> injectionProviders = new CopyOnWriteArrayList<>();
    
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int currentPort = -1;

    public ScriptServer() {
        this.engine = new LuaScriptEngine(4);
    }

    public ScriptServer(ScriptEngine engine) {
        this.engine = engine;
    }

    public void injectGlobal(String name, Object obj) {
        globalInjections.put(name, obj);
    }

    public void removeGlobalInjection(String name) {
        globalInjections.remove(name);
    }

    public void addInjectionProvider(InjectionProvider provider) {
        injectionProviders.add(provider);
    }

    public void removeInjectionProvider(InjectionProvider provider) {
        injectionProviders.remove(provider);
    }

    public int start(int port) {
        if (running.compareAndSet(false, true)) {
            try {
                serverSocket = new ServerSocket(port);
                currentPort = serverSocket.getLocalPort();
                executor = Executors.newCachedThreadPool();
                executor.submit(this::acceptLoop);
                Log.i(TAG, "服务器启动成功, 端口: " + currentPort);
                return currentPort;
            } catch (IOException e) {
                Log.e(TAG, "服务器启动失败: " + e.getMessage());
                running.set(false);
                return -1;
            }
        }
        return currentPort;
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException ignored) {}
            if (executor != null) {
                executor.shutdownNow();
            }
            currentPort = -1;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        return currentPort;
    }

    private void acceptLoop() {
        Log.i(TAG, "开始监听连接...");
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                Log.i(TAG, "收到客户端连接: " + client.getInetAddress());
                executor.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running.get()) {
                    Log.e(TAG, "acceptLoop异常: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            Log.i(TAG, "handleClient: 开始处理请求");
            java.io.InputStream inputStream = client.getInputStream();
            OutputStream out = client.getOutputStream();

            StringBuilder headerBuilder = new StringBuilder();
            int b;
            int contentLength = 0;
            int emptyLineCount = 0;
            
            while ((b = inputStream.read()) != -1) {
                headerBuilder.append((char) b);
                
                int len = headerBuilder.length();
                if (len >= 4 && headerBuilder.substring(len - 4).equals("\r\n\r\n")) {
                    break;
                }
            }
            
            String headers = headerBuilder.toString();
            String[] lines = headers.split("\r\n");
            
            String firstLine = lines[0];
            if (firstLine == null || !firstLine.startsWith("POST")) {
                Log.w(TAG, "handleClient: 无效请求, line=" + firstLine);
                sendResponse(out, 400, "Bad Request: expected POST");
                client.close();
                return;
            }

            String path = firstLine.split(" ")[1];
            Log.i(TAG, "handleClient: path=" + path);
            
            for (int i = 1; i < lines.length; i++) {
                String header = lines[i];
                if (header.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(header.substring(15).trim());
                }
            }

            String body = "";
            if (contentLength > 0) {
                byte[] bodyBytes = new byte[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int n = inputStream.read(bodyBytes, read, contentLength - read);
                    if (n < 0) break;
                    read += n;
                }
                body = new String(bodyBytes, 0, read, StandardCharsets.UTF_8);
            }

            Log.i(TAG, "handleClient: body长度=" + body.length() + ", contentLength=" + contentLength);

            if ("/eval".equals(path)) {
                handleEval(out, body, client);
            } else {
                sendResponse(out, 404, "Not Found: use /eval");
                client.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "handleClient异常: " + e.getMessage(), e);
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleEval(OutputStream out, String script, Socket client) {
        Log.i(TAG, "handleEval: 收到脚本, 长度=" + (script != null ? script.length() : 0));
        Log.i(TAG, "handleEval: 脚本内容:\n" + script);
        
        EvalHandle handle = engine.newEval(script);

        Log.i(TAG, "handleEval: 注入全局对象数量=" + globalInjections.size());
        for (Map.Entry<String, Object> entry : globalInjections.entrySet()) {
            Log.i(TAG, "handleEval: 注入 " + entry.getKey() + " -> " + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null"));
            handle.inject(entry.getKey(), entry.getValue());
        }

        for (InjectionProvider provider : injectionProviders) {
            Map<String, Object> injections = provider.getInjections();
            if (injections != null) {
                for (Map.Entry<String, Object> entry : injections.entrySet()) {
                    handle.inject(entry.getKey(), entry.getValue());
                }
            }
        }

        StringBuilder logBuilder = new StringBuilder();

        Log.i(TAG, "handleEval: 调用 eval()");
        handle.eval(new EvalListener() {
            @Override
            public void onLogAppended(String evalId, List<String> lines) {
                Log.i(TAG, "onLogAppended: evalId=" + evalId + ", lines=" + lines.size());
                for (String line : lines) {
                    logBuilder.append(line).append("\n");
                }
            }

            @Override
            public void onFinished(String evalId, EvalResult result) {
                Log.i(TAG, "onFinished: evalId=" + evalId + ", success=" + result.getSuccess());
                try {
                    StringBuilder response = new StringBuilder();
                    if (result.getSuccess()) {
                        response.append("SUCCESS\n");
                        if (logBuilder.length() > 0) {
                            response.append("--- LOGS ---\n");
                            response.append(logBuilder);
                            response.append("--- RESULT ---\n");
                        }
                        response.append(result.getEvalResult());
                    } else {
                        response.append("FAILED\n");
                        if (logBuilder.length() > 0) {
                            response.append("--- LOGS ---\n");
                            response.append(logBuilder);
                            response.append("--- ERROR ---\n");
                        }
                        response.append(result.getError());
                    }
                    sendResponse(out, 200, response.toString());
                } catch (IOException e) {
                    Log.e(TAG, "onFinished: 发送响应失败: " + e.getMessage());
                } finally {
                    try {
                        client.close();
                    } catch (IOException ignored) {}
                }
            }
        }, 30000);
    }

    private void sendResponse(OutputStream out, int code, String body) throws IOException {
        String status = code == 200 ? "OK" : (code == 400 ? "Bad Request" : "Not Found");
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + code + " " + status + "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    public interface InjectionProvider {
        Map<String, Object> getInjections();
    }
}
