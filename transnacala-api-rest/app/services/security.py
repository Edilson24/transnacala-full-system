import base64
import hashlib
import hmac
import json
import secrets
import time
from collections.abc import Mapping


PASSWORD_ITERATIONS = 310_000
MIN_PASSWORD_ITERATIONS = 100_000
MAX_PASSWORD_ITERATIONS = 1_000_000


def hash_password(password: str) -> str:
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac(
        "sha256", password.encode("utf-8"), salt, PASSWORD_ITERATIONS
    )
    return "$".join(
        (
            "pbkdf2_sha256",
            str(PASSWORD_ITERATIONS),
            base64.urlsafe_b64encode(salt).decode("ascii").rstrip("="),
            base64.urlsafe_b64encode(digest).decode("ascii").rstrip("="),
        )
    )


def verify_password(password: str, encoded: str) -> bool:
    try:
        algorithm, rounds, salt_text, expected_text = encoded.split("$", 3)
        if algorithm != "pbkdf2_sha256":
            return False
        iterations = int(rounds)
        if not MIN_PASSWORD_ITERATIONS <= iterations <= MAX_PASSWORD_ITERATIONS:
            return False
        salt = _decode(salt_text)
        expected = _decode(expected_text)
        actual = hashlib.pbkdf2_hmac(
            "sha256", password.encode("utf-8"), salt, iterations
        )
        return hmac.compare_digest(actual, expected)
    except (ValueError, TypeError):
        return False


def create_access_token(
    subject: str, secret: bytes, expires_in: int
) -> str:
    issued_at = int(time.time())
    header = _encode_json({"alg": "HS256", "typ": "JWT"})
    payload = _encode_json(
        {"sub": subject, "iat": issued_at, "exp": issued_at + expires_in}
    )
    signing_input = f"{header}.{payload}"
    signature = hmac.new(
        secret, signing_input.encode("ascii"), hashlib.sha256
    ).digest()
    return f"{signing_input}.{_encode(signature)}"


def verify_access_token(token: str, secret: bytes) -> Mapping[str, object] | None:
    try:
        header_text, payload_text, signature_text = token.split(".")
        header = json.loads(_decode(header_text))
        payload = json.loads(_decode(payload_text))
        if header != {"alg": "HS256", "typ": "JWT"}:
            return None
        signing_input = f"{header_text}.{payload_text}"
        expected = hmac.new(
            secret, signing_input.encode("ascii"), hashlib.sha256
        ).digest()
        if not hmac.compare_digest(expected, _decode(signature_text)):
            return None
        if not isinstance(payload, dict) or int(payload["exp"]) <= int(time.time()):
            return None
        if not isinstance(payload.get("sub"), str):
            return None
        return payload
    except (ValueError, TypeError, KeyError, json.JSONDecodeError):
        return None


def _encode_json(value: object) -> str:
    return _encode(
        json.dumps(value, separators=(",", ":"), sort_keys=True).encode("utf-8")
    )


def _encode(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).decode("ascii").rstrip("=")


def _decode(value: str) -> bytes:
    return base64.urlsafe_b64decode(value + "=" * (-len(value) % 4))
