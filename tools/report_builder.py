#!/usr/bin/env python3
from __future__ import annotations

import html
import json
import sys
from pathlib import Path
from typing import Any


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except Exception as exc:
        return {'_error': str(exc)}


def read_text(path: Path, max_chars: int = 16000) -> str:
    try:
        text = path.read_text(encoding='utf-8', errors='replace')
    except Exception as exc:
        return f'Could not read {path.name}: {exc}'
    if len(text) > max_chars:
        return text[:max_chars] + '\n... truncated ...'
    return text


def status_badge(status: str) -> str:
    s = (status or 'unknown').lower()
    cls = 'pending'
    if s in ('ok', 'success', 'valid'):
        cls = 'ok'
    elif s in ('failed', 'error', 'invalid'):
        cls = 'error'
    elif s in ('warning', 'partial'):
        cls = 'warn'
    return f'<span class="badge {cls}">{html.escape(status or "unknown")}</span>'


def main() -> int:
    if len(sys.argv) != 3:
        print('usage: report_builder.py <diagnostics_work_dir> <output_html>')
        return 2

    work = Path(sys.argv[1])
    out = Path(sys.argv[2])
    device = load_json(work / 'device_info.json')
    env = load_json(work / 'env_info.json')
    git = load_json(work / 'git_state.json')
    storage = load_json(work / 'storage_state.json')
    vulkan = load_json(work / 'vulkan_caps.json')
    perf = load_json(work / 'performance_history.json')

    vulkan_status = vulkan.get('status', 'unknown') if isinstance(vulkan, dict) else 'unknown'
    device_name = ''
    if isinstance(vulkan, dict):
        devices = vulkan.get('devices') or []
        if devices and isinstance(devices[0], dict):
            device_name = devices[0].get('deviceName', '')
    if not device_name and isinstance(device, dict):
        device_name = ' '.join(x for x in [device.get('manufacturer', ''), device.get('model', '')] if x)

    css = '''
    body { margin: 0; font-family: system-ui, sans-serif; background: #071114; color: #d8f7ff; }
    header { padding: 20px; border-bottom: 1px solid #16424a; background: #0b1b20; }
    main { padding: 16px; display: grid; gap: 16px; }
    section { border: 1px solid #16424a; background: #0b171b; border-radius: 14px; padding: 14px; }
    h1, h2 { margin: 0 0 10px; }
    h1 { font-size: 22px; }
    h2 { font-size: 16px; color: #79e9ff; }
    pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #050b0d; padding: 12px; border-radius: 10px; color: #cdeff5; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
    .kv { background: #081316; border: 1px solid #12333a; padding: 10px; border-radius: 10px; }
    .k { color: #84aab2; font-size: 12px; }
    .v { font-weight: 650; margin-top: 4px; }
    .badge { display: inline-block; padding: 4px 8px; border-radius: 999px; font-weight: 700; font-size: 12px; }
    .ok { background: #103d24; color: #7dffad; }
    .warn { background: #3d2b10; color: #ffd07d; }
    .error { background: #421819; color: #ff9b9b; }
    .pending { background: #24323a; color: #b7c9d0; }
    '''

    def kv(label: str, value: Any) -> str:
        return '<div class="kv"><div class="k">{}</div><div class="v">{}</div></div>'.format(
            html.escape(label), html.escape(str(value if value is not None else ''))
        )

    html_doc = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SOLUM Diagnostics v1</title><style>{css}</style></head>
<body>
<header>
<h1>SOLUM Diagnostics v1</h1>
<div>Vulkan caps: {status_badge(str(vulkan_status))}</div>
</header>
<main>
<section><h2>Summary</h2><div class="grid">
{kv('Device', device_name)}
{kv('Git branch', git.get('branch') if isinstance(git, dict) else '')}
{kv('Git commit', git.get('commit') if isinstance(git, dict) else '')}
{kv('SOLUM root', storage.get('solumRoot') if isinstance(storage, dict) else '')}
{kv('Free bytes', storage.get('freeBytes') if isinstance(storage, dict) else '')}
</div></section>
<section><h2>Vulkan capabilities</h2><pre>{html.escape(json.dumps(vulkan, indent=2, ensure_ascii=False))}</pre></section>
<section><h2>Device info</h2><pre>{html.escape(json.dumps(device, indent=2, ensure_ascii=False))}</pre></section>
<section><h2>Environment info</h2><pre>{html.escape(json.dumps(env, indent=2, ensure_ascii=False))}</pre></section>
<section><h2>Git state</h2><pre>{html.escape(json.dumps(git, indent=2, ensure_ascii=False))}</pre></section>
<section><h2>Performance history</h2><pre>{html.escape(json.dumps(perf, indent=2, ensure_ascii=False))}</pre></section>
<section><h2>Known issues</h2><pre>{html.escape(read_text(work / 'known_issues.txt'))}</pre></section>
<section><h2>Build log</h2><pre>{html.escape(read_text(work / 'build_log.txt'))}</pre></section>
</main></body></html>'''

    out.write_text(html_doc, encoding='utf-8')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
