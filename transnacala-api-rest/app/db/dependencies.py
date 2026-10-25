from collections.abc import Iterator

from fastapi import HTTPException, Request
from sqlalchemy.orm import Session


def get_db(request: Request) -> Iterator[Session]:
    session_factory = request.app.state.session_factory
    if session_factory is None:
        raise HTTPException(
            status_code=503,
            detail={
                "code": "DATABASE_NOT_CONFIGURED",
                "message": "DATABASE_URL is not configured.",
            },
        )

    with session_factory() as session:
        yield session
