---
agent: 'agent'
description: 'Analyze and assess the effectiveness of the GitlabFlow agent orchestration system, identify gaps, and suggest improvements.'
---

Analyze the GitlabFlow agent orchestration system in this repository.

Use repository files as the primary evidence source. When current GitHub Copilot agent behavior matters, use up-to-date GitHub documentation.

Your goals are to:

If you are asked to analize the log:

- assess the effectiveness of the current agent team and workflow
- identify strengths, gaps, and productivity bottlenecks
- pay special attention to context rot and context compaction failure modes
- recommend prompt, workflow, and control improvements that make the next iteration stronger

If you are asked to implement the improvements:

- familirie with current agentic-flow design and behavior using the logs and linked files
- plan and implement the approved improvements in the provided log file

Keep the analysis concise, structured, and grounded in the linked files. Prefer clear, human-readable explanation over academic wording. Use diagrams only when they materially improve clarity.

Review previous iterations in `/.github/agentic-flow/logs/` to understand system evolution and recurring issues.

Your response should include:

- Current gaps and issues
- Strong sides
- Improvements and how to apply them

## System Reference

Use [Agentic Flow Overview](../agentic-flow/agentic-flow-overview.md) as the primary high-level description of the custom workflow, agent responsibilities, control rules, and shared artifacts.

When answering, explain the system at a high level first, then describe each agent clearly, and keep the output traceable to the linked files.
