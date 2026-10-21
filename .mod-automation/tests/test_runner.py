"""
Unit tests for Minecraft Mod Automation Runner pure helper functions.
Uses only Python standard library (unittest).
"""

import os
from pathlib import Path
import sys
import tempfile
import unittest

# Ensure parent directory (.mod-automation) is in sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from runner import (
    compute_sha256,
    parse_prompt_pack,
    parse_timeout,
    slugify,
    validate_files_to_commit,
    atomic_write_text,
    atomic_write_json,
    resolve_execution_profile,
    check_project_state_size,
    AntigravitySession,
    JobManager,
    Prompt
)
from unittest.mock import patch, MagicMock


class TestRunnerHelpers(unittest.TestCase):

    def test_parse_timeout(self):
        self.assertEqual(parse_timeout("90m"), 5400)
        self.assertEqual(parse_timeout("1h"), 3600)
        self.assertEqual(parse_timeout("300s"), 300)
        self.assertEqual(parse_timeout("60"), 60)
        self.assertEqual(parse_timeout(120), 120)
        with self.assertRaises(ValueError):
            parse_timeout("invalid_timeout")

    def test_slugify(self):
        self.assertEqual(slugify("Repository Audit & Implementation Plan"), "repository-audit-implementation-plan")
        self.assertEqual(slugify("Feature 1: Portal Block States"), "feature-1-portal-block-states")
        self.assertEqual(slugify("---special!!chars---"), "special-chars")
        self.assertEqual(slugify(""), "step")

    def test_compute_sha256(self):
        text = "test prompt pack content"
        sha = compute_sha256(text)
        self.assertEqual(len(sha), 64)
        self.assertEqual(sha, compute_sha256(text))


class TestPromptPackParser(unittest.TestCase):

    def test_valid_pack_with_multiple_prompts(self):
        pack = """# My Mod — Antigravity Automation Prompt Pack

## Metadata
* Mode: NEW_PROJECT

# Prompt 1 — Repository audit
```text
Inspect the repository.
```

# Prompt 2 — Implement Block
```text
Implement custom ruby block.
```

# Prompt 3 — Implement Item
```text
Implement custom ruby item.
```

# Prompt 4 — Final verification
```text
Run build and check integration.
```
"""
        prompts = parse_prompt_pack(pack)
        self.assertEqual(len(prompts), 4)
        self.assertEqual(prompts[0].number, 1)
        self.assertEqual(prompts[0].title, "Repository audit")
        self.assertEqual(prompts[0].body, "Inspect the repository.")
        self.assertEqual(prompts[1].number, 2)
        self.assertEqual(prompts[2].number, 3)
        self.assertEqual(prompts[3].number, 4)

    def test_support_more_than_six_prompts(self):
        pack_lines = ["# Large Mod — Prompt Pack\n"]
        for i in range(1, 12):
            pack_lines.append(f"# Prompt {i} — Step {i}\n```text\nDo step {i}\n```\n")
        pack = "\n".join(pack_lines)

        prompts = parse_prompt_pack(pack)
        self.assertEqual(len(prompts), 11)
        self.assertEqual(prompts[10].number, 11)
        self.assertEqual(prompts[10].title, "Step 11")

    def test_reject_gap_in_numbering(self):
        pack = """# Pack
# Prompt 1 — Step 1
```text
body 1
```
# Prompt 3 — Step 3
```text
body 3
```
"""
        with self.assertRaises(ValueError) as ctx:
            parse_prompt_pack(pack)
        self.assertIn("expected Prompt 2", str(ctx.exception))

    def test_reject_starting_at_zero_or_two(self):
        pack = """# Pack
# Prompt 2 — Step 2
```text
body 2
```
"""
        with self.assertRaises(ValueError) as ctx:
            parse_prompt_pack(pack)
        self.assertIn("expected Prompt 1", str(ctx.exception))

    def test_reject_duplicate_prompt_numbers(self):
        pack = """# Pack
# Prompt 1 — Step 1
```text
body 1
```
# Prompt 1 — Duplicate Step 1
```text
duplicate body
```
"""
        with self.assertRaises(ValueError) as ctx:
            parse_prompt_pack(pack)
        self.assertIn("expected Prompt 2", str(ctx.exception))

    def test_reject_missing_code_block(self):
        pack = """# Pack
# Prompt 1 — Step 1
Missing code block completely.
"""
        with self.assertRaises(ValueError) as ctx:
            parse_prompt_pack(pack)
        self.assertIn("missing the required '```text'", str(ctx.exception))

    def test_reject_unclosed_code_block(self):
        pack = """# Pack
# Prompt 1 — Step 1
```text
Unclosed code block
"""
        with self.assertRaises(ValueError) as ctx:
            parse_prompt_pack(pack)
        self.assertIn("unclosed '```text'", str(ctx.exception))


class TestPathValidation(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.repo_root = Path(self.temp_dir.name).resolve()

        # Create dummy source files
        (self.repo_root / "src" / "main" / "java").mkdir(parents=True)
        (self.repo_root / "src" / "main" / "java" / "Mod.java").write_text("package test;")
        (self.repo_root / "README.md").write_text("# Test")

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_valid_files(self):
        files = ["src/main/java/Mod.java", "README.md"]
        validated = validate_files_to_commit(self.repo_root, files, allowed_deletions=[])
        self.assertEqual(validated, files)

    def test_reject_absolute_path(self):
        with self.assertRaises(ValueError) as ctx:
            validate_files_to_commit(self.repo_root, ["/etc/passwd"], allowed_deletions=[])
        self.assertIn("absolute path", str(ctx.exception).lower())

    def test_reject_directory_traversal(self):
        with self.assertRaises(ValueError) as ctx:
            validate_files_to_commit(self.repo_root, ["../../secret.txt"], allowed_deletions=[])
        self.assertIn("directory traversal", str(ctx.exception).lower())

    def test_reject_mod_automation_path(self):
        with self.assertRaises(ValueError) as ctx:
            validate_files_to_commit(
                self.repo_root,
                [".mod-automation/runtime/PROJECT_STATE.md"],
                allowed_deletions=[]
            )
        self.assertIn(".mod-automation", str(ctx.exception).lower())

    def test_reject_nonexistent_file_unless_deleted_in_git(self):
        with self.assertRaises(ValueError) as ctx:
            validate_files_to_commit(self.repo_root, ["src/Deleted.java"], allowed_deletions=[])
        self.assertIn("does not exist on disk", str(ctx.exception).lower())

        # Allowed if in deletions
        validated = validate_files_to_commit(
            self.repo_root,
            ["src/Deleted.java"],
            allowed_deletions=["src/Deleted.java"]
        )
        self.assertEqual(validated, ["src/Deleted.java"])

    def test_atomic_write(self):
        target = self.repo_root / "atomic_test.txt"
        atomic_write_text(target, "hello atomic")
        self.assertEqual(target.read_text(), "hello atomic")


class TestExecutionProfileParsing(unittest.TestCase):

    def test_low_profile_parsing(self):
        pack = """# Pack
# Prompt 1 — Setup Resources
```text
EXECUTION_PROFILE: LOW

Create recipe and loot table JSON files.
```
"""
        prompts = parse_prompt_pack(pack)
        self.assertEqual(len(prompts), 1)
        self.assertEqual(prompts[0].execution_profile, "LOW")
        self.assertEqual(prompts[0].body, "Create recipe and loot table JSON files.")
        self.assertNotIn("EXECUTION_PROFILE", prompts[0].body)

    def test_medium_profile_parsing(self):
        pack = """# Pack
# Prompt 1 — Implement Block
```text
EXECUTION_PROFILE: MEDIUM

Implement custom block logic.
```
"""
        prompts = parse_prompt_pack(pack)
        self.assertEqual(len(prompts), 1)
        self.assertEqual(prompts[0].execution_profile, "MEDIUM")
        self.assertEqual(prompts[0].body, "Implement custom block logic.")
        self.assertNotIn("EXECUTION_PROFILE", prompts[0].body)

    def test_high_profile_parsing(self):
        pack = """# Pack
# Prompt 1 — Fix Crash
```text
EXECUTION_PROFILE: HIGH

Investigate race condition in world storage.
```
"""
        prompts = parse_prompt_pack(pack)
        self.assertEqual(len(prompts), 1)
        self.assertEqual(prompts[0].execution_profile, "HIGH")
        self.assertEqual(prompts[0].body, "Investigate race condition in world storage.")
        self.assertNotIn("EXECUTION_PROFILE", prompts[0].body)

    def test_mixed_case_profile_normalization(self):
        pack = """# Pack
# Prompt 1 — Low Task
```text
execution_profile: low
Task A
```
# Prompt 2 — Medium Task
```text
Execution_Profile: Medium
Task B
```
# Prompt 3 — High Task
```text
EXECUTION_PROFILE: high
Task C
```
"""
        prompts = parse_prompt_pack(pack)
        self.assertEqual(prompts[0].execution_profile, "LOW")
        self.assertEqual(prompts[1].execution_profile, "MEDIUM")
        self.assertEqual(prompts[2].execution_profile, "HIGH")

    def test_missing_profile_uses_configured_default(self):
        pack = """# Pack
# Prompt 1 — Legacy Task
```text
Do legacy task without profile header.
```
"""
        # Default default is MEDIUM
        prompts_def = parse_prompt_pack(pack)
        self.assertEqual(prompts_def[0].execution_profile, "MEDIUM")
        self.assertEqual(prompts_def[0].body, "Do legacy task without profile header.")

        # Custom default
        prompts_custom = parse_prompt_pack(pack, default_profile="LOW")
        self.assertEqual(prompts_custom[0].execution_profile, "LOW")

    def test_invalid_profile_fails_closed(self):
        pack = """# Pack
# Prompt 1 — Bad Task
```text
EXECUTION_PROFILE: ULTRA

Do ultra task.
```
"""
        with self.assertRaises(ValueError) as ctx:
            parse_prompt_pack(pack)
        self.assertIn("invalid EXECUTION_PROFILE: 'ULTRA'", str(ctx.exception))
        self.assertIn("Supported values are: LOW, MEDIUM, HIGH", str(ctx.exception))

    def test_empty_body_after_metadata_removal_fails(self):
        pack = """# Pack
# Prompt 1 — Empty Task
```text
EXECUTION_PROFILE: LOW
```
"""
        with self.assertRaises(ValueError) as ctx:
            parse_prompt_pack(pack)
        self.assertIn("empty prompt body after removing EXECUTION_PROFILE", str(ctx.exception))


class TestExecutionProfileResolver(unittest.TestCase):

    def setUp(self):
        self.sample_config = {
            "default_execution_profile": "MEDIUM",
            "checkpoint_execution_profile": "LOW",
            "antigravity_timeout": "90m",
            "execution_profiles": {
                "LOW": {
                    "model": None,
                    "effort": "low",
                    "timeout": "45m"
                },
                "MEDIUM": {
                    "model": None,
                    "effort": "medium",
                    "timeout": "90m"
                },
                "HIGH": {
                    "model": "custom-model-id",
                    "effort": "high",
                    "timeout": "120m"
                },
                "EMPTY_MODEL": {
                    "model": "   ",
                    "effort": "low",
                    "timeout": "30m"
                }
            }
        }

    def test_resolve_correct_effort_and_timeout(self):
        low = resolve_execution_profile("LOW", self.sample_config)
        self.assertEqual(low.name, "LOW")
        self.assertEqual(low.effort, "low")
        self.assertEqual(low.timeout_seconds, 2700)
        self.assertEqual(low.raw_timeout, "45m")
        self.assertIsNone(low.model)

        med = resolve_execution_profile("MEDIUM", self.sample_config)
        self.assertEqual(med.name, "MEDIUM")
        self.assertEqual(med.effort, "medium")
        self.assertEqual(med.timeout_seconds, 5400)
        self.assertEqual(med.raw_timeout, "90m")
        self.assertIsNone(med.model)

        high = resolve_execution_profile("HIGH", self.sample_config)
        self.assertEqual(high.name, "HIGH")
        self.assertEqual(high.effort, "high")
        self.assertEqual(high.timeout_seconds, 7200)
        self.assertEqual(high.raw_timeout, "120m")
        self.assertEqual(high.model, "custom-model-id")

    def test_null_and_empty_model_resolution(self):
        low = resolve_execution_profile("LOW", self.sample_config)
        self.assertIsNone(low.model)

        empty_model = resolve_execution_profile("EMPTY_MODEL", self.sample_config)
        self.assertIsNone(empty_model.model)

    def test_unknown_profile_fails(self):
        with self.assertRaises(ValueError) as ctx:
            resolve_execution_profile("UNKNOWN", self.sample_config)
        self.assertIn("Unknown execution profile 'UNKNOWN'", str(ctx.exception))

    def test_invalid_effort_fails(self):
        bad_config = {
            "execution_profiles": {
                "TEST": {
                    "model": None,
                    "effort": "ultra",
                    "timeout": "30m"
                }
            }
        }
        with self.assertRaises(ValueError) as ctx:
            resolve_execution_profile("TEST", bad_config)
        self.assertIn("Invalid effort 'ultra'", str(ctx.exception))

    def test_fallback_to_antigravity_timeout(self):
        config_no_timeout = {
            "antigravity_timeout": "75m",
            "execution_profiles": {
                "TEST": {
                    "model": None,
                    "effort": "low"
                }
            }
        }
        res = resolve_execution_profile("TEST", config_no_timeout)
        self.assertEqual(res.timeout_seconds, 4500)
        self.assertEqual(res.raw_timeout, "75m")


class TestAntigravityCommandConstruction(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.repo_root = Path(self.temp_dir.name).resolve()
        self.log_path = self.repo_root / "test.log"

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_null_model_omits_model_flag(self):
        session = AntigravitySession(repo_root=self.repo_root, timeout_seconds=3600)
        with patch("subprocess.Popen") as mock_popen:
            mock_proc = MagicMock()
            mock_proc.stdout.readline.return_value = ""
            mock_proc.wait.return_value = 0
            mock_popen.return_value = mock_proc

            session.run_prompt(
                prompt_text="test prompt",
                log_path=self.log_path,
                session_label="Test",
                effort="low",
                model=None,
                timeout_str="45m"
            )

            cmd_called = mock_popen.call_args[0][0]
            self.assertIn("--effort", cmd_called)
            self.assertEqual(cmd_called[cmd_called.index("--effort") + 1], "low")
            self.assertIn("--print-timeout", cmd_called)
            self.assertEqual(cmd_called[cmd_called.index("--print-timeout") + 1], "45m")
            self.assertNotIn("--model", cmd_called)

    def test_configured_model_includes_model_flag(self):
        session = AntigravitySession(repo_root=self.repo_root, timeout_seconds=3600)
        with patch("subprocess.Popen") as mock_popen:
            mock_proc = MagicMock()
            mock_proc.stdout.readline.return_value = ""
            mock_proc.wait.return_value = 0
            mock_popen.return_value = mock_proc

            session.run_prompt(
                prompt_text="test prompt",
                log_path=self.log_path,
                session_label="Test",
                effort="high",
                model="custom-model-id",
                timeout_str="120m"
            )

            cmd_called = mock_popen.call_args[0][0]
            self.assertIn("--effort", cmd_called)
            self.assertEqual(cmd_called[cmd_called.index("--effort") + 1], "high")
            self.assertIn("--model", cmd_called)
            self.assertEqual(cmd_called[cmd_called.index("--model") + 1], "custom-model-id")
            self.assertIn("--print-timeout", cmd_called)
            self.assertEqual(cmd_called[cmd_called.index("--print-timeout") + 1], "120m")


class TestProjectStateMonitoring(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.ps_path = Path(self.temp_dir.name) / "PROJECT_STATE.md"
        self.config = {
            "project_state_warning_bytes": 1000,
            "project_state_strong_warning_bytes": 2000
        }

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_below_threshold_no_warning(self):
        self.ps_path.write_text("a" * 500)
        msg = check_project_state_size(self.ps_path, self.config)
        self.assertIsNone(msg)

    def test_at_warning_threshold(self):
        self.ps_path.write_text("a" * 1500)
        msg = check_project_state_size(self.ps_path, self.config)
        self.assertIsNotNone(msg)
        self.assertIn("WARNING: PROJECT_STATE.md is", msg)
        self.assertIn("It is intended to be compact persistent context", msg)

    def test_at_strong_warning_threshold(self):
        self.ps_path.write_text("a" * 2500)
        msg = check_project_state_size(self.ps_path, self.config)
        self.assertIsNotNone(msg)
        self.assertIn("WARNING: PROJECT_STATE.md is", msg)
        self.assertIn("Persistent project context is becoming large and may increase agent quota usage", msg)


class TestPromptContentIsolation(unittest.TestCase):

    def test_project_state_contents_not_inlined(self):
        """Verify prompt construction instructions point to disk path and do not paste entire content."""
        temp_dir = tempfile.TemporaryDirectory()
        try:
            repo_root = Path(temp_dir.name).resolve()
            # Setup directories
            runtime_dir = repo_root / ".mod-automation" / "runtime"
            runtime_dir.mkdir(parents=True)
            jobs_dir = repo_root / ".mod-automation" / "jobs"
            jobs_dir.mkdir(parents=True)
            templates_dir = repo_root / ".mod-automation" / "templates"
            templates_dir.mkdir(parents=True)

            ps_file = runtime_dir / "PROJECT_STATE.md"
            secret_marker = "SECRET_MEMORY_DO_NOT_INLINE_INTO_PROMPT_STRING"
            ps_file.write_text(f"# Memory\n{secret_marker}\n")

            (templates_dir / "PROJECT_STATE.template.md").write_text("# Template")
            (templates_dir / "state.template.json").write_text("{}")
            (repo_root / ".mod-automation" / "GLOBAL_CONTRACT.md").write_text("# Global Contract")
            (repo_root / ".mod-automation" / "CHECKPOINT_PROMPT.md").write_text("# Checkpoint Contract")

            config_path = repo_root / ".mod-automation" / "config.json"
            config_data = {
                "inbox_prompt_pack": ".mod-automation/runtime/PROMPT_PACK.md",
                "project_state": ".mod-automation/runtime/PROJECT_STATE.md",
                "jobs_directory": ".mod-automation/jobs",
                "global_contract": ".mod-automation/GLOBAL_CONTRACT.md",
                "checkpoint_prompt": ".mod-automation/CHECKPOINT_PROMPT.md",
                "project_state_template": ".mod-automation/templates/PROJECT_STATE.template.md",
                "state_template": ".mod-automation/templates/state.template.json",
                "default_execution_profile": "MEDIUM",
                "checkpoint_execution_profile": "LOW",
                "execution_profiles": {
                    "LOW": {"model": None, "effort": "low", "timeout": "45m"},
                    "MEDIUM": {"model": None, "effort": "medium", "timeout": "90m"},
                    "HIGH": {"model": None, "effort": "high", "timeout": "120m"}
                }
            }
            atomic_write_json(config_path, config_data)

            mgr = JobManager(repo_root=repo_root, config_path=config_path)

            # Check that default and checkpoint profiles are resolved correctly
            self.assertEqual(mgr.default_profile_name, "MEDIUM")
            self.assertEqual(mgr.checkpoint_profile_name, "LOW")

            # Check checkpoint resolution stays LOW even if an implementation is HIGH
            high_impl = resolve_execution_profile("HIGH", mgr.config)
            chk = resolve_execution_profile(mgr.checkpoint_profile_name, mgr.config)
            self.assertEqual(high_impl.effort, "high")
            self.assertEqual(chk.effort, "low")

            # Simulate building prompt strings as runner.py does
            global_contract = mgr.global_contract_file.read_text()
            checkpoint_contract = mgr.checkpoint_prompt_file.read_text()
            project_state_rel = str(mgr.project_state_file.relative_to(mgr.repo_root))
            handoff_rel = ".mod-automation/jobs/job-1/handoffs/01-test.md"
            result_rel = ".mod-automation/jobs/job-1/results/01-test.json"

            prompt = Prompt(
                number=2,
                title="Test Task",
                body="Implement something",
                execution_profile="HIGH"
            )

            impl_prompt_text = (
                f"{global_contract}\n\n"
                f"---\n\n"
                f"# Step Identity & Context\n"
                f"- Job ID: job-1\n"
                f"- Step: Prompt {prompt.number} of 3\n"
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
                f"# Implementation Task: Prompt {prompt.number} — {prompt.title}\n\n"
                f"{prompt.body}\n"
            )

            checkpoint_prompt_text = (
                f"{checkpoint_contract}\n\n"
                f"---\n\n"
                f"# Current Step Context\n"
                f"- Job ID: job-1\n"
                f"- Prompt Number: {prompt.number}\n"
                f"- Prompt Title: {prompt.title}\n"
                f"- Persistent Project State Path: {project_state_rel}\n"
                f"- Implementation Handoff File: {handoff_rel}\n"
                f"- Required Output Result JSON Path: {result_rel}\n\n"
                f"Read the project state from `{project_state_rel}` and the handoff from `{handoff_rel}` directly from the workspace.\n"
            )

            # Assert PROJECT_STATE contents are NOT inlined
            self.assertNotIn(secret_marker, impl_prompt_text)
            self.assertNotIn(secret_marker, checkpoint_prompt_text)

            # Assert path instruction IS present
            self.assertIn(f"read the current persistent project state directly from:\n\n{project_state_rel}", impl_prompt_text)
            self.assertIn(f"Read the project state from `{project_state_rel}`", checkpoint_prompt_text)

            # Assert historical handoffs are not inlined
            self.assertNotIn("PAST_HANDOFF_CONTENT", impl_prompt_text)
            self.assertNotIn("PAST_HANDOFF_CONTENT", checkpoint_prompt_text)

        finally:
            temp_dir.cleanup()


if __name__ == "__main__":
    unittest.main()
