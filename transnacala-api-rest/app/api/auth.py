import secrets
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.dependencies import get_db
from app.models.entities import User
from app.schemas.auth import LoginRequest, LoginResponse
from app.schemas.catalogue import UserResponse
from app.services.security import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/api/v1/auth", tags=["Authentication"])
DUMMY_PASSWORD_HASH = hash_password("dummy-password-used-only-for-timing")


@router.post("/login", response_model=LoginResponse)
def login(
    payload: LoginRequest, request: Request, session: Session = Depends(get_db)
) -> LoginResponse:
    user = session.scalar(select(User).where(User.username == payload.username))
    stored_hash = user.password_hash if user is not None else DUMMY_PASSWORD_HASH
    valid = verify_password(payload.password, stored_hash)
    if not valid or user is None or not user.is_active:
        raise HTTPException(
            status_code=401,
            detail={"code": "INVALID_CREDENTIALS", "message": "Invalid username or password."},
            headers={"WWW-Authenticate": "Bearer"},
        )

    secret = request.app.state.token_secret
    token = create_access_token(
        user.id, secret, request.app.state.access_token_expire_seconds
    )
    return LoginResponse(
        access_token=token,
        expires_in=request.app.state.access_token_expire_seconds,
        user=UserResponse.model_validate(user),
    )


def create_token_secret(configured_secret: str | None) -> bytes:
    if not configured_secret:
        return secrets.token_bytes(32)
    secret = configured_secret.encode("utf-8")
    if len(secret) < 32:
        raise ValueError("AUTH_SECRET_KEY must contain at least 32 bytes.")
    return secret


def parse_user_id(value: object) -> UUID | None:
    if not isinstance(value, str):
        return None
    try:
        return UUID(value)
    except ValueError:
        return None
