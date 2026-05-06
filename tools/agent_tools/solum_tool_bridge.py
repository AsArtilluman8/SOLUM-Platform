#!/usr/bin/env python3
"""SOLUM local tools bridge.

This is a CLI foundation for future MCP tools. It wraps existing project tools
through an explicit allowlist and does not expose arbitrary shell execution.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
REPORT_DIR = REPO_ROOT / "_work" / "agent_reports" / "latest"
TEXT_REPORT = REPORT_DIR / "SOLUM_TELEGRAM_REPORT.txt"
HTML_REPORT = REPORT_DIR / "SOLUM_AGENT_REPORT.html"
METRICS_REPORT = REPORT_DIR / "SOLUM_AGENT_METRICS.json"
FOUNDATION_REPORT = REPORT_DIR / "SOLUM_FOUNDATION_READINESS.txt"
BUILD_LOG_SHORT = REPORT_DIR / "SOLUM_AGENT_BUILD_LOG_SHORT.txt"
DIAGNOSTICS_ZIP = Path("/storage/emulated/0/SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip")
LATEST_REPORT_HTML = Path("/storage/emulated/0/SOLUMCreative/reports/latest/SOLUM_LATEST_REPORT.html")


class BridgeError(RuntimeError):
    pass


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(REPO_ROOT))
    except ValueError:
        return str(path)


def run_allowed(command: list[str], dry_run: bool) -> int:
    printable = " ".join(command)
    if dry_run:
        print(f"dry_run=ok command={printable}")
        return 0
    result = subprocess.run(command, cwd=REPO_ROOT, check=False)
    return int(result.returncode)


def git_value(args: list[str], fallback: str = "unknown") -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    )
    value = result.stdout.strip()
    return value or fallback


def git_status_lines() -> list[str]:
    result = subprocess.run(
        ["git", "status", "--short"],
        cwd=REPO_ROOT,
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    )
    if result.returncode != 0:
        return ["git_status=unavailable"]
    lines = [line for line in result.stdout.splitlines() if line.strip()]
    return lines or ["working_tree=clean"]


def path_state(path: Path) -> str:
    if path.exists():
        kind = "dir" if path.is_dir() else "file"
        return f"{rel(path)}={kind}"
    return f"{rel(path)}=missing"


def path_status(path: Path) -> dict[str, object]:
    exists = path.exists()
    kind = "missing"
    if exists:
        kind = "dir" if path.is_dir() else "file"
    return {
        "path": rel(path),
        "exists": exists,
        "kind": kind,
        "status": kind if exists else "missing",
    }


def emit_json(payload: dict[str, object]) -> int:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))
    return 0


def base_payload(args: argparse.Namespace, *, ok: bool = True) -> dict[str, object]:
    return {
        "ok": ok,
        "command": args.command,
        "dry_run": bool(args.dry_run),
        "repo_root": str(REPO_ROOT),
        "errors": [],
    }


def planned_command(command: list[str]) -> str:
    return " ".join(command)


def command_generate_report(args: argparse.Namespace) -> int:
    command = [
        sys.executable,
        "tools/agent_telegram_report.py",
        "--stage-patch",
        args.stage_patch,
        "--status",
        args.status,
        "--changed",
        args.changed,
        "--checks",
        args.checks,
        "--not-touched",
        args.not_touched,
        "--problems",
        args.problems,
        "--files",
        args.files,
        "--context-load",
        args.context_load,
        "--next-step",
        args.next_step,
    ]
    if args.json and args.dry_run:
        payload = base_payload(args)
        payload["paths"] = {
            "text_report": path_status(TEXT_REPORT),
            "html_report": path_status(HTML_REPORT),
        }
        payload["planned_actions"] = [
            {
                "type": "write_reports",
                "command": planned_command(command),
                "writes": [rel(TEXT_REPORT), rel(HTML_REPORT)],
            }
        ]
        return emit_json(payload)
    if args.dry_run:
        print("dry_run=ok")
        print("would_write=" + rel(TEXT_REPORT))
        print("would_write=" + rel(HTML_REPORT))
    return run_allowed(command, args.dry_run)


def command_send_telegram_report(args: argparse.Namespace) -> int:
    sender_mode = "--send" if args.send else "--dry-run"
    if args.dry_run:
        if args.json:
            payload = base_payload(args)
            payload["paths"] = {
                "text_report": path_status(TEXT_REPORT),
                "html_report": path_status(HTML_REPORT),
                "secret_file": {
                    "path": "~/.solum/secrets/telegram.env",
                    "status": "not_read",
                },
            }
            payload["planned_actions"] = [
                {
                    "type": "telegram_sender",
                    "command": f"tools/send_telegram_report.py {sender_mode}",
                    "network": args.send,
                    "token": "not_read",
                }
            ]
            return emit_json(payload)
        print("dry_run=ok")
        print("would_call=tools/send_telegram_report.py --send" if args.send else "would_call=tools/send_telegram_report.py --dry-run")
        print("secret_file=~/.solum/secrets/telegram.env")
        print("telegram_bot_token=not_read")
        print(path_state(TEXT_REPORT))
        print(path_state(HTML_REPORT))
        return 0
    return run_allowed([sys.executable, "tools/send_telegram_report.py", sender_mode], dry_run=False)


def command_foundation_readiness(args: argparse.Namespace) -> int:
    if args.run_runner:
        command = ["bash", "tools/agent_build_runner.sh"]
    else:
        command = ["bash", "tools/check_foundation_readiness.sh"]
    if args.dry_run:
        if args.json:
            payload = base_payload(args)
            payload["paths"] = {
                "foundation_report": path_status(FOUNDATION_REPORT),
            }
            payload["planned_actions"] = [
                {
                    "type": "run_runner" if args.run_runner else "foundation_readiness",
                    "command": planned_command(command),
                    "runner_requested": bool(args.run_runner),
                    "writes": [rel(FOUNDATION_REPORT)],
                }
            ]
            return emit_json(payload)
        print("dry_run=ok")
        print("runner_requested=" + ("yes" if args.run_runner else "no"))
        print("would_write=" + rel(FOUNDATION_REPORT))
    return run_allowed(command, args.dry_run)


def command_latest_paths(args: argparse.Namespace) -> int:
    paths = [
        TEXT_REPORT,
        HTML_REPORT,
        METRICS_REPORT,
        FOUNDATION_REPORT,
        BUILD_LOG_SHORT,
        DIAGNOSTICS_ZIP,
        LATEST_REPORT_HTML,
    ]
    if args.json:
        payload = base_payload(args)
        payload["paths"] = [path_status(path) for path in paths]
        if args.dry_run:
            payload["planned_actions"] = [
                {
                    "type": "inspect_paths",
                    "paths": [rel(path) for path in paths],
                }
            ]
        return emit_json(payload)
    if args.dry_run:
        print("dry_run=ok")
    for path in paths:
        print(path_state(path))
    return 0


def command_print_status(args: argparse.Namespace) -> int:
    tool_paths = [
        REPO_ROOT / "tools" / "agent_telegram_report.py",
        REPO_ROOT / "tools" / "send_telegram_report.py",
        REPO_ROOT / "tools" / "check_foundation_readiness.sh",
        REPO_ROOT / "tools" / "agent_build_runner.sh",
    ]
    if args.json:
        payload = base_payload(args)
        payload["branch"] = git_value(["branch", "--show-current"])
        payload["head"] = git_value(["rev-parse", "--short", "HEAD"])
        payload["tools"] = [path_status(path) for path in tool_paths]
        payload["git_status"] = git_status_lines()
        if args.dry_run:
            payload["planned_actions"] = [
                {
                    "type": "print_status",
                    "reads": ["git branch", "git rev-parse", "git status --short"],
                }
            ]
        return emit_json(payload)
    if args.dry_run:
        print("dry_run=ok")
    print(f"repo_root={REPO_ROOT}")
    print(f"branch={git_value(['branch', '--show-current'])}")
    print(f"head={git_value(['rev-parse', '--short', 'HEAD'])}")
    print("tools:")
    for path in tool_paths:
        print("- " + path_state(path))
    print("git_status:")
    for line in git_status_lines():
        print("- " + line)
    return 0


def add_common(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--dry-run", action="store_true", help="Print planned action without side effects.")
    parser.add_argument("--json", action="store_true", help="Print structured JSON for future MCP wrappers.")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="SOLUM local tools bridge foundation.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    generate = subparsers.add_parser("generate-report", help="Create local TXT/HTML agent report.")
    add_common(generate)
    generate.add_argument("--stage-patch", default="P01E — MCP/local tools bridge foundation")
    generate.add_argument("--status", default="готово к review")
    generate.add_argument(
        "--changed",
        default="Добавлен CLI bridge foundation;Добавлена MCP/local tools документация;Добавлен Accessibility companion plan",
    )
    generate.add_argument(
        "--checks",
        default="Bridge dry-run checks запланированы;Runtime/Vulkan/Gradle вне scope",
    )
    generate.add_argument(
        "--not-touched",
        default="Android runtime;Vulkan/render;Gradle/build system;Secrets",
    )
    generate.add_argument(
        "--problems",
        default="Настоящий MCP server ещё не создан;Accessibility companion пока только план",
    )
    generate.add_argument(
        "--files",
        default="docs/MCP_LOCAL_TOOLS_BRIDGE.md;docs/ACCESSIBILITY_COMPANION_PLAN.md;tools/agent_tools/README.md;tools/agent_tools/solum_tool_bridge.py",
    )
    generate.add_argument("--context-load", choices=["AUTO", "LOW", "MEDIUM", "HIGH"], default="MEDIUM")
    generate.add_argument("--next-step", default="Review PR")
    generate.set_defaults(func=command_generate_report)

    send = subparsers.add_parser("send-telegram-report", help="Validate or send latest report via Telegram sender.")
    add_common(send)
    send.add_argument("--send", action="store_true", help="Actually send through Telegram Bot API.")
    send.set_defaults(func=command_send_telegram_report)

    readiness = subparsers.add_parser("foundation-readiness", help="Run foundation readiness check.")
    add_common(readiness)
    readiness.add_argument("--run-runner", action="store_true", help="Explicitly run tools/agent_build_runner.sh.")
    readiness.set_defaults(func=command_foundation_readiness)

    latest = subparsers.add_parser("latest-paths", help="Print latest report/diagnostics paths.")
    add_common(latest)
    latest.set_defaults(func=command_latest_paths)

    status = subparsers.add_parser("print-status", help="Print repo/tool status.")
    add_common(status)
    status.set_defaults(func=command_print_status)

    return parser.parse_args()


def main() -> int:
    args = parse_args()
    return int(args.func(args))


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except BridgeError as exc:
        print(f"solum_tool_bridge_error={exc}")
        raise SystemExit(1)
