from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any


TYPE_MAP = {
    "object": dict,
    "array": list,
    "string": str,
    "integer": int,
    "number": (int, float),
    "boolean": bool,
    "null": type(None),
}


def validate_schema(value: Any, schema: dict[str, Any], path: str = "$") -> list[str]:
    """Validate the strict JSON-Schema subset shipped with the reader.

    Unknown schema keywords are ignored, but every keyword used by the local
    schemas is enforced. This avoids a network/package dependency in Termux.
    """
    errors: list[str] = []
    expected_type = schema.get("type")
    if expected_type:
        expected = TYPE_MAP.get(expected_type)
        if expected is None:
            errors.append(f"{path}: unsupported schema type {expected_type}")
            return errors
        if expected_type in ("integer", "number") and isinstance(value, bool):
            errors.append(f"{path}: boolean is not {expected_type}")
            return errors
        if not isinstance(value, expected):
            errors.append(f"{path}: expected {expected_type}, got {type(value).__name__}")
            return errors
    if "const" in schema and value != schema["const"]:
        errors.append(f"{path}: value does not equal const {schema['const']!r}")
    if "enum" in schema and value not in schema["enum"]:
        errors.append(f"{path}: value {value!r} is outside enum")
    if isinstance(value, str):
        if len(value) < int(schema.get("minLength", 0)):
            errors.append(f"{path}: string shorter than minLength")
        pattern = schema.get("pattern")
        if pattern and re.search(pattern, value) is None:
            errors.append(f"{path}: string does not match {pattern!r}")
    if isinstance(value, dict):
        for name in schema.get("required", []):
            if name not in value:
                errors.append(f"{path}: missing required property {name}")
        properties = schema.get("properties", {})
        for name, child_schema in properties.items():
            if name in value:
                errors.extend(validate_schema(value[name], child_schema, f"{path}.{name}"))
    if isinstance(value, list) and isinstance(schema.get("items"), dict):
        for index, item in enumerate(value):
            errors.extend(validate_schema(item, schema["items"], f"{path}[{index}]"))
    return errors


def load_schema(schema_dir: Path, name: str) -> dict[str, Any]:
    return json.loads((schema_dir / name).read_text(encoding="utf-8"))


def validate_json_file(path: Path, schema_path: Path) -> list[str]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        return [f"{path}: {type(exc).__name__}: {exc}"]
    return validate_schema(value, schema)
