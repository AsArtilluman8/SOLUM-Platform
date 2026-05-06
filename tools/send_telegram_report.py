#!/usr/bin/env python3
"""Send the latest SOLUM Telegram report through Telegram Bot API.

Secrets are loaded only from ~/.solum/secrets/telegram.env. The bot token is
never printed.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from urllib import error, parse, request


SECRET_FILE = Path.home() / ".solum" / "secrets" / "telegram.env"
REPORT_FILE = Path("_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt")
TELEGRAM_API_BASE = "https://api.telegram.org"
ALLOWED_KEYS = {"TELEGRAM_BOT_TOKEN", "TELEGRAM_CHAT_ID"}


class TelegramReportError(RuntimeError):
    pass


def _strip_optional_quotes(value: str) -> str:
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        return value[1:-1]
    return value


def load_telegram_config(path: Path = SECRET_FILE) -> dict[str, str]:
    if not path.exists():
        raise TelegramReportError(f"secret_file_missing={path}")
    if not path.is_file():
        raise TelegramReportError(f"secret_file_not_regular={path}")

    values: dict[str, str] = {}
    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export ") :].strip()
        if "=" not in line:
            raise TelegramReportError(f"invalid_secret_line={line_number}")
        key, value = line.split("=", 1)
        key = key.strip()
        if key not in ALLOWED_KEYS:
            continue
        values[key] = _strip_optional_quotes(value.strip())

    missing = sorted(key for key in ALLOWED_KEYS if not values.get(key))
    if missing:
        raise TelegramReportError(f"missing_secret_keys={','.join(missing)}")
    return values


def load_report(path: Path = REPORT_FILE) -> str:
    if not path.exists():
        raise TelegramReportError(f"report_missing={path}")
    if not path.is_file():
        raise TelegramReportError(f"report_not_regular={path}")
    text = path.read_text(encoding="utf-8").strip()
    if not text:
        raise TelegramReportError(f"report_empty={path}")
    return text


def send_message(token: str, chat_id: str, text: str, timeout_seconds: int) -> dict[str, object]:
    endpoint = f"{TELEGRAM_API_BASE}/bot{parse.quote(token, safe='')}/sendMessage"
    payload = parse.urlencode(
        {
            "chat_id": chat_id,
            "text": text,
            "disable_web_page_preview": "true",
        }
    ).encode("utf-8")
    req = request.Request(
        endpoint,
        data=payload,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    try:
        with request.urlopen(req, timeout=timeout_seconds) as response:
            body = response.read().decode("utf-8")
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        safe_body = body.replace(token, "<redacted-token>")
        raise TelegramReportError(f"telegram_http_error={exc.code} body={safe_body}") from None
    except error.URLError as exc:
        reason = str(exc.reason).replace(token, "<redacted-token>")
        raise TelegramReportError(f"telegram_network_error={reason}") from None

    try:
        data = json.loads(body)
    except json.JSONDecodeError as exc:
        raise TelegramReportError(f"telegram_bad_json={exc}") from None

    if not data.get("ok"):
        safe_data = json.dumps(data, ensure_ascii=False).replace(token, "<redacted-token>")
        raise TelegramReportError(f"telegram_api_error={safe_data}")
    return data


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Send latest SOLUM agent report to Telegram.")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--dry-run", action="store_true", help="Validate config/report without sending.")
    mode.add_argument("--send", action="store_true", help="Send report to Telegram.")
    parser.add_argument("--timeout-seconds", type=int, default=20)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    config = load_telegram_config()
    text = load_report()

    if args.dry_run:
        print("dry_run=ok")
        print(f"secret_file={SECRET_FILE}")
        print("telegram_bot_token=present_redacted")
        print("telegram_chat_id=present")
        print(f"report_file={REPORT_FILE}")
        print(f"report_chars={len(text)}")
        return 0

    result = send_message(
        token=config["TELEGRAM_BOT_TOKEN"],
        chat_id=config["TELEGRAM_CHAT_ID"],
        text=text,
        timeout_seconds=args.timeout_seconds,
    )
    message = result.get("result", {})
    message_id = message.get("message_id") if isinstance(message, dict) else None
    print("send=success")
    if message_id is not None:
        print(f"message_id={message_id}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except TelegramReportError as exc:
        print(f"send_telegram_report_error={exc}")
        raise SystemExit(1)
