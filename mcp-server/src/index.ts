import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import z from 'zod';

const server = new McpServer({
  name: "GitLab Flow MCP Server",
  description: "A MCP server for Agile project management using GitLab Flow",
  version: "1.0.0",
}, {
  capabilities: {
    resources: {},
    tools: {},
    prompts: {}
  }
});

server.registerTool(
    'calculate-bmi',
    {
        title: 'BMI Calculator',
        description: 'Calculate Body Mass Index',
        inputSchema: {
            weightKg: z.number(),
            heightM: z.number()
        },
        outputSchema: { bmi: z.number() }
    },
    async ({ weightKg, heightM }) => {
        const output = { bmi: weightKg / (heightM * heightM) };
        return {
            content: [{ type: 'text', text: JSON.stringify(output) }],
            structuredContent: output
        };
    }
);


async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main();

