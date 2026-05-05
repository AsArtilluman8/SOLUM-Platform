#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import shutil
import tempfile
import time
from pathlib import Path
from typing import Any


def now_stamp() -> str:
    return time.strftime("%Y%m%d_%H%M%S")


def atomic_write_text(path: Path, text: str) -> dict[str, Any]:
    path.parent.mkdir(parents=True, exist_ok=True)
    backup_path = None

    fd, tmp_name = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=str(path.parent))
    tmp_path = Path(tmp_name)

    report: dict[str, Any] = {
        "schema": "solum.save_report",
        "schemaVersion": 1,
        "target": str(path),
        "temp": str(tmp_path),
        "backup": None,
        "status": "pending",
        "error": None
    }

    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(text)
            f.flush()
            os.fsync(f.fileno())

        if path.exists():
            backup_path = path.with_name(path.name + f".bak_{now_stamp()}")
            shutil.copy2(path, backup_path)
            report["backup"] = str(backup_path)

        os.replace(tmp_path, path)
        report["status"] = "valid"
        return report
    except Exception as exc:
        report["status"] = "invalid"
        report["error"] = str(exc)
        try:
            if tmp_path.exists():
                tmp_path.unlink()
        except Exception:
            pass
        return report


def atomic_write_json(path: Path, data: Any) -> dict[str, Any]:
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    return atomic_write_text(path, text)
