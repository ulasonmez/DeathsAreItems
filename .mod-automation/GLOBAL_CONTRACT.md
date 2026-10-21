# Global Antigravity Implementation Contract

You are executing an automated implementation step within this Minecraft mod repository.

## Non-Negotiable Operational Rules

1. **Working Directory & Boundaries**
   - The current working directory is already the repository root.
   - Never search, inspect, read, or write outside the repository root.
   - The current Git repository and source code are the absolute source of truth.

2. **Context & State**
   - Read `.mod-automation/runtime/PROJECT_STATE.md` first to understand previous architectural discoveries, decisions, and completed steps.
   - Inspect relevant existing code and `git status` before modifying any files.
   - Do not assume or hallucinate APIs or package names; verify against active code.

3. **Scope & Non-Interference**
   - Implement ONLY the scope explicitly requested by the current prompt.
   - Preserve unrelated user work and pre-existing changes.
   - Preserve previously completed features and subsystems.
   - Avoid unrelated refactors, styling rewrites, or unsolicited cleanup.
   - Do not introduce arbitrary new dependencies or mixins without explicit architectural need.

4. **Minecraft Architecture & Integrity**
   - Keep gameplay logic strictly server-authoritative.
   - Ensure all code is dedicated-server-safe (no client classes or client-only packages referenced on dedicated servers).
   - Keep client-only classes, rendering code, and event listeners strictly behind client-only registration boundaries (e.g., `Dist.CLIENT`, client events).
   - Avoid unsafe asynchronous Minecraft world access or race conditions.
   - Never produce placeholder textures, missing-texture cubes, or temporary debug assets as final output; all assets must be production-ready.
   - Remove any temporary debug logs or diagnostic code before finishing.

5. **Verification & Quality Gate**
   - Run relevant Gradle build/test commands (e.g., `./gradlew compileJava` or `./gradlew build` or repository-specific test tasks).
   - Resolve any build, compilation, or test failures introduced by your changes.
   - Run `git diff --check` and inspect `git status` to ensure zero trailing whitespace errors, merge markers, or unintentional file creations.
   - Never stage, modify, or commit automation files, runtime files (`.mod-automation/runtime/`, `.mod-automation/jobs/`), or IDE caches as product diffs.

6. **Source Control Separation**
   - The implementation conversation must NEVER execute `git add`, `git commit`, or `git push`. Source control is handled by an independent checkpoint stage.

7. **Handoff & Completion**
   - Update `.mod-automation/runtime/PROJECT_STATE.md` with compact, essential architectural notes, decisions, and remaining work.
   - Write the required step handoff summary file to the exact path specified by the runner.
   - If blocked by an external dependency, missing asset, or unsolvable conflict, clearly report the exact blocker in the handoff and `PROJECT_STATE.md` rather than pretending success.