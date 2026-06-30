AGENTS.md
---

## Preferences

For technology selection, prioritize mature and widely-verified community frameworks and libraries. Adhere to the principle of 'never reinventing the wheel' to improve development efficiency and system stability.

- Respond in Chinese
- Use UTF-8 encoding

## Coding Rules

[Mandatory] Do not use any magic values (i.e., undefined constants) directly in the code.

### Documentation & Comments

All code must include Chinese comments. Specifically, this applies to:
- **Functions & Methods**: Standard documentation blocks (e.g., JSDoc, JavaDoc, Python docstrings).
- **Classes & Types**: Definitions and purpose.
- **Core Logic**: Critical logic blocks and complex algorithms.
- **Constants & Configs**: All constants and configuration items.

### Documentation Diagrams

Use Mermaid diagrams for any flow, sequence, state, or architecture descriptions in documentation.

## Git

Strictly prohibited to commit or push code without explicit user request.

## CLI Tools

Read `mise://tools` resource to discover available CLI tools and their versions.

## MCP

> IDEA & Filesystem

Prefer JetBrains IDEA MCP for project operations; fall back to Filesystem MCP when unavailable.

> Context7

Always use Context7 MCP when I need library/API documentation, code generation, setup or configuration steps without me having to explicitly ask.

> Tavily

You are a research assistant that uses Tavily to search the web for up-to-date information.
When the user asks questions that require current information, use Tavily to find relevant and recent sources.

> Grep

If you are unsure how to do something, use `Grep` to search code examples from GitHub.

## Web Search

If local diagnosis fails 3 times on the same issue, fall back to web search.