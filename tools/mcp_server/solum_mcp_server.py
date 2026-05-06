#!/usr/bin/env python3
"""SOLUM local MCP-style server/wrapper foundation.

No external packages are required. The wrapper exposes an explicit tool schema,
does not accept arbitrary shell commands, and delegates only to:

    tools/agent_tools/solum_tool_bridge.py --json
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
BRIDGE = REPO_ROOT / "tools" / "agent_tools" / "solum_tool_bridge.py"
SERVER_NAME = "solum-local-mcp-wrapper"
SERVER_VERSION = "0.1.0"


class McpWrapperError(RuntimeError):
    pass


@dataclass(frozen=True)
class ToolSpec:
    name: str
    description: str
    bridge_command: str
    input_schema: dict[str, Any]


COMMON_DRY_RUN_PROPERTY = {
    "type": "boolean",
    "description": "When true, do not perform side effects. Defaults to true for this wrapper.",
    "default": True,
}


TOOLS: dict[str, ToolSpec] = {
    "solum_print_status": ToolSpec(
        name="solum_print_status",
        description="Print repo/tool status through the SOLUM local bridge.",
        bridge_command="print-status",
        input_schema={
            "type": "object",
            "properties": {"dry_run": COMMON_DRY_RUN_PROPERTY},
            "additionalProperties": False,
        },
    ),
    "solum_latest_paths": ToolSpec(
        name="solum_latest_paths",
        description="Return latest report, build log, and diagnostics paths.",
        bridge_command="latest-paths",
        input_schema={
            "type": "object",
            "properties": {"dry_run": COMMON_DRY_RUN_PROPERTY},
            "additionalProperties": False,
        },
    ),
    "solum_generate_report": ToolSpec(
        name="solum_generate_report",
        description="Generate local TXT/HTML agent report via the SOLUM bridge.",
        bridge_command="generate-report",
        input_schema={
            "type": "object",
            "properties": {
                "dry_run": COMMON_DRY_RUN_PROPERTY,
                "stage_patch": {"type": "string"},
                "status": {"type": "string"},
                "changed": {"type": "string", "description": "Semicolon-separated change list."},
                "checks": {"type": "string", "description": "Semicolon-separated check list."},
                "not_touched": {"type": "string", "description": "Semicolon-separated out-of-scope list."},
                "problems": {"type": "string", "description": "Semicolon-separated known issue list."},
                "files": {"type": "string", "description": "Semicolon-separated file list."},
                "context_load": {"type": "string", "enum": ["AUTO", "LOW", "MEDIUM", "HIGH"]},
                "next_step": {"type": "string"},
            },
            "additionalProperties": False,
        },
    ),
    "solum_send_telegram_report": ToolSpec(
        name="solum_send_telegram_report",
        description="Dry-run or explicitly send latest Telegram report and attachments.",
        bridge_command="send-telegram-report",
        input_schema={
            "type": "object",
            "properties": {
                "dry_run": {
                    "type": "boolean",
                    "description": "Defaults to true. Real send requires send=true and dry_run=false.",
                    "default": True,
                },
                "send": {
                    "type": "boolean",
                    "description": "Explicitly request real Telegram send. Token is handled only by send_telegram_report.py.",
                    "default": False,
                },
            },
            "additionalProperties": False,
        },
    ),
    "solum_foundation_readiness": ToolSpec(
        name="solum_foundation_readiness",
        description="Run or dry-run foundation readiness through the SOLUM bridge.",
        bridge_command="foundation-readiness",
        input_schema={
            "type": "object",
            "properties": {
                "dry_run": COMMON_DRY_RUN_PROPERTY,
                "run_runner": {
                    "type": "boolean",
                    "description": "Explicitly run tools/agent_build_runner.sh through the bridge.",
                    "default": False,
                },
            },
            "additionalProperties": False,
        },
    ),
}


def emit_json(payload: dict[str, Any]) -> int:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))
    return 0


def error_payload(tool: str, dry_run: bool, message: str) -> dict[str, Any]:
    return {
        "ok": False,
        "tool": tool,
        "dry_run": dry_run,
        "result": None,
        "errors": [message],
    }


def tool_list_payload() -> dict[str, Any]:
    return {
        "server": {"name": SERVER_NAME, "version": SERVER_VERSION},
        "tools": [
            {
                "name": spec.name,
                "description": spec.description,
                "inputSchema": spec.input_schema,
            }
            for spec in TOOLS.values()
        ],
    }


def parse_bool(value: str) -> bool:
    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "y", "on"}:
        return True
    if normalized in {"0", "false", "no", "n", "off"}:
        return False
    raise McpWrapperError(f"invalid_boolean={value}")


def coerce_call_args(spec: ToolSpec, raw_args: dict[str, Any]) -> dict[str, Any]:
    schema_props = spec.input_schema.get("properties", {})
    allowed = set(schema_props)
    unknown = sorted(set(raw_args) - allowed)
    if unknown:
        raise McpWrapperError("unknown_arguments=" + ",".join(unknown))

    args = dict(raw_args)
    args.setdefault("dry_run", True)
    for key, definition in schema_props.items():
        if key not in args:
            continue
        if definition.get("type") == "boolean" and isinstance(args[key], str):
            args[key] = parse_bool(args[key])
        if definition.get("type") == "boolean" and not isinstance(args[key], bool):
            raise McpWrapperError(f"argument_must_be_boolean={key}")
        if definition.get("type") == "string" and not isinstance(args[key], str):
            raise McpWrapperError(f"argument_must_be_string={key}")
        enum = definition.get("enum")
        if enum and args[key] not in enum:
            raise McpWrapperError(f"argument_invalid_enum={key}")
    return args


def bridge_args_for(spec: ToolSpec, args: dict[str, Any]) -> list[str]:
    bridge_args = [sys.executable, str(BRIDGE), spec.bridge_command, "--json"]
    if args.get("dry_run", True):
        bridge_args.append("--dry-run")

    if spec.name == "solum_generate_report":
        mappings = {
            "stage_patch": "--stage-patch",
            "status": "--status",
            "changed": "--changed",
            "checks": "--checks",
            "not_touched": "--not-touched",
            "problems": "--problems",
            "files": "--files",
            "context_load": "--context-load",
            "next_step": "--next-step",
        }
        for key, flag in mappings.items():
            if key in args:
                bridge_args.extend([flag, str(args[key])])
    elif spec.name == "solum_send_telegram_report":
        if args.get("send", False):
            bridge_args.append("--send")
    elif spec.name == "solum_foundation_readiness":
        if args.get("run_runner", False):
            bridge_args.append("--run-runner")

    return bridge_args


def call_tool(tool: str, raw_args: dict[str, Any] | None = None) -> dict[str, Any]:
    if tool not in TOOLS:
        return error_payload(tool, True, "unknown_tool")
    spec = TOOLS[tool]
    raw_args = raw_args or {}
    try:
        args = coerce_call_args(spec, raw_args)
        dry_run = bool(args.get("dry_run", True))
        if spec.name == "solum_send_telegram_report" and args.get("send", False) and dry_run:
            return error_payload(tool, dry_run, "real_send_requires_dry_run_false")
        command = bridge_args_for(spec, args)
        result = subprocess.run(
            command,
            cwd=REPO_ROOT,
            check=False,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        errors: list[str] = []
        bridge_payload: Any
        try:
            bridge_payload = json.loads(result.stdout or "{}")
        except json.JSONDecodeError:
            bridge_payload = {"raw_stdout": result.stdout}
            errors.append("bridge_stdout_not_json")
        if result.stderr.strip():
            errors.append("bridge_stderr=" + result.stderr.strip())
        if result.returncode != 0:
            errors.append(f"bridge_exit_code={result.returncode}")
        if isinstance(bridge_payload, dict) and bridge_payload.get("errors"):
            errors.extend(str(item) for item in bridge_payload["errors"])
        return {
            "ok": result.returncode == 0 and not errors,
            "tool": tool,
            "dry_run": dry_run,
            "result": {
                "bridge_command": spec.bridge_command,
                "bridge": bridge_payload,
            },
            "errors": errors,
        }
    except McpWrapperError as exc:
        dry_run = True
        if raw_args and isinstance(raw_args.get("dry_run"), bool):
            dry_run = bool(raw_args["dry_run"])
        return error_payload(tool, dry_run, str(exc))


def parse_key_value_args(values: list[str]) -> dict[str, Any]:
    parsed: dict[str, Any] = {}
    for value in values:
        if "=" not in value:
            raise McpWrapperError(f"invalid_argument={value}")
        key, raw = value.split("=", 1)
        key = key.strip().replace("-", "_")
        raw = raw.strip()
        if raw.lower() in {"true", "false"}:
            parsed[key] = parse_bool(raw)
        else:
            parsed[key] = raw
    return parsed


def handle_json_rpc(request_obj: dict[str, Any]) -> dict[str, Any]:
    request_id = request_obj.get("id")
    method = request_obj.get("method")
    params = request_obj.get("params") or {}
    if method == "initialize":
        result = {
            "protocolVersion": "2024-11-05",
            "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION},
            "capabilities": {"tools": {}},
        }
    elif method == "tools/list":
        result = {"tools": tool_list_payload()["tools"]}
    elif method == "tools/call":
        name = params.get("name") if isinstance(params, dict) else None
        arguments = params.get("arguments", {}) if isinstance(params, dict) else {}
        if not isinstance(name, str) or not isinstance(arguments, dict):
            return {"jsonrpc": "2.0", "id": request_id, "error": {"code": -32602, "message": "invalid_params"}}
        payload = call_tool(name, arguments)
        result = {
            "content": [{"type": "text", "text": json.dumps(payload, ensure_ascii=False)}],
            "isError": not payload["ok"],
        }
    else:
        return {"jsonrpc": "2.0", "id": request_id, "error": {"code": -32601, "message": "method_not_found"}}
    return {"jsonrpc": "2.0", "id": request_id, "result": result}


def serve_stdio() -> int:
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            request_obj = json.loads(line)
            if not isinstance(request_obj, dict):
                raise ValueError("request must be object")
            response = handle_json_rpc(request_obj)
        except (json.JSONDecodeError, ValueError) as exc:
            response = {"jsonrpc": "2.0", "id": None, "error": {"code": -32700, "message": str(exc)}}
        print(json.dumps(response, ensure_ascii=False), flush=True)
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="SOLUM local MCP-style server/wrapper foundation.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("list-tools", help="Print explicit MCP tool schema.")
    subparsers.add_parser("serve-stdio", help="Run a minimal MCP-style JSON-RPC stdio loop.")

    call = subparsers.add_parser("call", help="Call one explicit SOLUM MCP tool.")
    call.add_argument("tool", choices=sorted(TOOLS), help="Tool name.")
    call.add_argument("--dry-run", action="store_true", help="Disable side effects for this call.")
    call.add_argument("--no-dry-run", action="store_true", help="Allow side effects when the tool supports them.")
    call.add_argument("--send", action="store_true", help="Explicitly request Telegram send for solum_send_telegram_report.")
    call.add_argument(
        "--arg",
        action="append",
        default=[],
        metavar="KEY=VALUE",
        help="Tool argument. Only schema keys are accepted.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.command == "list-tools":
        return emit_json(tool_list_payload())
    if args.command == "serve-stdio":
        return serve_stdio()
    if args.command == "call":
        try:
            tool_args = parse_key_value_args(args.arg)
            if args.no_dry_run:
                tool_args["dry_run"] = False
            else:
                tool_args["dry_run"] = True if args.dry_run or "dry_run" not in tool_args else tool_args["dry_run"]
            if args.send:
                tool_args["send"] = True
            return emit_json(call_tool(args.tool, tool_args))
        except McpWrapperError as exc:
            return emit_json(error_payload(args.tool, True, str(exc)))
    raise McpWrapperError(f"unknown_command={args.command}")


if __name__ == "__main__":
    raise SystemExit(main())
