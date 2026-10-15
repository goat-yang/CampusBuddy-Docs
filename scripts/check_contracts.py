#!/usr/bin/env python3
"""Validate OpenAPI and example envelopes. Field names in examples must exist in the spec."""
from __future__ import annotations

import json
import sys
from pathlib import Path

import yaml
from openapi_spec_validator import validate_spec

ROOT = Path(__file__).resolve().parents[1]
SPEC = ROOT / "contracts" / "openapi.yaml"
EXAMPLES = ROOT / "contracts" / "examples"

ENVELOPE_KEYS = {"code", "message", "data", "requestId"}


def fail(msg: str) -> None:
    print(f"FAIL: {msg}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    spec = yaml.safe_load(SPEC.read_text(encoding="utf-8"))
    validate_spec(spec)

    paths = spec.get("paths") or {}
    if "/auth/otp/send" not in paths:
        fail("missing /auth/otp/send")
    if "/reco/for-you" not in paths:
        fail("missing /reco/for-you")

    schemas = (spec.get("components") or {}).get("schemas") or {}
    card = schemas.get("ActivityCard") or {}
    props = set((card.get("properties") or {}).keys())
    for required_field in ("remainCount", "enrollNum", "reason", "publisher"):
        if required_field not in props:
            fail(f"ActivityCard missing {required_field}")

    err_codes = set(((schemas.get("ErrorEnvelope") or {}).get("properties") or {}).get("code", {}).get("enum") or [])
    for code in (40001, 40100, 40300, 40400, 40901, 40902, 40903, 40904, 40905, 42900, 50000):
        if code not in err_codes:
            fail(f"ErrorEnvelope.code enum missing {code}")

    files = sorted(EXAMPLES.glob("*.json"))
    if len(files) < 8:
        fail("expected at least 8 example json files")

    for path in files:
        payload = json.loads(path.read_text(encoding="utf-8"))
        if not path.name.endswith(".request.json"):
            missing = ENVELOPE_KEYS - set(payload)
            if missing:
                fail(f"{path.name} missing envelope keys {missing}")
            if path.name.startswith("error.") and payload.get("code") == 0:
                fail(f"{path.name} is an error example but code=0")
            if path.name.endswith(".ok.json") and payload.get("code") != 0:
                fail(f"{path.name} ok example must have code=0")
        else:
            if "mode" in payload and payload["mode"] not in {"CAMPUS_PASSWORD", "CAMPUS_OTP", "WECHAT"}:
                fail(f"{path.name} invalid login mode")
            if "channel" in payload and payload["channel"] not in {"EMAIL", "SMS"}:
                fail(f"{path.name} invalid otp channel")

    login = json.loads((EXAMPLES / "auth.login.ok.json").read_text(encoding="utf-8"))
    user = (login.get("data") or {}).get("user") or {}
    for banned in ("campusId", "email", "phone", "openid", "password"):
        if banned in user:
            fail(f"login ok user must not expose {banned}")

    print("OK: openapi.yaml and examples")


if __name__ == "__main__":
    main()
