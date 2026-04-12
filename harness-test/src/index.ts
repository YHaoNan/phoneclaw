import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import http from "http";

const SCRIPT_SERVER_HOST = process.env.SCRIPT_SERVER_HOST || "localhost";
const SCRIPT_SERVER_PORT = process.env.SCRIPT_SERVER_PORT || "8765";
const SCRIPT_SERVER_URL = `http://${SCRIPT_SERVER_HOST}:${SCRIPT_SERVER_PORT}`;

interface ScriptResponse {
  success: boolean;
  logs: string[];
  result: string;
  error: string;
}

function executeScriptOnServer(script: string): Promise<ScriptResponse> {
  return new Promise((resolve, reject) => {
    const scriptBuffer = Buffer.from(script, "utf-8");
    
    const options = {
      hostname: SCRIPT_SERVER_HOST,
      port: parseInt(SCRIPT_SERVER_PORT),
      path: "/eval",
      method: "POST",
      headers: {
        "Content-Type": "text/plain",
        "Content-Length": scriptBuffer.length,
      },
      timeout: 35000,
    };

    const req = http.request(options, (res) => {
      let data = "";
      
      res.on("data", (chunk) => {
        data += chunk;
      });
      
      res.on("end", () => {
        const response = parseScriptServerResponse(data);
        resolve(response);
      });
    });

    req.on("error", (err) => {
      resolve({
        success: false,
        logs: [],
        result: "",
        error: `Connection error: ${err.message}. Is ScriptServer running at ${SCRIPT_SERVER_URL}?`,
      });
    });

    req.on("timeout", () => {
      req.destroy();
      resolve({
        success: false,
        logs: [],
        result: "",
        error: `Request timeout. ScriptServer at ${SCRIPT_SERVER_URL} did not respond in 35 seconds.`,
      });
    });

    req.write(scriptBuffer);
    req.end();
  });
}

function parseScriptServerResponse(response: string): ScriptResponse {
  const lines = response.split("\n");
  
  if (lines.length === 0) {
    return {
      success: false,
      logs: [],
      result: "",
      error: "Empty response from ScriptServer",
    };
  }

  const status = lines[0].trim();
  
  if (status === "SUCCESS") {
    const logsStart = lines.findIndex((l) => l === "--- LOGS ---");
    const resultStart = lines.findIndex((l) => l === "--- RESULT ---");
    
    const logs: string[] = [];
    let result = "";
    
    if (logsStart !== -1 && resultStart !== -1) {
      for (let i = logsStart + 1; i < resultStart; i++) {
        if (lines[i]) logs.push(lines[i]);
      }
      result = lines.slice(resultStart + 1).join("\n").trim();
    } else if (resultStart !== -1) {
      result = lines.slice(resultStart + 1).join("\n").trim();
    } else {
      result = lines.slice(1).join("\n").trim();
    }
    
    return { success: true, logs, result, error: "" };
  } else if (status === "FAILED") {
    const logsStart = lines.findIndex((l) => l === "--- LOGS ---");
    const errorStart = lines.findIndex((l) => l === "--- ERROR ---");
    
    const logs: string[] = [];
    let error = "";
    
    if (logsStart !== -1 && errorStart !== -1) {
      for (let i = logsStart + 1; i < errorStart; i++) {
        if (lines[i]) logs.push(lines[i]);
      }
      error = lines.slice(errorStart + 1).join("\n").trim();
    } else if (errorStart !== -1) {
      error = lines.slice(errorStart + 1).join("\n").trim();
    } else {
      error = lines.slice(1).join("\n").trim();
    }
    
    return { success: false, logs, result: "", error };
  }
  
  return {
    success: false,
    logs: [],
    result: "",
    error: `Unexpected response format: ${status}`,
  };
}

async function main() {
  const server = new McpServer({
    name: "phoneclaw-harness",
    version: "1.0.0",
  });

  server.tool(
    "executeScript",
    `Execute a Lua script on the connected PhoneClaw device via ScriptServer.
    
The 'emu' object is automatically available with phone automation APIs:
- emu:openApp(packageName) - Open an app
- emu:clickById(id) - Click element by ID
- emu:clickByPos(x, y) - Click at position
- emu:swipe(x1, y1, x2, y2, durationMs) - Swipe gesture
- emu:inputTextById(id, text) - Input text
- emu:getCurrentWindowByAccessibilityService(...) - Get screen UI
- emu:getInstalledApps(filterPattern) - Get installed apps
- emu:waitMS(ms) - Wait for milliseconds
- emu:back(), emu:home() - Navigation

ScriptServer URL: ${SCRIPT_SERVER_URL}`,
    {
      script: z.string().describe("Lua script to execute"),
    },
    async ({ script }) => {
      const result = await executeScriptOnServer(script);
      
      let responseText = "";
      if (result.success) {
        responseText = `SUCCESS\n`;
        if (result.logs.length > 0) {
          responseText += `--- LOGS ---\n${result.logs.join("\n")}\n`;
        }
        responseText += `--- RESULT ---\n${result.result}`;
      } else {
        responseText = `FAILED\n`;
        if (result.logs.length > 0) {
          responseText += `--- LOGS ---\n${result.logs.join("\n")}\n`;
        }
        responseText += `--- ERROR ---\n${result.error}`;
      }
      
      return {
        content: [{ type: "text", text: responseText }],
      };
    }
  );

  const transport = new StdioServerTransport();
  await server.connect(transport);
  
  console.error(`PhoneClaw Harness MCP Server started`);
  console.error(`ScriptServer URL: ${SCRIPT_SERVER_URL}`);
}

main().catch((error) => {
  console.error("Fatal error:", error);
  process.exit(1);
});
