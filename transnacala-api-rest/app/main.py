from contextlib import asynccontextmanager
import json
from pathlib import Path

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.health import router as health_router
from app.api.domain import router as domain_router
from app.api.auth import create_token_secret, router as auth_router
from app.core.config import Settings, get_settings
from app.db.session import create_database_engine, create_session_factory


def create_app(settings: Settings | None = None) -> FastAPI:
    app_settings = settings or get_settings()
    engine = (
        create_database_engine(app_settings.resolved_database_url)
        if app_settings.resolved_database_url
        else None
    )

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        yield
        if engine is not None:
            engine.dispose()

    app = FastAPI(
        title=app_settings.app_name,
        version="0.1.0",
        description=(
            "REST API for TransNacala. MySQL is accessed only by this service."
        ),
        lifespan=lifespan,
    )
    app.state.engine = engine
    app.state.token_secret = create_token_secret(app_settings.auth_secret_key)
    app.state.access_token_expire_seconds = app_settings.access_token_expire_seconds
    app.state.session_factory = (
        create_session_factory(engine) if engine is not None else None
    )

    @app.exception_handler(HTTPException)
    async def http_exception_handler(
        _: Request, exc: HTTPException
    ) -> JSONResponse:
        detail = exc.detail
        if isinstance(detail, dict) and "code" in detail and "message" in detail:
            error = detail
        else:
            error = {
                "code": "HTTP_ERROR",
                "message": str(detail),
            }
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": error},
            headers=exc.headers,
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(
        _: Request, exc: RequestValidationError
    ) -> JSONResponse:
        fields = [
            {
                "field": ".".join(str(part) for part in error["loc"]),
                "message": error["msg"],
                "code": error["type"],
            }
            for error in exc.errors()
        ]
        return JSONResponse(
            status_code=422,
            content={
                "error": {
                    "code": "VALIDATION_ERROR",
                    "message": "The request contains invalid fields.",
                    "fields": fields,
                }
            },
        )

    app.include_router(health_router)
    app.include_router(auth_router)
    app.include_router(domain_router)

    def contract_openapi() -> dict:
        if app.openapi_schema is None:
            contract_path = Path(__file__).with_name("openapi.json")
            app.openapi_schema = json.loads(contract_path.read_text(encoding="utf-8"))
        return app.openapi_schema

    app.openapi = contract_openapi
    return app


app = create_app()
