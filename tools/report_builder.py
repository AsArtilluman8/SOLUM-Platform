#!/usr/bin/env python3
from __future__ import annotations

import html
import json
import sys
from pathlib import Path
from typing import Any


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        return {"_error": str(exc)}


def read_text(path: Path, max_chars: int = 16000) -> str:
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except Exception as exc:
        return f"Не удалось прочитать {path.name}: {exc}"
    if len(text) > max_chars:
        return text[:max_chars] + "\n... обрезано ..."
    return text


def detect_vulkan(vulkan: Any) -> dict:
    result = {
        "rawStatus": "unknown",
        "uiStatus": "failed",
        "deviceName": "",
        "deviceType": "",
        "isSoftware": False,
        "title": "✗ Vulkan capability check не выполнен.",
        "explain": "Vulkan-данные не получены. Нужно смотреть vulkan_caps_build_log.txt внутри ZIP.",
        "next": "Пришли SOLUM_LATEST_DIAGNOSTICS.zip, чтобы найти причину.",
    }

    if not isinstance(vulkan, dict):
        return result

    result["rawStatus"] = str(vulkan.get("status", "unknown"))
    devices = vulkan.get("devices") or []
    if devices and isinstance(devices[0], dict):
        d = devices[0]
        result["deviceName"] = str(d.get("deviceName", ""))
        result["deviceType"] = str(d.get("deviceType", ""))

    name_l = result["deviceName"].lower()
    type_l = result["deviceType"].lower()
    is_software = (
        "llvmpipe" in name_l
        or "lavapipe" in name_l
        or "software" in name_l
        or type_l == "cpu"
    )
    result["isSoftware"] = is_software

    if result["rawStatus"].lower() == "ok" and is_software:
        result["uiStatus"] = "partial"
        result["title"] = "⚠ Vulkan найден, но это software renderer через CPU."
        result["explain"] = (
            "Диагностика сейчас видит llvmpipe/lavapipe/CPU. "
            "Это не настоящий Mali-G57. Такой результат нельзя использовать как финальные GPU capabilities."
        )
        result["next"] = (
            "Следующий правильный шаг: Android native Vulkan caps runner внутри apps/engine, "
            "чтобы проверить настоящий vendor Vulkan driver телефона."
        )
    elif result["rawStatus"].lower() == "ok":
        result["uiStatus"] = "ok"
        result["title"] = "✓ Vulkan capability check выполнен."
        result["explain"] = "Vulkan-устройство найдено. Нужно проверить, что это реальный GPU, а не software renderer."
        result["next"] = "Можно использовать ZIP как базовую диагностику Patch 02."
    return result


def status_badge(status: str) -> str:
    cls = {
        "ok": "ok",
        "partial": "warn",
        "warning": "warn",
        "failed": "error",
        "error": "error",
    }.get(status.lower(), "pending")
    label = {
        "ok": "OK",
        "partial": "PARTIAL",
        "failed": "FAILED",
    }.get(status.lower(), status.upper())
    return f'<span class="badge {cls}">{html.escape(label)}</span>'


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: report_builder.py <diagnostics_work_dir> <output_html>")
        return 2

    work = Path(sys.argv[1])
    out = Path(sys.argv[2])

    device = load_json(work / "device_info.json")
    env = load_json(work / "env_info.json")
    git = load_json(work / "git_state.json")
    storage = load_json(work / "storage_state.json")
    vulkan = load_json(work / "vulkan_caps.json")
    perf = load_json(work / "performance_history.json")
    vk = detect_vulkan(vulkan)

    phone_name = ""
    if isinstance(device, dict):
        phone_name = " ".join(
            x for x in [
                str(device.get("manufacturer", "")),
                str(device.get("model", "")),
            ] if x
        ).strip()

    css = """
    body { margin: 0; font-family: system-ui, sans-serif; background: #071114; color: #d8f7ff; }
    header { padding: 20px; border-bottom: 1px solid #16424a; background: #0b1b20; }
    main { padding: 16px; display: grid; gap: 16px; }
    section { border: 1px solid #16424a; background: #0b171b; border-radius: 14px; padding: 14px; }
    h1, h2 { margin: 0 0 10px; }
    h1 { font-size: 24px; }
    h2 { font-size: 17px; color: #79e9ff; }
    p { line-height: 1.45; }
    pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #050b0d; padding: 12px; border-radius: 10px; color: #cdeff5; }
    details { background: #061013; border: 1px solid #12333a; border-radius: 10px; padding: 10px; }
    summary { cursor: pointer; color: #79e9ff; font-weight: 700; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; }
    .kv { background: #081316; border: 1px solid #12333a; padding: 10px; border-radius: 10px; min-width: 0; }
    .k { color: #84aab2; font-size: 12px; }
    .v { font-weight: 650; margin-top: 4px; overflow-wrap: anywhere; word-break: break-word; }
    .badge { display: inline-block; padding: 4px 8px; border-radius: 999px; font-weight: 700; font-size: 12px; }
    .ok { background: #103d24; color: #7dffad; }
    .warn { background: #3d2b10; color: #ffd07d; }
    .error { background: #421819; color: #ff9b9b; }
    .pending { background: #24323a; color: #b7c9d0; }
    .big { font-size: 18px; font-weight: 800; }
    """

    def kv(label: str, value: Any) -> str:
        return (
            '<div class="kv">'
            f'<div class="k">{html.escape(label)}</div>'
            f'<div class="v">{html.escape(str(value if value is not None else ""))}</div>'
            '</div>'
        )

    checklist = "\n".join([
        "✓ ZIP диагностики создан",
        "✓ HTML отчёт создан",
        "✓ Git/env/device/storage данные собраны",
        "✓ vulkan_caps.json создан",
        "⚠ Если Vulkan device = llvmpipe/lavapipe/CPU — это не Mali-G57",
    ])

    what_send = "\n".join([
        "Обычно присылай только один файл:",
        "/storage/emulated/0/SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip",
        "",
        "Скриншот нужен только для визуальных проблем: UI, тени, материалы, мерцание, модели.",
    ])

    html_doc = f"""<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>SOLUM Diagnostics v1</title>
<style>{css}</style>
</head>
<body>
<header>
<h1>SOLUM Diagnostics v1</h1>
<div>Vulkan check: {status_badge(vk["uiStatus"])}</div>
</header>
<main>
<section>
<h2>Итог простыми словами</h2>
<p class="big">{html.escape(vk["title"])}</p>
<p>{html.escape(vk["explain"])}</p>
<p>{html.escape(vk["next"])}</p>
</section>

<section>
<h2>Короткая сводка</h2>
<div class="grid">
{kv("Телефон", phone_name)}
{kv("Vulkan device", vk["deviceName"] or "unknown")}
{kv("Vulkan device type", vk["deviceType"] or "unknown")}
{kv("Git branch", git.get("branch") if isinstance(git, dict) else "")}
{kv("Git commit", git.get("commit") if isinstance(git, dict) else "")}
{kv("SOLUM root", storage.get("solumRoot") if isinstance(storage, dict) else "")}
</div>
</section>

<section>
<h2>Что хорошо / что важно</h2>
<pre>{html.escape(checklist)}</pre>
</section>

<section>
<h2>Какой файл присылать</h2>
<pre>{html.escape(what_send)}</pre>
</section>

<section>
<h2>Advanced: Vulkan capabilities JSON</h2>
<details><summary>Показать технические данные</summary>
<pre>{html.escape(json.dumps(vulkan, indent=2, ensure_ascii=False))}</pre>
</details>
</section>

<section>
<h2>Advanced: device_info.json</h2>
<details><summary>Показать</summary>
<pre>{html.escape(json.dumps(device, indent=2, ensure_ascii=False))}</pre>
</details>
</section>

<section>
<h2>Advanced: env_info.json</h2>
<details><summary>Показать</summary>
<pre>{html.escape(json.dumps(env, indent=2, ensure_ascii=False))}</pre>
</details>
</section>

<section>
<h2>Advanced: git_state.json</h2>
<details><summary>Показать</summary>
<pre>{html.escape(json.dumps(git, indent=2, ensure_ascii=False))}</pre>
</details>
</section>

<section>
<h2>Known issues</h2>
<pre>{html.escape(read_text(work / "known_issues.txt"))}</pre>
</section>

<section>
<h2>Build log</h2>
<pre>{html.escape(read_text(work / "build_log.txt"))}</pre>
</section>
</main>
</body>
</html>
"""

    out.write_text(html_doc, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
