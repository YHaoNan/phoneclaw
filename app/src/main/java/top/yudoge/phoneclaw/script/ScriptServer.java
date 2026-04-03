package top.yudoge.phoneclaw.script;

import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import top.yudoge.phoneclaw.SelectToSpeakService;

public class ScriptServer {
    private static final String TAG = "ScriptServer";
    private static final int DEFAULT_PORT = 8765;

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;
    private android.content.Context androidContext;

    public ScriptServer(android.content.Context context) {
        this.androidContext = context;
        this.executor = Executors.newCachedThreadPool();
    }

    public int start() {
        return start(DEFAULT_PORT);
    }

    public int start(int port) {
        if (running) {
            return -1;
        }

        try {
            serverSocket = new ServerSocket(port);
            running = true;
            
            final int actualPort = serverSocket.getLocalPort();
            Log.i(TAG, "Script server started on port " + actualPort);

            executor.submit(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.submit(() -> handleClient(client));
                    } catch (IOException e) {
                        if (running) {
                            Log.e(TAG, "Error accepting client", e);
                        }
                    }
                }
            });

            return actualPort;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server", e);
            return -1;
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server", e);
            }
        }
        if (executor != null) {
            executor.shutdown();
        }
        Log.i(TAG, "Script server stopped");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
    }

    private void handleClient(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream output = client.getOutputStream();

            String line = reader.readLine();
            if (line == null || !line.startsWith("POST")) {
                sendResponse(output, 400, "Bad Request");
                client.close();
                return;
            }

            int contentLength = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) break;
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }

            if (contentLength > 0) {
                char[] body = new char[contentLength];
                reader.read(body, 0, contentLength);
                String script = new String(body);

                String result = executeScript(script);
                sendResponse(output, 200, result);
            } else {
                sendResponse(output, 400, "No script content");
            }

            client.close();
        } catch (Exception e) {
            Log.e(TAG, "Error handling client", e);
            try {
                sendResponse(client.getOutputStream(), 500, "Error: " + e.getMessage());
                client.close();
            } catch (IOException ignored) {}
        }
    }

    private void sendResponse(OutputStream output, int code, String body) throws IOException {
        String response = "HTTP/1.1 " + code + " " + (code == 200 ? "OK" : "Error") + "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Connection: close\r\n" +
                "\r\n" + body;
        output.write(response.getBytes("UTF-8"));
        output.flush();
    }

    private String executeScript(String script) {
        try {
            Globals g = JsePlatform.standardGlobals();
            
            SelectToSpeakService accessibilityService = SelectToSpeakService.getService();
            ScriptHelper helper = new ScriptHelper(accessibilityService);
            
            LuaValue luaHelper = CoerceJavaToLua.coerce(helper);
            g.set("helper", luaHelper);
            
            LuaValue luaService = CoerceJavaToLua.coerce(accessibilityService);
            g.set("service", luaService);

            LuaValue result = g.load(script).call();
            
            if (!result.isnil()) {
                return result.tojstring();
            }
            return "Script executed successfully";
        } catch (Exception e) {
            Log.e(TAG, "Script execution error", e);
            return "Error: " + e.getClass().getName() + ": " + e.getMessage();
        }
    }
}
