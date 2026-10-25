from fastapi import APIRouter, HTTPException, Request
from sqlalchemy.exc import SQLAlchemyError

from app.services.database import check_database_connection

router = APIRouter(tags=["health"])


@router.get("/health/live", summary="Check whether the API process is running")
def liveness() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/health/ready", summary="Check whether the API can reach MySQL")
def readiness(request: Request) -> dict[str, str]:
    session_factory = request.app.state.session_factory
    if session_factory is None:
        raise HTTPException(
            status_code=503,
            detail={
                "code": "DATABASE_NOT_CONFIGURED",
                "message": "DATABASE_URL is not configured.",
            },
        )

    try:
        check_database_connection(session_factory)
    except SQLAlchemyError as exc:
        raise HTTPException(
            status_code=503,
            detail={
                "code": "DATABASE_UNAVAILABLE",
                "message": "The database is not ready.",
            },
        ) from exc
    return {"status": "ok", "database": "ok"}
