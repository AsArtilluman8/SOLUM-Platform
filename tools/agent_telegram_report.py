#!/usr/bin/env python3
"""Build local human-friendly SOLUM agent reports.

This tool only writes a local text file. It does not read tokens, call network
APIs, or send Telegram messages.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
from html import escape
from pathlib import Path


DEFAULT_TEXT_OUTPUT = Path("_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt")
DEFAULT_HTML_OUTPUT = Path("_work/agent_reports/latest/SOLUM_AGENT_REPORT.html")
LOAD_LABELS = {
    "LOW": "🟢 LOW",
    "MEDIUM": "🟡 MEDIUM",
    "HIGH": "🔴 HIGH",
}


def split_items(value: str) -> list[str]:
    items: list[str] = []
    for raw in value.split(";"):
        item = raw.strip()
        if item:
            items.append(item)
    return items


def prefixed_items(items: list[str], prefix: str) -> list[str]:
    if items:
        return [f"{prefix} {item}" for item in items]
    return [f"{prefix} n/a"]


def section(title: str, items: list[str], prefix: str) -> list[str]:
    lines = [f"{title}:"]
    lines.extend(prefixed_items(items, prefix))
    return lines


def estimate_context_load(args: argparse.Namespace) -> str:
    if args.context_load != "AUTO":
        return args.context_load

    weighted_count = (
        len(split_items(args.changed))
        + len(split_items(args.checks))
        + len(split_items(args.not_touched))
        + len(split_items(args.problems))
        + len(split_items(args.files))
        + len(split_items(args.output))
    )
    if weighted_count <= 8:
        return "LOW"
    if weighted_count <= 18:
        return "MEDIUM"
    return "HIGH"


def build_text_report(args: argparse.Namespace) -> str:
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    load = estimate_context_load(args)
    lines: list[str] = [
        "✅ SOLUM Agent Report",
        "",
        f"Патч: {args.stage_patch}",
        f"Статус: {args.status}",
        f"Время: {timestamp}",
        "",
    ]
    lines.extend(section("Что сделал", split_items(args.changed), "++"))
    lines.append("")
    lines.extend(section("Что проверил", split_items(args.checks), "++"))
    lines.append("")
    lines.extend(section("Что не трогал", split_items(args.not_touched), "--"))
    lines.append("")
    lines.extend(section("Проблемы", split_items(args.problems), "!!"))
    lines.append("")
    lines.append("Нагрузка:")
    lines.append(LOAD_LABELS[load])
    lines.append("Token Load Estimate: примерная оценка, без точных токенов")
    lines.append("")
    lines.extend(section("Следующий шаг", split_items(args.next_step), "->"))
    lines.append("")
    lines.extend(section("Файлы", split_items(args.files) + split_items(args.output), "++"))
    lines.append("")
    return "\n".join(lines)


def build_html_report(text_report: str, args: argparse.Namespace) -> str:
    load = estimate_context_load(args)
    sections: list[tuple[str, list[str], str]] = [
        ("Что сделал", split_items(args.changed), "++"),
        ("Что проверил", split_items(args.checks), "++"),
        ("Что не трогал", split_items(args.not_touched), "--"),
        ("Проблемы", split_items(args.problems), "!!"),
        ("Следующий шаг", split_items(args.next_step), "->"),
        ("Файлы", split_items(args.files) + split_items(args.output), "++"),
    ]
    rendered_sections: list[str] = []
    for title, items, prefix in sections:
        rendered_items = "\n".join(
            f"<li><span>{escape(prefix)}</span> {escape(item)}</li>"
            for item in (items or ["n/a"])
        )
        rendered_sections.append(
            f"<section><h2>{escape(title)}</h2><ul>{rendered_items}</ul></section>"
        )

    return f"""<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>SOLUM Agent Report</title>
  <style>
    body {{ margin: 0; font-family: Arial, sans-serif; background: #f4f6f8; color: #111827; }}
    main {{ max-width: 900px; margin: 0 auto; padding: 24px; }}
    header, section {{ background: #ffffff; border: 1px solid #d8dee6; border-radius: 8px; padding: 16px; margin-bottom: 12px; }}
    h1 {{ margin: 0 0 8px; font-size: 24px; }}
    h2 {{ margin: 0 0 10px; font-size: 18px; }}
    p {{ margin: 4px 0; }}
    ul {{ margin: 0; padding-left: 0; list-style: none; }}
    li {{ margin: 6px 0; line-height: 1.4; }}
    li span {{ display: inline-block; min-width: 28px; font-weight: 700; }}
    .load {{ font-size: 20px; font-weight: 700; }}
    pre {{ white-space: pre-wrap; overflow-wrap: anywhere; background: #111827; color: #f9fafb; padding: 12px; border-radius: 8px; }}
  </style>
</head>
<body>
<main>
  <header>
    <h1>✅ SOLUM Agent Report</h1>
    <p><strong>Патч:</strong> {escape(args.stage_patch)}</p>
    <p><strong>Статус:</strong> {escape(args.status)}</p>
    <p class="load">{escape(LOAD_LABELS[load])}</p>
    <p>Token Load Estimate: примерная оценка, без точных токенов.</p>
  </header>
  {''.join(rendered_sections)}
  <section>
    <h2>Текстовая версия</h2>
    <pre>{escape(text_report)}</pre>
  </section>
</main>
</body>
</html>
"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create local human-friendly SOLUM Telegram and HTML reports."
    )
    parser.add_argument("--stage-patch", required=True)
    parser.add_argument("--status", default="готово к review")
    parser.add_argument("--changed", default="")
    parser.add_argument("--checks", default="")
    parser.add_argument("--output", default="", help="Backward-compatible output/file list.")
    parser.add_argument("--not-touched", default="")
    parser.add_argument("--problems", default="")
    parser.add_argument("--known-issues", default="")
    parser.add_argument("--next-step", default="")
    parser.add_argument("--files", default="")
    parser.add_argument(
        "--context-load",
        choices=["AUTO", "LOW", "MEDIUM", "HIGH"],
        default="AUTO",
        help="Approximate context/token load label. AUTO estimates LOW/MEDIUM/HIGH without exact tokens.",
    )
    parser.add_argument("--write", type=Path, default=DEFAULT_TEXT_OUTPUT)
    parser.add_argument("--html-write", type=Path, default=DEFAULT_HTML_OUTPUT)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.known_issues and not args.problems:
        args.problems = args.known_issues

    report = build_text_report(args)
    html = build_html_report(report, args)

    args.write.parent.mkdir(parents=True, exist_ok=True)
    args.write.write_text(report, encoding="utf-8")
    args.html_write.parent.mkdir(parents=True, exist_ok=True)
    args.html_write.write_text(html, encoding="utf-8")
    print(f"text_report={args.write}")
    print(f"html_report={args.html_write}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
