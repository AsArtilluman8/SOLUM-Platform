#!/usr/bin/env python3
"""Build local human-friendly SOLUM agent reports.

This tool only writes a local text file. It does not read tokens, call network
APIs, or send Telegram messages.
"""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from html import escape
from pathlib import Path


DEFAULT_TEXT_OUTPUT = Path("_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt")
DEFAULT_HTML_OUTPUT = Path("_work/agent_reports/latest/SOLUM_AGENT_REPORT.html")
DEFAULT_METRICS_INPUT = Path("_work/agent_reports/latest/SOLUM_AGENT_METRICS.json")
LOAD_LABELS = {
    "LOW": "🟢 LOW",
    "MEDIUM": "🟡 MEDIUM",
    "HIGH": "🔴 HIGH",
}
METRIC_FIELDS = [
    "fps_current",
    "fps_previous",
    "fps_delta",
    "quality_score",
    "material_score",
    "shadow_score",
    "visual_status",
    "changed_files_count",
    "checks_passed",
    "checks_failed",
]


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


def load_metrics(path: Path) -> tuple[dict[str, object], str | None]:
    if not path.is_file():
        return {}, "Метрики недоступны: runtime/visual diagnostics не запускались"
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return {}, f"Метрики недоступны: файл не прочитан ({exc})"
    if not isinstance(raw, dict):
        return {}, "Метрики недоступны: JSON должен быть объектом"
    return {key: raw.get(key) for key in METRIC_FIELDS}, None


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


def percent(value: object, fallback: int = 0) -> int:
    if isinstance(value, bool):
        return fallback
    if isinstance(value, (int, float)):
        return max(0, min(100, int(round(float(value)))))
    return fallback


def format_metric(value: object) -> str:
    if value is None:
        return "not_available"
    return str(value)


def checks_progress(metrics: dict[str, object], checks: list[str]) -> tuple[int, str]:
    passed = metrics.get("checks_passed")
    failed = metrics.get("checks_failed")
    if isinstance(passed, int) and isinstance(failed, int):
        total = passed + failed
        if total <= 0:
            return 0, "checks not_available"
        return int(round((passed / total) * 100)), f"{passed} OK / {failed} failed"
    if checks:
        return 100, f"{len(checks)} listed"
    return 0, "checks not_available"


def readiness_progress(metrics: dict[str, object], load: str, checks: list[str], problems: list[str]) -> int:
    checks_value, _ = checks_progress(metrics, checks)
    quality = percent(metrics.get("quality_score"), fallback=0)
    context_penalty = {"LOW": 0, "MEDIUM": 5, "HIGH": 12}[load]
    problem_penalty = 10 if problems else 0
    base = checks_value if checks_value else 70
    if quality:
        base = int(round((base + quality) / 2))
    return max(0, min(100, base - context_penalty - problem_penalty))


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


def html_list(items: list[str], prefix: str) -> str:
    return "\n".join(
        f"<li><span>{escape(prefix)}</span> {escape(item)}</li>"
        for item in (items or ["n/a"])
    )


def bar(label: str, value: int, detail: str) -> str:
    safe_value = max(0, min(100, value))
    return f"""
      <div class="bar-row">
        <div class="bar-label"><span>{escape(label)}</span><strong>{escape(detail)}</strong></div>
        <div class="bar-track"><div class="bar-fill" style="width: {safe_value}%"></div></div>
      </div>
    """


def metric_bar(label: str, value: object) -> str:
    if value is None:
        return bar(label, 0, "not_available")
    return bar(label, percent(value), format_metric(value))


def card(title: str, value: str, detail: str, state: str = "neutral") -> str:
    return f"""
      <article class="metric-card {escape(state)}">
        <p>{escape(title)}</p>
        <strong>{escape(value)}</strong>
        <span>{escape(detail)}</span>
      </article>
    """


def files_table(files: list[str]) -> str:
    rows = []
    for index, file_path in enumerate(files or ["n/a"], 1):
        kind = "output" if file_path.startswith("_work/") else "repo"
        rows.append(
            "<tr>"
            f"<td>{index}</td>"
            f"<td><code>{escape(file_path)}</code></td>"
            f"<td>{escape(kind)}</td>"
            "</tr>"
        )
    return "\n".join(rows)


def timeline_step(title: str, done: bool) -> str:
    state = "done" if done else "pending"
    marker = "++" if done else "--"
    return f"<li class=\"{state}\"><span>{escape(marker)}</span><strong>{escape(title)}</strong></li>"


def build_html_report(text_report: str, args: argparse.Namespace) -> str:
    load = estimate_context_load(args)
    changed = split_items(args.changed)
    checks = split_items(args.checks)
    not_touched = split_items(args.not_touched)
    problems = split_items(args.problems)
    next_step = split_items(args.next_step)
    files = split_items(args.files) + split_items(args.output)
    metrics, metrics_issue = load_metrics(args.metrics)
    readiness = readiness_progress(metrics, load, checks, problems)
    checks_value, checks_detail = checks_progress(metrics, checks)
    telegram_done = any("telegram" in item.lower() and "ok" in item.lower() for item in checks)
    visual_status = format_metric(metrics.get("visual_status")) if metrics else "not_available"
    changed_count = metrics.get("changed_files_count")
    if changed_count is None:
        changed_count = len(files)
    metrics_rows = "\n".join(
        f"<tr><td>{escape(key)}</td><td>{escape(format_metric(metrics.get(key)))}</td></tr>"
        for key in METRIC_FIELDS
    )

    return f"""<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>SOLUM Agent Report</title>
  <style>
    :root {{ color-scheme: light; --ink: #111827; --muted: #5b6472; --line: #d8dee6; --panel: #ffffff; --bg: #f4f6f8; --ok: #137a46; --warn: #a35f00; --bad: #b42318; --accent: #2563eb; }}
    * {{ box-sizing: border-box; }}
    body {{ margin: 0; font-family: Arial, sans-serif; background: var(--bg); color: var(--ink); }}
    main {{ max-width: 1120px; margin: 0 auto; padding: 24px; }}
    header, section {{ background: var(--panel); border: 1px solid var(--line); border-radius: 8px; padding: 18px; margin-bottom: 14px; }}
    h1 {{ margin: 0 0 10px; font-size: 30px; line-height: 1.15; }}
    h2 {{ margin: 0 0 12px; font-size: 18px; }}
    p {{ margin: 5px 0; line-height: 1.45; }}
    code {{ font-family: Consolas, monospace; font-size: 13px; }}
    ul {{ margin: 0; padding-left: 0; list-style: none; }}
    li {{ margin: 7px 0; line-height: 1.4; }}
    li span {{ display: inline-block; min-width: 28px; font-weight: 700; }}
    table {{ width: 100%; border-collapse: collapse; }}
    th, td {{ border-top: 1px solid var(--line); padding: 9px; text-align: left; vertical-align: top; }}
    th {{ color: var(--muted); font-size: 13px; }}
    .hero {{ display: grid; gap: 14px; grid-template-columns: minmax(0, 1.5fr) minmax(240px, .8fr); align-items: stretch; }}
    .status {{ font-size: 18px; font-weight: 700; color: var(--ok); }}
    .plain {{ background: #eef6ff; border-color: #bfdbfe; }}
    .grid {{ display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 10px; }}
    .metric-card {{ border: 1px solid var(--line); border-radius: 8px; padding: 13px; min-height: 110px; background: #fbfcfe; }}
    .metric-card p {{ color: var(--muted); font-size: 13px; margin: 0 0 8px; }}
    .metric-card strong {{ display: block; font-size: 20px; margin-bottom: 6px; }}
    .metric-card span {{ color: var(--muted); font-size: 13px; }}
    .metric-card.ok strong {{ color: var(--ok); }}
    .metric-card.warn strong {{ color: var(--warn); }}
    .metric-card.bad strong {{ color: var(--bad); }}
    .bar-row {{ margin: 12px 0; }}
    .bar-label {{ display: flex; justify-content: space-between; gap: 12px; font-size: 13px; color: var(--muted); }}
    .bar-label strong {{ color: var(--ink); }}
    .bar-track {{ height: 12px; background: #e5e7eb; border-radius: 999px; overflow: hidden; margin-top: 6px; }}
    .bar-fill {{ height: 100%; background: var(--accent); border-radius: 999px; }}
    .timeline {{ display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; }}
    .timeline li {{ border: 1px solid var(--line); border-radius: 8px; padding: 10px; background: #fbfcfe; margin: 0; }}
    .timeline .done {{ border-color: #bbf7d0; background: #f0fdf4; }}
    .timeline .pending {{ border-color: #fed7aa; background: #fff7ed; }}
    .debug {{ background: #111827; color: #f9fafb; }}
    .debug th, .debug td {{ border-color: #374151; }}
    .debug th {{ color: #cbd5e1; }}
    .debug code {{ color: #bfdbfe; }}
    pre {{ white-space: pre-wrap; overflow-wrap: anywhere; background: #0b1020; color: #f9fafb; padding: 12px; border-radius: 8px; }}
    @media (max-width: 760px) {{ main {{ padding: 12px; }} .hero, .grid, .timeline {{ grid-template-columns: 1fr; }} h1 {{ font-size: 24px; }} }}
  </style>
</head>
<body>
<main>
  <header class="hero">
    <div>
      <h1>✅ SOLUM Agent Dashboard</h1>
      <p><strong>Патч:</strong> {escape(args.stage_patch)}</p>
      <p class="status">{escape(args.status)}</p>
      <p><strong>Готовность:</strong> {readiness}%</p>
      {bar("Progress", readiness, f"{readiness}%")}
    </div>
    <div>
      <p><strong>Context Load</strong></p>
      <p class="status">{escape(LOAD_LABELS[load])}</p>
      <p>Token Load Estimate: примерная оценка, без точных токенов.</p>
    </div>
  </header>
  <section class="plain">
    <h2>Простыми словами</h2>
    <p>Этот dashboard показывает, что сделал агент по текущему патчу, какие проверки прошли, что специально не трогалось и какие runtime/visual метрики доступны.</p>
    <p>{escape(metrics_issue or "Metrics JSON найден и прочитан.")}</p>
  </section>
  <section>
    <h2>Visual Cards</h2>
    <div class="grid">
      {card("Build", "not_run", "Gradle/build system вне scope", "warn")}
      {card("Runtime", "not_tested", "Android runtime не запускался", "warn")}
      {card("Vulkan", "not_touched", "Vulkan/render вне scope", "warn")}
      {card("Telegram", "OK" if telegram_done else "not_available", "summary + dashboard attachment", "ok" if telegram_done else "warn")}
      {card("Context Load", LOAD_LABELS[load], "без точных токенов", "neutral")}
    </div>
  </section>
  <section>
    <h2>Timeline</h2>
    <ul class="timeline">
      {timeline_step("docs read", True)}
      {timeline_step("patch", bool(changed))}
      {timeline_step("checks", bool(checks))}
      {timeline_step("PR", True)}
      {timeline_step("Telegram", telegram_done)}
    </ul>
  </section>
  <section><h2>Что сделал</h2><ul>{html_list(changed, "++")}</ul></section>
  <section><h2>Что проверил</h2><ul>{html_list(checks, "++")}</ul></section>
  <section><h2>Что не трогал</h2><ul>{html_list(not_touched, "--")}</ul></section>
  <section><h2>Проблемы</h2><ul>{html_list(problems, "!!")}</ul></section>
  <section><h2>Следующий шаг</h2><ul>{html_list(next_step, "->")}</ul></section>
  <section>
    <h2>Изменённые файлы</h2>
    <table>
      <thead><tr><th>#</th><th>Файл</th><th>Тип</th></tr></thead>
      <tbody>{files_table(files)}</tbody>
    </table>
  </section>
  <section>
    <h2>Диаграммы</h2>
    {bar("Checks", checks_value, checks_detail)}
    {metric_bar("FPS current", metrics.get("fps_current"))}
    {metric_bar("Quality", metrics.get("quality_score"))}
    {metric_bar("Material", metrics.get("material_score"))}
    {metric_bar("Shadow", metrics.get("shadow_score"))}
  </section>
  <section class="debug">
    <h2>Debug / Metrics</h2>
    <p><strong>Metrics file:</strong> <code>{escape(str(args.metrics))}</code></p>
    <p><strong>visual_status:</strong> {escape(visual_status)}</p>
    <p><strong>changed_files_count:</strong> {escape(format_metric(changed_count))}</p>
    <table>
      <thead><tr><th>Поле</th><th>Значение</th></tr></thead>
      <tbody>{metrics_rows}</tbody>
    </table>
  </section>
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
    parser.add_argument("--metrics", type=Path, default=DEFAULT_METRICS_INPUT)
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
