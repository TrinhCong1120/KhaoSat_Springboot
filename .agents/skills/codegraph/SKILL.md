---
name: codegraph
description: Initialize and use CodeGraph — pre-indexed knowledge graph for 92% faster code exploration. Auto-init when .codegraph/ missing. Use when exploring unfamiliar codebases, tracing call flows, or before editing.
---

# CodeGraph Skill

Pre-indexed code knowledge graph tool designed to optimize code exploration by replacing traditional grep/glob/read exploration loops with fast, structural graph queries.

## When to Use

- Exploring unfamiliar codebases or large repositories
- Tracing function call flows (upstream callers / downstream callees)
- Impact analysis and blast radius checks before refactoring or editing code
- Finding symbols, definitions, usages, and implementations across projects

## 1. Initialization & Setup (Auto-Init Workflow)

When entering a project that lacks a `.codegraph/` directory:

1. **Check Installation:**
   ```bash
   codegraph --version
   ```
   If not installed, install globally via npm:
   ```bash
   npm install -g @colbymchenry/codegraph
   ```

2. **Initialize and Index the Project:**
   ```bash
   codegraph init -i
   ```

3. **Verify the Status:**
   ```bash
   codegraph status
   ```
   *Expected output:* `Files: N, Nodes: N, Backend: native`

4. **Troubleshooting Backend Performance:**
   If the output shows `Backend: wasm` (slower), rebuild the native module:
   ```bash
   npm rebuild better-sqlite3
   ```

## 2. Using CodeGraph for Code Exploration

Once the `.codegraph/` repository index exists, prioritize CodeGraph tools or CLI commands over raw file reading:

| Tool / CLI Command | Primary Purpose |
| :--- | :--- |
| `codegraph explore <query>` / `codegraph_explore` | **Primary exploration** — returns full source code from all relevant files in a single unified call. |
| `codegraph search <name>` / `codegraph_search` | Find symbols, methods, classes, or variables by name. |
| `codegraph callers <symbol>` / `callees` | Trace execution call flows upstream or downstream. |
| `codegraph impact <symbol>` / `codegraph_impact` | Run a blast radius check to see what components/tests are affected before editing. |
| `codegraph node <symbol>` / `codegraph_node` | Retrieve deep metadata and details for a single code symbol. |

## 3. Core Operational Rules for AI Agents

- **Primary Directive:** When a project has CodeGraph initialized (`.codegraph/`), use `codegraph_explore` / `codegraph explore` as your PRIMARY exploration tool.
- **Avoid Redundancy:** NEVER re-read or fetch files via standard file-read tools if CodeGraph has already supplied the source code in its payload.
- **Hierarchy of Exploration:** CodeGraph Graph Queries > Grep / Glob / File Read.

