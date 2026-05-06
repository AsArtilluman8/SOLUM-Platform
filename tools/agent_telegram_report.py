#!/usr/bin/env python3
"""Build a local Telegram-ready agent report.

This tool only writes a local text file. It does not read tokens, call network
APIs, or send Telegram messages.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
from pathlib import Path


DEFAULT_OUTPUT = Path("_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt")


def split_items(value: str) -> list[str]:
    items: list[str] = []
    for raw in value.split(";"):
        item = raw.strip()
        if item:
            items.append(item)
    return items


def section(title: str, items: list[str]) -> list[str]:
    lines = [f"{title}:"]
    if items:
        lines.extend(f"- {item}" for item in items)
    else:
        lines.append("- n/a")
    return lines


def build_report(args: argparse.Namespace) -> str:
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    lines: list[str] = [
        "SOLUM AGENT REPORT",
        f"Timestamp: {timestamp}",
        f"Stage / Patch: {args.stage_patch}",
        "",
    ]
    lines.extend(section("Changed", split_items(args.changed)))
    lines.append("")
    lines.extend(section("Checks", split_items(args.checks)))
    lines.append("")
    lines.extend(section("Output", split_items(args.output)))
    lines.append("")
    lines.extend(section("Known issues", split_items(args.known_issues)))
    lines.append("")
    lines.extend(section("Next step", split_items(args.next_step)))
    lines.append("")
    return "\n".join(lines)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create a local Telegram-ready SOLUM agent report."
    )
    parser.add_argument("--stage-patch", required=True)
    parser.add_argument("--changed", default="")
    parser.add_argument("--checks", default="")
    parser.add_argument("--output", default="")
    parser.add_argument("--known-issues", default="")
    parser.add_argument("--next-step", default="")
    parser.add_argument("--write", type=Path, default=DEFAULT_OUTPUT)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    report = build_report(args)
    args.write.parent.mkdir(parents=True, exist_ok=True)
    args.write.write_text(report, encoding="utf-8")
    print(f"report={args.write}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
