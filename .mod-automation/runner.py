#!/usr/bin/env python3
"""
Minecraft Mod Automation Infrastructure — Standardized Pipeline Runner

Generic, multi-stage, recoverable orchestration runner for automated
Minecraft mod development using Google Antigravity (agy).
"""

import argparse
from dataclasses import dataclass
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import time
from typing import Any, Dict, List, Optional, Tuple

# Global reference to currently executing subprocess for clean signal handling
_ACTIVE_SUBPROCESS: Optional[subprocess.Popen] = None


def _handle_signal(signum: int, frame: Any) -> None:
    """Gracefully terminate active child process on interruption and preserve state."""
    global _ACTIVE_SUBPROCESS
    sig_name = signal.Signals(signum).name
    print(f"\n\n[Runner] Interrupted by {sig_name}. Preserving state...", file=sys.stderr)
    if _ACTIVE_SUBPROCESS and _ACTIVE_SUBPROCESS.poll() is None:
        try:
            print("[Runner] Terminating active Antigravity session...", file=sys.stderr)
            _ACTIVE_SUBPROCESS.terminate()
            _ACTIVE_SUBPROCESS.wait(timeout=3)
        except Exception:
            try:
                _ACTIVE_SUBPROCESS.kill()
            except Exception:
                pass
    print("[Runner] State safely preserved. To resume execution, run:", file=sys.stderr)
    print("  python3 .mod-automation/runner.py --resume\n", file=sys.stderr)
    sys.exit(128 + signum)


signal.signal(signal.SIGINT, _handle_signal)
signal.signal(signal.SIGTERM, _handle_signal)


# ---------------------------------------------------------------------------
# Utility & Helper Functions
# ---------------------------------------------------------------------------

def parse_timeout(timeout_val: Any) -> int:
    """Parse timeout representations (e.g. '90m', '1h', '300s', 5400) to seconds."""
    if isinstance(timeout_val, (int, float)):
        return int(timeout_val)
    s = str(timeout_val).strip().lower()
    if not s:
        return 5400  # 90 minutes default
    match = re.match(r"^(\d+)\s*(s|m|h)?$", s)
    if not match:
        raise ValueError(f"Invalid timeout format: '{timeout_val}'. Expected e.g. '90m', '1h', '300s'.")
    val, unit = int(match.group(1)), match.group(2)
    if unit == "h":
        return val * 3600
    elif unit == "m":
        return val * 60
    else:
        return val


def slugify(text: str) -> str:
    """Convert a human-readable title into a safe filename slug."""
    slug = re.sub(r"[^a-zA-Z0-9]+", "-", text.strip().lower()).strip("-")
    return slug or "step"


def compute_sha256(content: str) -> str:
    """Calculate the SHA-256 hex digest of a string."""
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def atomic_write_text(path: Path, content: str) -> None:
    """Write text to a file atomically via a temporary file."""
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_suffix(f"{path.suffix}.tmp.{os.getpid()}")
    try:
        with open(tmp_path, "w", encoding="utf-8") as f:
            f.write(content)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp_path, path)
    finally:
        if tmp_path.exists():
            try:
                tmp_path.unlink()
            except Exception:
                pass


def atomic_write_json(path: Path, data: Any) -> None:
    """Write formatted JSON to a file atomically."""
    formatted = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    atomic_write_text(path, formatted)


# ---------------------------------------------------------------------------
# Prompt Pack Data Structures & Parsing
# ---------------------------------------------------------------------------

@dataclass
class Prompt:
    number: int
    title: str
    body: str
    execution_profile: str = "MEDIUM"

    @property
    def safe_filename(self) -> str:
        return f"{self.number:02d}-{slugify(self.title)}"


def parse_prompt_pack(markdown_text: str, default_profile: str = "MEDIUM") -> List[Prompt]:
    """
    Parse prompt pack markdown into sequential Prompts.
    Headings must match: # Prompt N — <title>
    Prompt bodies must be contained within ```text fenced blocks.
    Strictly validates sequential numbering starting at 1.
    Extracts optional EXECUTION_PROFILE from the first non-empty line of the body.
    """
    lines = markdown_text.splitlines()
    # Matches '# Prompt 1 — Title' or '# Prompt 2 - Title' or '# Prompt 3 -- Title'
    heading_regex = re.compile(r"^#\s+Prompt\s+(\d+)\s*(?:[\u2014\u2013]|\-{1,2})\s*(.+)$")

    prompt_headers: List[Tuple[int, int, str]] = []  # (line_idx, prompt_num, title)
    for idx, line in enumerate(lines):
        match = heading_regex.match(line.strip())
        if match:
            p_num = int(match.group(1))
            p_title = match.group(2).strip()
            prompt_headers.append((idx, p_num, p_title))

    if not prompt_headers:
        raise ValueError(
            "No prompt headings found in PROMPT_PACK.md! "
            "Headings must follow the format: '# Prompt N — <title>'."
        )

    # Validate numbering sequence: must start at 1 and increment by 1
    for expected_idx, (_, p_num, p_title) in enumerate(prompt_headers, start=1):
        if p_num != expected_idx:
            raise ValueError(
                f"Prompt numbering gap or mismatch: found Prompt {p_num} ('{p_title}'), "
                f"but expected Prompt {expected_idx}. Prompts must be sequential starting at 1."
            )

    prompts: List[Prompt] = []
    for i, (start_idx, p_num, p_title) in enumerate(prompt_headers):
        end_idx = prompt_headers[i + 1][0] if i + 1 < len(prompt_headers) else len(lines)
        section_lines = lines[start_idx + 1:end_idx]
        section_text = "\n".join(section_lines)

        # Extract ```text block
        code_block_match = re.search(r"```text\s*\n(.*?)\n```", section_text, re.DOTALL)
        if not code_block_match:
            # Check if there is an unclosed or missing block
            if "```text" in section_text:
                raise ValueError(
                    f"Prompt {p_num} ('{p_title}') contains an unclosed '```text' block."
                )
            else:
                raise ValueError(
                    f"Prompt {p_num} ('{p_title}') is missing the required '```text' fenced block for its prompt body."
                )

        raw_body = code_block_match.group(1)
        body_lines = raw_body.splitlines()

        # Find first non-empty line
        first_non_empty_idx = None
        for line_idx, line in enumerate(body_lines):
            if line.strip():
                first_non_empty_idx = line_idx
                break

        if first_non_empty_idx is None:
            raise ValueError(f"Prompt {p_num} ('{p_title}') has an empty prompt body.")

        first_line = body_lines[first_non_empty_idx].strip()
        profile_match = re.match(r"^EXECUTION_PROFILE\s*:\s*(.+)$", first_line, re.IGNORECASE)

        if profile_match:
            raw_val = profile_match.group(1).strip()
            norm_val = raw_val.upper()
            if norm_val not in ("LOW", "MEDIUM", "HIGH"):
                raise ValueError(
                    f"Prompt {p_num} ('{p_title}') specifies an invalid EXECUTION_PROFILE: '{raw_val}'. "
                    f"Supported values are: LOW, MEDIUM, HIGH."
                )
            execution_profile = norm_val
            # Remove that metadata line
            remaining_lines = body_lines[:first_non_empty_idx] + body_lines[first_non_empty_idx + 1:]
            body = "\n".join(remaining_lines).strip()
            if not body:
                raise ValueError(
                    f"Prompt {p_num} ('{p_title}') has an empty prompt body after removing EXECUTION_PROFILE."
                )
        else:
            execution_profile = str(default_profile).strip().upper()
            body = "\n".join(body_lines).strip()
            if not body:
                raise ValueError(f"Prompt {p_num} ('{p_title}') has an empty prompt body.")

        prompts.append(Prompt(
            number=p_num,
            title=p_title,
            body=body,
            execution_profile=execution_profile
        ))

    return prompts


# ---------------------------------------------------------------------------
# Git Operations
# ---------------------------------------------------------------------------

class GitManager:
    """Safe Git wrapper using explicit argument lists (no shell=True)."""

    def __init__(self, repo_root: Path):
        self.repo_root = repo_root

    def _run(self, args: List[str], check: bool = True) -> subprocess.CompletedProcess:
        cmd = ["git"] + args
        res = subprocess.run(
            cmd,
            cwd=str(self.repo_root),
            capture_output=True,
            text=True,
            check=False
        )
        if check and res.returncode != 0:
            err = res.stderr.strip() or res.stdout.strip()
            raise RuntimeError(f"Git command failed: {' '.join(cmd)}\nError: {err}")
        return res

    def is_repo(self) -> bool:
        res = self._run(["rev-parse", "--is-inside-work-tree"], check=False)
        return res.returncode == 0 and res.stdout.strip() == "true"

    def get_toplevel(self) -> Path:
        res = self._run(["rev-parse", "--show-toplevel"])
        return Path(res.stdout.strip()).resolve()

    def get_current_branch(self) -> str:
        res = self._run(["symbolic-ref", "-q", "--short", "HEAD"], check=False)
        if res.returncode != 0:
            raise RuntimeError(
                "Current branch is detached! Mod automation requires a named branch (e.g. 'main' or a feature branch)."
            )
        return res.stdout.strip()

    def get_upstream_branch(self) -> Optional[str]:
        res = self._run(["rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}"], check=False)
        if res.returncode == 0:
            return res.stdout.strip()
        return None

    def get_status_porcelain(self) -> str:
        res = self._run(["status", "--porcelain"])
        return res.stdout

    def run_diff_check(self) -> bool:
        res = self._run(["diff", "--check"], check=False)
        return res.returncode == 0

    def check_ignored(self, rel_path: str) -> bool:
        res = self._run(["check-ignore", "-q", rel_path], check=False)
        return res.returncode == 0

    def add_files(self, rel_paths: List[str]) -> None:
        """Stage explicitly provided relative file paths (never git add .)."""
        if not rel_paths:
            return
        # Reject any path traversing outside or into .mod-automation
        for p in rel_paths:
            if p.startswith(".mod-automation") or "/.mod-automation" in p:
                raise ValueError(f"Security error: attempted to git add automation path: {p}")
        self._run(["add", "--"] + rel_paths)

    def get_staged_files(self) -> List[str]:
        res = self._run(["diff", "--cached", "--name-only"])
        lines = [line.strip() for line in res.stdout.splitlines() if line.strip()]
        return lines

    def get_staged_stat(self) -> str:
        res = self._run(["diff", "--cached", "--stat"])
        return res.stdout.strip()

    def commit(self, message: str) -> str:
        """Commit staged changes and return the new commit hash."""
        self._run(["commit", "-m", message])
        res = self._run(["rev-parse", "HEAD"])
        return res.stdout.strip()

    def push(self) -> None:
        """Push current branch to its configured upstream."""
        self._run(["push"])

    def get_deleted_files_in_status(self) -> List[str]:
        """Return files that were deleted from git according to git status."""
        deleted = []
        for line in self.get_status_porcelain().splitlines():
            # Porcelain format: ' D file', 'D  file', 'MD file', etc.
            status_code = line[:2]
            filename = line[3:].strip()
            if "D" in status_code:
                deleted.append(filename)
        return deleted


# ---------------------------------------------------------------------------
# Path Security & File Validation
# ---------------------------------------------------------------------------

def validate_files_to_commit(
    repo_root: Path,
    files_to_commit: List[str],
    allowed_deletions: List[str]
) -> List[str]:
    """
    Validate that every path in files_to_commit is safe, relative,
    does not touch .mod-automation, and exists on disk or is a tracked deletion.
    """
    if not files_to_commit:
        raise ValueError("files_to_commit list cannot be empty for READY_TO_COMMIT status.")

    validated: List[str] = []
    root_resolved = repo_root.resolve()

    for item in files_to_commit:
        if not isinstance(item, str):
            raise ValueError(f"Invalid non-string path in files_to_commit: {item}")
        rel_str = item.strip().replace("\\", "/")
        if not rel_str:
            continue

        if os.path.isabs(rel_str) or rel_str.startswith("/"):
            raise ValueError(f"Security error: absolute path in files_to_commit: '{rel_str}'")

        parts = rel_str.split("/")
        if ".." in parts:
            raise ValueError(f"Security error: directory traversal in files_to_commit: '{rel_str}'")

        if parts[0] == ".mod-automation" or any(p == ".mod-automation" for p in parts):
            raise ValueError(
                f"Security error: .mod-automation path in files_to_commit: '{rel_str}'. "
                "Automation infrastructure must never be committed in product diffs."
            )

        full_path = (repo_root / rel_str).resolve()
        try:
            full_path.relative_to(root_resolved)
        except ValueError:
            raise ValueError(f"Security error: path resolves outside repo root: '{rel_str}'")

        # Check existence or deletion
        if not full_path.exists():
            if rel_str not in allowed_deletions:
                raise ValueError(
                    f"File in files_to_commit does not exist on disk: '{rel_str}' "
                    "(and is not an active git deletion)."
                )

        if full_path.is_dir():
            raise ValueError(
                f"Invalid path in files_to_commit: '{rel_str}' is a directory. "
                "Only specific files may be staged."
            )

        validated.append(rel_str)

    if not validated:
        raise ValueError("No valid files remained in files_to_commit.")

    return validated


# ---------------------------------------------------------------------------
# Execution Profiles & Project State Monitoring
# ---------------------------------------------------------------------------

@dataclass
class ResolvedProfile:
    name: str
    model: Optional[str]
    effort: str
    timeout_seconds: int
    raw_timeout: str


def resolve_execution_profile(profile_name: str, config: Dict[str, Any]) -> ResolvedProfile:
    """
    Resolve and validate an execution profile against runner configuration.
    Returns a ResolvedProfile with normalized name, model (or None), effort, and timeout.
    """
    if not profile_name:
        raise ValueError("Execution profile name cannot be empty.")

    norm_name = str(profile_name).strip().upper()
    profiles = config.get("execution_profiles", {})

    if norm_name not in profiles:
        available = ", ".join(sorted(profiles.keys())) if profiles else "none"
        raise ValueError(
            f"Unknown execution profile '{profile_name}'. Configured profiles in config.json: {available}"
        )

    prof_conf = profiles[norm_name]
    if not isinstance(prof_conf, dict):
        raise ValueError(f"Configuration for execution profile '{norm_name}' must be an object/dict.")

    # Validate effort: exactly one of low, medium, high
    raw_effort = prof_conf.get("effort")
    if not raw_effort:
        raise ValueError(f"Execution profile '{norm_name}' is missing required 'effort' field.")
    effort = str(raw_effort).strip().lower()
    if effort not in ("low", "medium", "high"):
        raise ValueError(
            f"Invalid effort '{raw_effort}' for execution profile '{norm_name}'. "
            "Must be one of: low, medium, high."
        )

    # Validate timeout
    raw_timeout = prof_conf.get("timeout")
    if not raw_timeout:
        raw_timeout = config.get("antigravity_timeout", "90m")
    timeout_seconds = parse_timeout(raw_timeout)

    # Validate model
    raw_model = prof_conf.get("model")
    if raw_model is None:
        model = None
    else:
        model_str = str(raw_model).strip()
        model = model_str if model_str else None

    return ResolvedProfile(
        name=norm_name,
        model=model,
        effort=effort,
        timeout_seconds=timeout_seconds,
        raw_timeout=str(raw_timeout)
    )


def check_project_state_size(project_state_path: Path, config: Dict[str, Any]) -> Optional[str]:
    """
    Check the file size of PROJECT_STATE.md and return an advisory warning string if above thresholds.
    Does not truncate or block execution.
    """
    if not project_state_path.exists():
        return None

    size_bytes = project_state_path.stat().st_size
    size_kib = size_bytes / 1024.0

    warning_threshold = config.get("project_state_warning_bytes", 15360)
    strong_warning_threshold = config.get("project_state_strong_warning_bytes", 25600)

    if size_bytes >= strong_warning_threshold:
        return (
            f"WARNING: PROJECT_STATE.md is {size_kib:.1f} KiB.\n"
            "Persistent project context is becoming large and may increase agent quota usage.\n"
            "Compact stale historical detail into handoff files."
        )
    elif size_bytes >= warning_threshold:
        return (
            f"WARNING: PROJECT_STATE.md is {size_kib:.1f} KiB.\n"
            "It is intended to be compact persistent context.\n"
            "Consider compacting stale history."
        )
    return None


# ---------------------------------------------------------------------------
# Antigravity Headless Runner
# ---------------------------------------------------------------------------

class AntigravitySession:
    """Executes single-turn headless Antigravity prompts with logging and error checking."""

    def __init__(
        self,
        repo_root: Path,
        timeout_seconds: int,
        auto_approve: bool = False
    ):
        self.repo_root = repo_root
        self.timeout_seconds = timeout_seconds
        self.auto_approve = auto_approve

    def run_prompt(
        self,
        prompt_text: str,
        log_path: Path,
        session_label: str = "SESSION",
        effort: Optional[str] = None,
        model: Optional[str] = None,
        timeout_seconds: Optional[int] = None,
        timeout_str: Optional[str] = None
    ) -> bool:
        """
        Run a single prompt in a fresh non-interactive `agy` process.
        Saves full stream output line-by-line to log_path.
        Returns True on success, False on any failure or permission denial.
        """
        global _ACTIVE_SUBPROCESS
        log_path.parent.mkdir(parents=True, exist_ok=True)

        effective_timeout_sec = timeout_seconds if timeout_seconds is not None else self.timeout_seconds
        timeout_arg = timeout_str if timeout_str else f"{effective_timeout_sec}s"

        cmd = [
            "agy",
            "-p", prompt_text,
            "--output-format", "stream-json",
            "--print-timeout", timeout_arg
        ]
        if effort:
            cmd.extend(["--effort", effort])
        if model:
            cmd.extend(["--model", model])
        if self.auto_approve:
            cmd.append("--dangerously-skip-permissions")

        had_error = False
        denied_action_detected = False
        saw_result_success = False

        start_time = time.time()

        try:
            with open(log_path, "w", encoding="utf-8") as log_file:
                proc = subprocess.Popen(
                    cmd,
                    cwd=str(self.repo_root),
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1
                )
                _ACTIVE_SUBPROCESS = proc

                # Stream and analyze events in real-time
                if proc.stdout:
                    for raw_line in iter(proc.stdout.readline, ""):
                        log_file.write(raw_line)
                        log_file.flush()

                        line_str = raw_line.strip()
                        if not line_str:
                            continue

                        # Check for JSON stream events
                        try:
                            event = json.loads(line_str)
                            # Check for permission denial
                            if event.get("denied_actions") or event.get("permission_denied"):
                                denied_action_detected = True
                                print(f"  [!] Permission denial detected in {session_label} stream.", file=sys.stderr)
                            # Check for explicit error event
                            if event.get("type") == "error" or event.get("status") in ("ERROR", "BLOCKED"):
                                had_error = True
                            # Check for explicit success completion event
                            if event.get("status") == "SUCCESS" or (
                                event.get("type") == "result" and event.get("status") != "ERROR"
                            ):
                                saw_result_success = True
                        except json.JSONDecodeError:
                            # Not an event line or mixed text
                            if "permission denied" in line_str.lower() or "denied_actions" in line_str:
                                denied_action_detected = True
                            if "fatal:" in line_str.lower() or "error:" in line_str.lower():
                                pass  # log it, let returncode decide

                returncode = proc.wait(timeout=effective_timeout_sec)
                _ACTIVE_SUBPROCESS = None

        except subprocess.TimeoutExpired:
            print(f"\n[Runner] {session_label} timed out after {effective_timeout_sec}s!", file=sys.stderr)
            if _ACTIVE_SUBPROCESS:
                _ACTIVE_SUBPROCESS.kill()
                _ACTIVE_SUBPROCESS = None
            return False
        except Exception as ex:
            print(f"\n[Runner] Failed to execute {session_label}: {ex}", file=sys.stderr)
            _ACTIVE_SUBPROCESS = None
            return False

        duration = int(time.time() - start_time)

        if returncode != 0:
            print(f"  [X] {session_label} process exited with code {returncode} (duration: {duration}s).", file=sys.stderr)
            return False

        if denied_action_detected:
            print(f"  [X] {session_label} failed due to action/tool permission denial.", file=sys.stderr)
            return False

        if had_error:
            print(f"  [X] {session_label} reported an error status in stream events.", file=sys.stderr)
            return False

        print(f"  [OK] {session_label} completed successfully ({duration}s).")
        return True


# ---------------------------------------------------------------------------
# Job Orchestration & State Management
# ---------------------------------------------------------------------------

class JobManager:
    """Manages the full lifecycle of a mod automation job."""

    def __init__(self, repo_root: Path, config_path: Optional[Path] = None):
        self.repo_root = repo_root
        self.git = GitManager(repo_root)

        # Locate and load config.json
        if config_path:
            self.config_path = config_path.resolve()
        else:
            self.config_path = (repo_root / ".mod-automation" / "config.json").resolve()

        if not self.config_path.exists():
            raise FileNotFoundError(f"Configuration file not found: {self.config_path}")

        with open(self.config_path, "r", encoding="utf-8") as f:
            self.config: Dict[str, Any] = json.load(f)

        # Resolve config paths relative to repo root
        self.inbox_prompt_pack = (self.repo_root / self.config["inbox_prompt_pack"]).resolve()
        self.project_state_file = (self.repo_root / self.config["project_state"]).resolve()
        self.jobs_dir = (self.repo_root / self.config["jobs_directory"]).resolve()
        self.global_contract_file = (self.repo_root / self.config["global_contract"]).resolve()
        self.checkpoint_prompt_file = (self.repo_root / self.config["checkpoint_prompt"]).resolve()
        self.project_state_template = (self.repo_root / self.config["project_state_template"]).resolve()
        self.state_template_file = (self.repo_root / self.config["state_template"]).resolve()

        self.commit_after_each = self.config.get("commit_after_each_implementation_prompt", True)
        self.push_after_each = self.config.get("push_after_each_commit", True)
        self.skip_commit_p1 = self.config.get("skip_commit_for_prompt_1_audit", True)
        self.timeout_seconds = parse_timeout(self.config.get("antigravity_timeout", "90m"))
        self.auto_approve = self.config.get("auto_approve_tool_permissions", False)

        self.default_profile_name = self.config.get("default_execution_profile", "MEDIUM")
        self.checkpoint_profile_name = self.config.get("checkpoint_execution_profile", "LOW")
        # Validate profile configuration early
        resolve_execution_profile(self.default_profile_name, self.config)
        resolve_execution_profile(self.checkpoint_profile_name, self.config)

        self.active_job_marker = (self.repo_root / ".mod-automation" / "runtime" / "active_job.json").resolve()

    def ensure_runtime_initialized(self) -> None:
        """Initialize .mod-automation/runtime/PROJECT_STATE.md if missing."""
        runtime_dir = self.repo_root / ".mod-automation" / "runtime"
        runtime_dir.mkdir(parents=True, exist_ok=True)
        if not self.project_state_file.exists():
            if not self.project_state_template.exists():
                raise FileNotFoundError(f"PROJECT_STATE template missing: {self.project_state_template}")
            content = self.project_state_template.read_text(encoding="utf-8")
            atomic_write_text(self.project_state_file, content)
            print(f"[Runner] Initialized {self.project_state_file.relative_to(self.repo_root)} from template.")

    def run_preflight_checks(self) -> None:
        """Run environment preflight checks."""
        # 1. agy check
        if not shutil.which("agy"):
            raise RuntimeError(
                "Antigravity CLI ('agy') is not found in PATH! "
                "Ensure Antigravity is installed and accessible."
            )

        # 2. git check
        if not shutil.which("git"):
            raise RuntimeError("Git CLI ('git') is not found in PATH!")

        # 3. repository check
        if not self.git.is_repo():
            raise RuntimeError(f"Current directory is not a Git repository: {self.repo_root}")

        actual_toplevel = self.git.get_toplevel()
        if actual_toplevel != self.repo_root.resolve():
            raise RuntimeError(
                f"Repository root mismatch: expected {self.repo_root.resolve()}, "
                f"but git reported {actual_toplevel}"
            )

        # 4. branch check
        current_branch = self.git.get_current_branch()

        # 5. upstream check if push is enabled
        if self.push_after_each:
            upstream = self.git.get_upstream_branch()
            if not upstream:
                raise RuntimeError(
                    f"Current branch '{current_branch}' has no configured upstream tracking branch.\n"
                    f"Push is enabled in config.json. Please set an upstream branch first using:\n"
                    f"  git push -u origin {current_branch}"
                )

        # 6. gitignore check for runtime paths
        runtime_sample = ".mod-automation/runtime/PROMPT_PACK.md"
        jobs_sample = ".mod-automation/jobs/test/state.json"
        if not self.git.check_ignored(runtime_sample):
            print(
                f"[WARNING] '{runtime_sample}' does not appear to be ignored by Git! "
                "Check that .mod-automation/runtime/ is in .gitignore.",
                file=sys.stderr
            )
        if not self.git.check_ignored(jobs_sample):
            print(
                f"[WARNING] '{jobs_sample}' does not appear to be ignored by Git! "
                "Check that .mod-automation/jobs/ is in .gitignore.",
                file=sys.stderr
            )

    def create_new_job(self, job_name: str) -> str:
        """Create a new job directory, archive inbox PROMPT_PACK.md, and initialize state."""
        safe_name = slugify(job_name)
        if not safe_name:
            raise ValueError(f"Invalid job name: '{job_name}'. Must contain alphanumeric characters.")

        if not self.inbox_prompt_pack.exists():
            raise FileNotFoundError(
                f"Inbox prompt pack missing: {self.inbox_prompt_pack}.\n"
                f"Please place your generated PROMPT_PACK.md at this path before starting a new job."
            )

        pack_text = self.inbox_prompt_pack.read_text(encoding="utf-8")
        prompts = parse_prompt_pack(pack_text, default_profile=self.default_profile_name)
        pack_sha = compute_sha256(pack_text)

        timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        job_id = f"{timestamp}-{safe_name}"
        job_dir = self.jobs_dir / job_id

        if job_dir.exists():
            raise FileExistsError(f"Job directory already exists: {job_dir}")

        # Create job subdirectories
        (job_dir / "handoffs").mkdir(parents=True, exist_ok=True)
        (job_dir / "results").mkdir(parents=True, exist_ok=True)
        (job_dir / "logs").mkdir(parents=True, exist_ok=True)

        # Archive the prompt pack
        archived_pack = job_dir / "PROMPT_PACK.md"
        archived_pack.write_text(pack_text, encoding="utf-8")

        # Create initial state.json
        state_data: Dict[str, Any] = {
            "job_id": job_id,
            "job_name": safe_name,
            "prompt_pack_sha256": pack_sha,
            "total_prompts": len(prompts),
            "next_prompt_index": 0,  # 0-indexed: Prompt 1 corresponds to index 0
            "completed_prompts": [],
            "current_stage": None,
            "last_status": None,
            "last_commit": None,
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }
        atomic_write_json(job_dir / "state.json", state_data)

        # Update active job pointer
        active_data = {
            "active_job_id": job_id,
            "updated_at": datetime.now().isoformat()
        }
        atomic_write_json(self.active_job_marker, active_data)

        print(f"[Runner] Created new job: {job_id}")
        print(f"         Total prompts detected: {len(prompts)}")
        print(f"         Job path: {job_dir.relative_to(self.repo_root)}")
        return job_id

    def load_job_state(self, job_id: str) -> Tuple[Dict[str, Any], Path, List[Prompt]]:
        """Load and validate the state of an existing job."""
        job_dir = self.jobs_dir / job_id
        if not job_dir.exists():
            raise FileNotFoundError(f"Job directory does not exist: {job_dir}")

        state_path = job_dir / "state.json"
        if not state_path.exists():
            raise FileNotFoundError(f"Job state file missing: {state_path}")

        with open(state_path, "r", encoding="utf-8") as f:
            state: Dict[str, Any] = json.load(f)

        archived_pack = job_dir / "PROMPT_PACK.md"
        if not archived_pack.exists():
            raise FileNotFoundError(f"Archived PROMPT_PACK.md missing: {archived_pack}")

        pack_text = archived_pack.read_text(encoding="utf-8")
        current_sha = compute_sha256(pack_text)

        # Safety verification: ensure prompt pack was not tampered with after job creation
        if state.get("prompt_pack_sha256") and current_sha != state["prompt_pack_sha256"]:
            raise RuntimeError(
                f"Integrity check failed for job '{job_id}'!\n"
                f"Archived PROMPT_PACK.md SHA-256 has changed since job initialization.\n"
                f"Expected: {state.get('prompt_pack_sha256')}\n"
                f"Actual:   {current_sha}\n"
                "Refusing to continue silently. Start a new job for modified prompt packs."
            )

        prompts = parse_prompt_pack(pack_text, default_profile=self.default_profile_name)
        return state, job_dir, prompts

    def get_active_job_id(self) -> Optional[str]:
        """Read the currently active job ID from runtime marker."""
        if not self.active_job_marker.exists():
            return None
        try:
            with open(self.active_job_marker, "r", encoding="utf-8") as f:
                data = json.load(f)
                return data.get("active_job_id")
        except Exception:
            return None

    def execute_dry_run(self, job_name: Optional[str] = None, job_id: Optional[str] = None) -> None:
        """Perform a non-destructive dry run showing prompt layout and execution plan."""
        print("=" * 70)
        print(" MINECRAFT MOD AUTOMATION — DRY RUN VALIDATION")
        print("=" * 70)

        # Preflight checks
        current_branch = self.git.get_current_branch()
        upstream = self.git.get_upstream_branch()
        print(f"Repository Root : {self.repo_root}")
        print(f"Current Branch  : {current_branch} (upstream: {upstream or 'None'})")
        print(f"Project State   : {'Exists' if self.project_state_file.exists() else 'Will initialize from template'}")

        if job_id:
            state, job_dir, prompts = self.load_job_state(job_id)
            print(f"Target Job      : {job_id} (resuming)")
            start_index = state.get("next_prompt_index", 0)
        else:
            if not self.inbox_prompt_pack.exists():
                raise FileNotFoundError(f"Cannot dry-run: inbox prompt pack not found at {self.inbox_prompt_pack}")
            pack_text = self.inbox_prompt_pack.read_text(encoding="utf-8")
            prompts = parse_prompt_pack(pack_text, default_profile=self.default_profile_name)
            start_index = 0
            slug = slugify(job_name or "dry-run-job")
            print(f"Target Job      : [New Job: YYYYMMDD-HHMMSS-{slug}]")

        chk_profile = resolve_execution_profile(self.checkpoint_profile_name, self.config)

        if self.project_state_file.exists():
            ps_size_bytes = self.project_state_file.stat().st_size
            ps_size_str = f"{ps_size_bytes / 1024.0:.1f} KiB"
        else:
            ps_size_str = "Will initialize from template"

        print(f"Total Prompts   : {len(prompts)}")
        print(f"CHECKPOINT PROFILE: {chk_profile.name}")
        print(f"CHECKPOINT MODEL  : {chk_profile.model or 'CLI DEFAULT'}")
        print(f"CHECKPOINT EFFORT : {chk_profile.effort}")
        print(f"PROJECT_STATE     : {ps_size_str}")

        size_warning = check_project_state_size(self.project_state_file, self.config)
        if size_warning:
            print(f"\n{size_warning}")

        print("\nExecution Plan:")
        print("-" * 70)

        for idx, p in enumerate(prompts):
            p_profile = resolve_execution_profile(p.execution_profile, self.config)
            is_audit = (p.number == 1 and self.skip_commit_p1)
            commit_action = "SKIP COMMIT (Audit Step)" if is_audit else "RUN CHECKPOINT & COMMIT"
            commit_yn = "no" if is_audit else ("yes" if self.commit_after_each else "no")
            status_tag = "COMPLETED" if idx < start_index else ("NEXT" if idx == start_index else "PENDING")
            print(f"  [{status_tag:^9}] Prompt {p.number:02d}: {p.title}")
            print(f"              profile  : {p_profile.name}")
            print(f"              model    : {p_profile.model or 'CLI DEFAULT'}")
            print(f"              effort   : {p_profile.effort}")
            print(f"              timeout  : {p_profile.raw_timeout}")
            print(f"              commit   : {commit_yn}")
            print(f"              Action   : {commit_action}")
            print(f"              Handoff  : handoffs/{p.safe_filename}.md")
            if not is_audit:
                print(f"              Result   : results/{p.safe_filename}.json")

        print("-" * 70)
        print("DRY RUN COMPLETE — No Antigravity sessions launched. No Git commits made.")
        print("=" * 70)

    def run_job(self, job_id: str) -> None:
        """Run the automation pipeline for the specified job."""
        self.ensure_runtime_initialized()
        self.run_preflight_checks()

        state, job_dir, prompts = self.load_job_state(job_id)
        start_index = state.get("next_prompt_index", 0)

        if start_index >= len(prompts):
            print(f"\n[Runner] Job '{job_id}' is already fully completed! ({len(prompts)}/{len(prompts)} prompts done).")
            return

        session_runner = AntigravitySession(
            repo_root=self.repo_root,
            timeout_seconds=self.timeout_seconds,
            auto_approve=self.auto_approve
        )

        global_contract = self.global_contract_file.read_text(encoding="utf-8")

        for idx in range(start_index, len(prompts)):
            prompt = prompts[idx]
            prompt_num = prompt.number
            total = len(prompts)
            is_audit_step = (prompt_num == 1 and self.skip_commit_p1)

            handoff_rel = f".mod-automation/jobs/{job_id}/handoffs/{prompt.safe_filename}.md"
            handoff_file = self.repo_root / handoff_rel
            impl_log_rel = f".mod-automation/jobs/{job_id}/logs/{prompt.safe_filename}-impl.ndjson"
            impl_log_file = self.repo_root / impl_log_rel

            # Advisory check on PROJECT_STATE size
            size_warning = check_project_state_size(self.project_state_file, self.config)
            if size_warning:
                print(f"\n{size_warning}\n")

            impl_profile = resolve_execution_profile(prompt.execution_profile, self.config)

            # ---------------------------------------------------------------
            # STAGE 1: IMPLEMENTATION
            # ---------------------------------------------------------------
            print("\n" + "=" * 70)
            print(f"JOB:     {job_id}")
            print(f"PROMPT:  {prompt_num}/{total} — {prompt.title}")
            print("STAGE:   IMPLEMENTATION")
            print(f"PROFILE: {impl_profile.name}")
            print(f"MODEL:   {impl_profile.model or 'CLI DEFAULT'}")
            print(f"EFFORT:  {impl_profile.effort}")
            print(f"TIMEOUT: {impl_profile.raw_timeout}")
            print("SESSION: fresh")
            print(f"LOG:     {impl_log_rel}")
            print("=" * 70)

            project_state_rel = str(self.project_state_file.relative_to(self.repo_root))

            # Construct implementation prompt (without inlining PROJECT_STATE contents)
            impl_prompt_text = (
                f"{global_contract}\n\n"
                f"---\n\n"
                f"# Step Identity & Context\n"
                f"- Job ID: {job_id}\n"
                f"- Step: Prompt {prompt_num} of {total}\n"
                f"- Title: {prompt.title}\n"
                f"- Persistent Project State Path: {project_state_rel}\n"
                f"- Required Step Handoff Path: {handoff_rel}\n\n"
                f"Before implementation, read the current persistent project state directly from:\n\n"
                f"{project_state_rel}\n\n"
                f"Treat the repository as the source of truth if the file is stale or conflicts with real code.\n\n"
                f"When you complete this step, you MUST:\n"
                f"1. Write a clear markdown summary of your work and discoveries to: {handoff_rel}\n"
                f"2. Update `{project_state_rel}` with compact context for future steps.\n"
                f"3. Run build/test verification and git diff --check.\n"
                f"4. Do NOT execute git add, commit, or push.\n\n"
                f"---\n\n"
                f"# Implementation Task: Prompt {prompt_num} — {prompt.title}\n\n"
                f"{prompt.body}\n"
            )

            # Update state to in-progress
            state["current_stage"] = "IMPLEMENTATION"
            state["active_prompt_index"] = idx
            state["updated_at"] = datetime.now().isoformat()
            atomic_write_json(job_dir / "state.json", state)

            # Run implementation conversation
            success = session_runner.run_prompt(
                prompt_text=impl_prompt_text,
                log_path=impl_log_file,
                session_label=f"Prompt {prompt_num} Implementation",
                effort=impl_profile.effort,
                model=impl_profile.model,
                timeout_seconds=impl_profile.timeout_seconds,
                timeout_str=impl_profile.raw_timeout
            )

            if not success:
                state["last_status"] = "IMPLEMENTATION_FAILED"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)
                raise RuntimeError(
                    f"Implementation failed for Prompt {prompt_num} ('{prompt.title}').\n"
                    f"Inspect log: {impl_log_rel}\n"
                    f"To resume after fixing: python3 .mod-automation/runner.py --resume"
                )

            # Verify that handoff was produced
            if not handoff_file.exists() or handoff_file.stat().st_size == 0:
                state["last_status"] = "MISSING_HANDOFF"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)
                raise RuntimeError(
                    f"Prompt {prompt_num} completed without creating the required handoff file: {handoff_rel}"
                )

            # ---------------------------------------------------------------
            # STAGE 2: AUDIT HANDLING VS CHECKPOINT
            # ---------------------------------------------------------------
            if is_audit_step:
                print("\n[Runner] Inspecting Prompt 1 audit results...")
                # Verify that no unexpected product code was modified
                porcelain = self.git.get_status_porcelain()
                product_changes = []
                for line in porcelain.splitlines():
                    fname = line[3:].strip()
                    if not (fname.startswith(".mod-automation/") or fname.startswith(".mod-automation\\")):
                        product_changes.append(line)

                if product_changes:
                    state["last_status"] = "AUDIT_UNEXPECTED_CHANGES"
                    state["updated_at"] = datetime.now().isoformat()
                    atomic_write_json(job_dir / "state.json", state)
                    raise RuntimeError(
                        f"Prompt 1 is an audit step, but product changes were detected in the repository:\n"
                        + "\n".join(product_changes)
                        + "\nAudit steps must not modify product source code. Aborting for user review."
                    )

                print("  [OK] Prompt 1 audit completed cleanly with no product changes. Skipping commit.")
                state["completed_prompts"].append({
                    "prompt_number": prompt_num,
                    "title": prompt.title,
                    "status": "AUDIT_COMPLETED",
                    "commit_hash": None,
                    "completed_at": datetime.now().isoformat()
                })
                state["next_prompt_index"] = idx + 1
                state["current_stage"] = None
                state["last_status"] = "SUCCESS"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)
                continue

            # ---------------------------------------------------------------
            # STAGE 3: CHECKPOINT CONVERSATION
            # ---------------------------------------------------------------
            chk_profile = resolve_execution_profile(self.checkpoint_profile_name, self.config)
            result_rel = f".mod-automation/jobs/{job_id}/results/{prompt.safe_filename}.json"
            result_file = self.repo_root / result_rel
            chk_log_rel = f".mod-automation/jobs/{job_id}/logs/{prompt.safe_filename}-checkpoint.ndjson"
            chk_log_file = self.repo_root / chk_log_rel

            print("\n" + "=" * 70)
            print(f"JOB:     {job_id}")
            print(f"PROMPT:  {prompt_num}/{total} — {prompt.title}")
            print("STAGE:   CHECKPOINT")
            print(f"PROFILE: {chk_profile.name}")
            print(f"MODEL:   {chk_profile.model or 'CLI DEFAULT'}")
            print(f"EFFORT:  {chk_profile.effort}")
            print(f"TIMEOUT: {chk_profile.raw_timeout}")
            print("SESSION: fresh")
            print(f"LOG:     {chk_log_rel}")
            print("=" * 70)

            # Ensure previous result is removed to avoid stale reads
            if result_file.exists():
                result_file.unlink()

            checkpoint_contract = self.checkpoint_prompt_file.read_text(encoding="utf-8")

            checkpoint_prompt_text = (
                f"{checkpoint_contract}\n\n"
                f"---\n\n"
                f"# Current Step Context\n"
                f"- Job ID: {job_id}\n"
                f"- Prompt Number: {prompt_num}\n"
                f"- Prompt Title: {prompt.title}\n"
                f"- Persistent Project State Path: {project_state_rel}\n"
                f"- Implementation Handoff File: {handoff_rel}\n"
                f"- Required Output Result JSON Path: {result_rel}\n\n"
                f"Read the project state from `{project_state_rel}` and the handoff from `{handoff_rel}` directly from the workspace.\n"
            )

            state["current_stage"] = "CHECKPOINT"
            state["updated_at"] = datetime.now().isoformat()
            atomic_write_json(job_dir / "state.json", state)

            chk_success = session_runner.run_prompt(
                prompt_text=checkpoint_prompt_text,
                log_path=chk_log_file,
                session_label=f"Prompt {prompt_num} Checkpoint",
                effort=chk_profile.effort,
                model=chk_profile.model,
                timeout_seconds=chk_profile.timeout_seconds,
                timeout_str=chk_profile.raw_timeout
            )

            if not chk_success:
                state["last_status"] = "CHECKPOINT_SESSION_FAILED"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)
                raise RuntimeError(
                    f"Checkpoint session failed for Prompt {prompt_num}.\nInspect log: {chk_log_rel}"
                )

            # Read and parse checkpoint result JSON
            if not result_file.exists():
                state["last_status"] = "MISSING_CHECKPOINT_RESULT"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)
                raise RuntimeError(
                    f"Checkpoint session completed but did not write the result JSON to {result_rel}"
                )

            try:
                with open(result_file, "r", encoding="utf-8") as rf:
                    result_data = json.load(rf)
            except Exception as ex:
                raise RuntimeError(f"Failed to parse checkpoint result JSON at {result_rel}: {ex}")

            status = result_data.get("status")
            summary = result_data.get("summary", "")
            commit_msg = result_data.get("commit_message", "").strip()
            files_to_commit = result_data.get("files_to_commit", [])

            print(f"\n[Runner] Checkpoint Decision: {status}")
            print(f"         Summary: {summary}")

            if status == "BLOCKED":
                warnings = result_data.get("warnings", [])
                state["last_status"] = "BLOCKED"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)
                err_msg = f"Checkpoint BLOCKED for Prompt {prompt_num} ('{prompt.title}').\nReason: {summary}"
                if warnings:
                    err_msg += f"\nWarnings: {warnings}"
                err_msg += f"\nFix the issue and resume with: python3 .mod-automation/runner.py --resume"
                raise RuntimeError(err_msg)

            elif status == "NO_CHANGES":
                print("  [OK] No product changes to commit for this step.")
                state["completed_prompts"].append({
                    "prompt_number": prompt_num,
                    "title": prompt.title,
                    "status": "NO_CHANGES",
                    "commit_hash": None,
                    "completed_at": datetime.now().isoformat()
                })
                state["next_prompt_index"] = idx + 1
                state["current_stage"] = None
                state["last_status"] = "SUCCESS"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)
                continue

            elif status == "READY_TO_COMMIT":
                if not commit_msg:
                    raise ValueError("Checkpoint returned READY_TO_COMMIT but commit_message is empty!")

                allowed_deletions = self.git.get_deleted_files_in_status()
                validated_files = validate_files_to_commit(
                    repo_root=self.repo_root,
                    files_to_commit=files_to_commit,
                    allowed_deletions=allowed_deletions
                )

                print(f"[Runner] Staging {len(validated_files)} checkpoint-approved file(s)...")
                self.git.add_files(validated_files)

                # Verify staged files match approved set
                staged = self.git.get_staged_files()
                for s in staged:
                    if s.startswith(".mod-automation"):
                        raise RuntimeError(f"CRITICAL: .mod-automation path staged: {s}")

                if set(staged) != set(validated_files):
                    raise RuntimeError(
                        f"Staged files mismatch!\nApproved: {set(validated_files)}\nStaged: {set(staged)}"
                    )

                stat_summary = self.git.get_staged_stat()
                print("Staged Diff Summary:\n" + stat_summary)

                commit_hash = self.git.commit(commit_msg)
                print(f"[Runner] Created commit {commit_hash[:8]}: {commit_msg}")

                if self.push_after_each:
                    print(f"[Runner] Pushing {self.git.get_current_branch()} to upstream...")
                    self.git.push()
                    print("  [OK] Push successful.")

                state["completed_prompts"].append({
                    "prompt_number": prompt_num,
                    "title": prompt.title,
                    "status": "COMMITTED",
                    "commit_hash": commit_hash,
                    "completed_at": datetime.now().isoformat()
                })
                state["last_commit"] = commit_hash
                state["next_prompt_index"] = idx + 1
                state["current_stage"] = None
                state["last_status"] = "SUCCESS"
                state["updated_at"] = datetime.now().isoformat()
                atomic_write_json(job_dir / "state.json", state)

            else:
                raise ValueError(f"Unknown checkpoint status: '{status}'")

        print("\n" + "=" * 70)
        print(f" JOB COMPLETED SUCCESSFULLY: {job_id}")
        print(f" All {len(prompts)} prompts executed and verified.")
        print("=" * 70 + "\n")


# ---------------------------------------------------------------------------
# CLI Entry Point
# ---------------------------------------------------------------------------

def build_cli_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Minecraft Mod Automation Infrastructure — Runner",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Validate inbox PROMPT_PACK.md without executing anything:
  python3 .mod-automation/runner.py --new-job initial-mod --dry-run

  # Start a brand new automation job:
  python3 .mod-automation/runner.py --new-job initial-mod

  # Resume an interrupted job:
  python3 .mod-automation/runner.py --resume

  # Explicitly resume a specific past job:
  python3 .mod-automation/runner.py --job 20260914-120000-initial-mod --resume
"""
    )
    parser.add_argument(
        "--new-job", "-n",
        metavar="SLUG",
        help="Create and start a new job from .mod-automation/runtime/PROMPT_PACK.md"
    )
    parser.add_argument(
        "--resume", "-r",
        action="store_true",
        help="Resume the currently active job (or job specified by --job)"
    )
    parser.add_argument(
        "--job", "-j",
        metavar="JOB_ID",
        help="Specify an explicit job ID to inspect or resume"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show detected prompts, planned steps, and git status without running agy or making commits"
    )
    parser.add_argument(
        "--config",
        metavar="PATH",
        help="Path to custom config.json (defaults to .mod-automation/config.json)"
    )
    parser.add_argument(
        "--dangerously-skip-permissions", "--auto-approve",
        action="store_true",
        help="Run Antigravity with auto-approved tool permissions (for unattended execution)"
    )
    return parser


def main() -> None:
    # Determine repo root safely
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent.resolve()

    parser = build_cli_parser()
    args = parser.parse_args()

    config_path = Path(args.config) if args.config else None
    manager = JobManager(repo_root=repo_root, config_path=config_path)

    if args.dangerously_skip_permissions:
        manager.auto_approve = True

    # 1. DRY RUN
    if args.dry_run:
        target_job_id = args.job or (manager.get_active_job_id() if args.resume else None)
        manager.execute_dry_run(job_name=args.new_job, job_id=target_job_id)
        return

    # 2. NEW JOB
    if args.new_job:
        job_id = manager.create_new_job(args.new_job)
        manager.run_job(job_id)
        return

    # 3. RESUME
    if args.resume or args.job:
        job_id = args.job or manager.get_active_job_id()
        if not job_id:
            print("[Runner Error] No active job found to resume! Use --new-job <name> to start one.", file=sys.stderr)
            sys.exit(1)
        print(f"[Runner] Resuming job: {job_id}")
        manager.run_job(job_id)
        return

    # 4. Default when no flags provided: check if there is an incomplete active job
    active_id = manager.get_active_job_id()
    if active_id:
        try:
            state, _, prompts = manager.load_job_state(active_id)
            if state.get("next_prompt_index", 0) < len(prompts):
                print(f"[Runner] Active incomplete job found: {active_id}")
                print("         Resuming automatically... (run with --help for options)")
                manager.run_job(active_id)
                return
        except Exception:
            pass

    parser.print_help()


if __name__ == "__main__":
    main()
