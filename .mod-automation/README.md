# Minecraft Mod Automation — Quick Reference & Guide

Orchestration pipeline for automated Minecraft mod development using Google Antigravity (`agy`).

---

## 🚀 Quick Start (Cheat Sheet)

### Step 1: Put Your Prompt Pack Here
Always place your generated prompt pack in this exact location:
- **Folder:** `.mod-automation/runtime/`
- **File:** `.mod-automation/runtime/PROMPT_PACK.md`

*(If the folder does not exist yet, create it: `mkdir -p .mod-automation/runtime`)*

---

### Step 2: Run the Commands

#### 🟢 1. Starting a New Mod / Feature
```bash
# Optional: Preview the execution plan without running anything
python3 .mod-automation/runner.py --new-job my-mod --dry-run

# Start execution unattended (auto-approves tool permissions):
python3 .mod-automation/runner.py --new-job my-mod --dangerously-skip-permissions
```

#### 🟡 2. Running Bug Fixes (or Polish Batches)
1. Describe your bugs to GPT-6 High using `.mod-automation/AstraPromptGenerator.md`.
2. Overwrite `.mod-automation/runtime/PROMPT_PACK.md` with the newly generated prompt pack.
3. Start a new bugfix job:
```bash
python3 .mod-automation/runner.py --new-job bugfix-batch-1 --dangerously-skip-permissions
```
*(Previous completed jobs remain safely archived in `.mod-automation/jobs/`. `PROJECT_STATE.md` automatically carries over memory).*

#### 🟠 3. Resuming After an Interruption (Ctrl+C, Sleep, Timeout)
```bash
python3 .mod-automation/runner.py --resume --dangerously-skip-permissions
```
*(Automatically detects the active job and resumes from the first incomplete prompt without repeating completed work).*

---

## 📋 Common Commands Summary

| Task | Command |
|---|---|
| **Validate / Dry-run** | `python3 .mod-automation/runner.py --new-job <slug> --dry-run` |
| **Start New Job** | `python3 .mod-automation/runner.py --new-job <slug> --dangerously-skip-permissions` |
| **Resume Active Job** | `python3 .mod-automation/runner.py --resume --dangerously-skip-permissions` |
| **Resume Specific Job ID** | `python3 .mod-automation/runner.py --job <job-id> --resume --dangerously-skip-permissions` |
| **Run Unit Tests** | `python3 .mod-automation/tests/test_runner.py` |

---

## ⚡ Execution Profiles & Quota Efficiency

Each prompt declared in `PROMPT_PACK.md` specifies an execution profile on its first line:

```text
EXECUTION_PROFILE: LOW | MEDIUM | HIGH
```

| Profile | Effort | Timeout | When to Use |
|---|---|---|---|
| **LOW** | `low` | `45m` | Recipes, loot tables, tags, lang files, simple JSON/resources |
| **MEDIUM** | `medium` | `90m` | Normal Java feature work, blocks, items, entities, GUIs, bug fixes *(Default)* |
| **HIGH** | `high` | `120m` | Complex crashes, concurrency, networking, complex worldgen |

- **Checkpoint Sessions:** Always run automatically at **LOW** effort (`45m`) regardless of the implementation prompt's profile.
- **Model:** By default uses `model: null` (Antigravity CLI's default configured model, e.g. Gemini 3.8 Flash).

---

## 📂 File & Folder Structure

```text
.mod-automation/
├── runtime/
│   ├── PROMPT_PACK.md          <-- [INBOX] Paste new prompt packs here
│   ├── PROJECT_STATE.md        <-- Compact living project memory (auto-updated)
│   └── active_job.json         <-- Pointer to currently active job
│
├── jobs/                       <-- Historical archive of all executed jobs
│   └── <timestamp>-<slug>/
│       ├── logs/               <-- Real-time CLI stream logs
│       ├── handoffs/           <-- Per-step implementation summaries
│       └── results/            <-- Checkpoint validation decisions (JSON)
│
├── AstraPromptGenerator.md     <-- Master prompt to generate PROMPT_PACK.md
├── GLOBAL_CONTRACT.md          <-- Shared rules applied to every implementation turn
├── CHECKPOINT_PROMPT.md        <-- Rules for automated review & verification turns
├── config.json                 <-- Configuration (profiles, timeouts, size limits)
└── runner.py                   <-- Orchestration script
```

---

## 🔧 Useful Tips & Troubleshooting

1. **Watch Live Logs in Another Terminal**:
   ```bash
   tail -f .mod-automation/jobs/<active-job-id>/logs/*.ndjson
   ```

2. **Upstream Git Branch Check**:
   If push is enabled in `config.json`, make sure your current branch has an upstream set:
   ```bash
   git push -u origin <current-branch>
   ```

3. **Audit Step (Prompt 1)**:
   Prompt 1 is strictly for discovery and planning. The runner will verify no product code was changed and skip committing for Prompt 1.

4. **Checkpoint Blocked**:
   If a checkpoint reports `BLOCKED`, inspect the result JSON in `.mod-automation/jobs/<job-id>/results/`, fix the issue, and resume with `--resume`.