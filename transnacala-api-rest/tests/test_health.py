from fastapi.testclient import TestClient

from app.core.config import Settings
from app.main import create_app


def test_mysql_connection_settings_match_local_workbench_defaults() -> None:
    settings = Settings(
        mysql_user="local-user",
        mysql_password="local@password:/with?symbols",
    )

    assert settings.resolved_database_url == (
        "mysql+pymysql://local-user:local%40password%3A%2Fwith%3Fsymbols"
        "@127.0.0.1:3306/transnacala?charset=utf8mb4"
    )


def test_explicit_database_url_takes_precedence() -> None:
    settings = Settings(
        database_url="sqlite://",
        mysql_user="local-user",
        mysql_password="local-password",
    )

    assert settings.resolved_database_url == "sqlite://"


def test_missing_mysql_credentials_leave_database_unconfigured() -> None:
    settings = Settings(mysql_user="", mysql_password="")

    assert settings.resolved_database_url is None


def test_liveness_does_not_require_database() -> None:
    client = TestClient(create_app(Settings(database_url=None)))

    response = client.get("/health/live")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_readiness_reports_missing_database_configuration() -> None:
    client = TestClient(create_app(Settings(database_url=None)))

    response = client.get("/health/ready")

    assert response.status_code == 503
    assert response.json()["error"]["code"] == "DATABASE_NOT_CONFIGURED"


def test_readiness_checks_database_connection() -> None:
    client = TestClient(create_app(Settings(database_url="sqlite://")))

    response = client.get("/health/ready")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "database": "ok"}
