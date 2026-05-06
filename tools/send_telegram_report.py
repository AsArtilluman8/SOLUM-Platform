#!/usr/bin/env python3
"""Send the latest SOLUM Telegram report through Telegram Bot API.

Secrets are loaded only from ~/.solum/secrets/telegram.env. The bot token is
never printed.
"""

from __future__ import annotations

import argparse
import json
import uuid
from pathlib import Path
from urllib import error, parse, request


SECRET_FILE = Path.home() / ".solum" / "secrets" / "telegram.env"
REPORT_FILE = Path("_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt")
HTML_REPORT_FILE = Path("_work/agent_reports/latest/SOLUM_AGENT_REPORT.html")
TELEGRAM_API_BASE = "https://api.telegram.org"
ALLOWED_KEYS = {"TELEGRAM_BOT_TOKEN", "TELEGRAM_CHAT_ID"}
MAX_TELEGRAM_MESSAGE_CHARS = 3900


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


def read_optional_text(path: Path) -> str | None:
    if not path.exists() or not path.is_file():
        return None
    text = path.read_text(encoding="utf-8").strip()
    return text or None


def existing_regular_files(paths: list[Path]) -> list[Path]:
    return [path for path in paths if path.exists() and path.is_file()]


def build_summary(text_report: str | None, html_path: Path, txt_path: Path) -> str:
    missing: list[str] = []
    if not html_path.is_file():
        missing.append(f"!! HTML-отчёт не найден: {html_path}")
    if not txt_path.is_file():
        missing.append(f"!! TXT-отчёт не найден: {txt_path}")

    if text_report:
        summary = text_report
    else:
        summary = "\n".join(
            [
                "✅ SOLUM Agent Report",
                "",
                "Патч: unknown",
                "Статус: отчётные файлы не найдены",
                "",
                "Проблемы:",
            ]
        )

    if missing:
        summary = f"{summary.rstrip()}\n\nПроблемы:\n" + "\n".join(missing)

    if len(summary) > MAX_TELEGRAM_MESSAGE_CHARS:
        return summary[: MAX_TELEGRAM_MESSAGE_CHARS - 40].rstrip() + "\n\n!! Summary сокращён для Telegram"
    return summary


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


def send_document(token: str, chat_id: str, path: Path, timeout_seconds: int) -> dict[str, object]:
    endpoint = f"{TELEGRAM_API_BASE}/bot{parse.quote(token, safe='')}/sendDocument"
    boundary = f"----solum-{uuid.uuid4().hex}"
    file_bytes = path.read_bytes()
    filename = path.name
    parts = [
        (
            f"--{boundary}\r\n"
            'Content-Disposition: form-data; name="chat_id"\r\n\r\n'
            f"{chat_id}\r\n"
        ).encode("utf-8"),
        (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="document"; filename="{filename}"\r\n'
            "Content-Type: application/octet-stream\r\n\r\n"
        ).encode("utf-8"),
        file_bytes,
        f"\r\n--{boundary}--\r\n".encode("utf-8"),
    ]
    req = request.Request(
        endpoint,
        data=b"".join(parts),
        method="POST",
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    try:
        with request.urlopen(req, timeout=timeout_seconds) as response:
            body = response.read().decode("utf-8")
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        safe_body = body.replace(token, "<redacted-token>")
        raise TelegramReportError(f"telegram_document_http_error={exc.code} file={filename} body={safe_body}") from None
    except error.URLError as exc:
        reason = str(exc.reason).replace(token, "<redacted-token>")
        raise TelegramReportError(f"telegram_document_network_error={reason} file={filename}") from None

    try:
        data = json.loads(body)
    except json.JSONDecodeError as exc:
        raise TelegramReportError(f"telegram_document_bad_json={exc} file={filename}") from None

    if not data.get("ok"):
        safe_data = json.dumps(data, ensure_ascii=False).replace(token, "<redacted-token>")
        raise TelegramReportError(f"telegram_document_api_error={safe_data} file={filename}")
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
    text = read_optional_text(REPORT_FILE)
    summary = build_summary(text, HTML_REPORT_FILE, REPORT_FILE)
    documents = existing_regular_files([HTML_REPORT_FILE, REPORT_FILE])

    if args.dry_run:
        print("dry_run=ok")
        print(f"secret_file={SECRET_FILE}")
        print("telegram_bot_token=present_redacted")
        print("telegram_chat_id=present")
        print(f"summary_chars={len(summary)}")
        print(f"html_report={'present' if HTML_REPORT_FILE in documents else 'missing'} path={HTML_REPORT_FILE}")
        print(f"txt_report={'present' if REPORT_FILE in documents else 'missing'} path={REPORT_FILE}")
        return 0

    result = send_message(
        token=config["TELEGRAM_BOT_TOKEN"],
        chat_id=config["TELEGRAM_CHAT_ID"],
        text=summary,
        timeout_seconds=args.timeout_seconds,
    )
    message = result.get("result", {})
    message_id = message.get("message_id") if isinstance(message, dict) else None
    print("send=success")
    if message_id is not None:
        print(f"message_id={message_id}")
    if not documents:
        print("documents=none")
        return 0
    for document in documents:
        doc_result = send_document(
            token=config["TELEGRAM_BOT_TOKEN"],
            chat_id=config["TELEGRAM_CHAT_ID"],
            path=document,
            timeout_seconds=args.timeout_seconds,
        )
        doc_message = doc_result.get("result", {})
        doc_message_id = doc_message.get("message_id") if isinstance(doc_message, dict) else None
        print(f"document=success path={document}")
        if doc_message_id is not None:
            print(f"document_message_id={doc_message_id}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except TelegramReportError as exc:
        print(f"send_telegram_report_error={exc}")
        raise SystemExit(1)
