"""Sign in the local DEMO account. Prints the profile only, never the password.

Reads DEMO_CAMPUS_ID and DEMO_PASSWORD from the environment or the repo .env.
Writes the access token to backend/.demo-session.json, which is gitignored.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env"
SESSION = ROOT / "backend" / ".demo-session.json"
API = os.environ.get("APP_BASE_URL", "http://127.0.0.1:8080").rstrip("/") + "/api/v1"


def load_env(path: Path) -> None:
    if not path.is_file():
        return
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        if line.startswith("export "):
            line = line[7:].strip()
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def main() -> int:
    load_env(ENV_FILE)
    campus_id = os.environ.get("DEMO_CAMPUS_ID", "").strip()
    password = os.environ.get("DEMO_PASSWORD", "").strip()
    if not campus_id or not password:
        print("DEMO_CAMPUS_ID and DEMO_PASSWORD are required", file=sys.stderr)
        return 2
    body = json.dumps(
        {"mode": "CAMPUS_PASSWORD", "campusId": campus_id, "password": password}
    ).encode("utf-8")
    request = urllib.request.Request(
        API + "/auth/login",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=8) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.URLError as ex:
        print("login failed: backend unreachable", file=sys.stderr)
        print(ex.__class__.__name__, file=sys.stderr)
        return 1
    if payload.get("code") != 0:
        print(f"login failed: code={payload.get('code')}", file=sys.stderr)
        return 1
    data = payload["data"]
    user = data["user"]
    SESSION.write_text(
        json.dumps(
            {
                "accessToken": data["accessToken"],
                "refreshToken": data["refreshToken"],
                "expiresIn": data["expiresIn"],
                "userId": user["id"],
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"userId={user['id']} nickname={user['nickname']} expiresIn={data['expiresIn']}")
    print(f"session={SESSION}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
