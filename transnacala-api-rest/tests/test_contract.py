import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.core.config import Settings
from app.main import create_app
from app.schemas import StopWrite, UserCreate, UserUpdate


def test_openapi_contract_covers_all_client_domains() -> None:
    app = create_app(Settings(database_url=None))
    contract = app.openapi()

    assert contract["openapi"] == "3.1.0"
    assert {
        "/api/v1/auth/login",
        "/api/v1/users",
        "/api/v1/routes",
        "/api/v1/routes/{route_id}/stops",
        "/api/v1/stops",
        "/api/v1/vehicles/{vehicle_id}/assignment",
        "/api/v1/sync/changes",
        "/api/v1/tracking/sessions",
        "/api/v1/tracking/sessions/active",
        "/api/v1/tracking/live",
    }.issubset(contract["paths"])
    assert contract["components"]["schemas"]["User"]["properties"]["id"] == {
        "type": "string",
        "format": "uuid",
        "example": "92c478b7-5c45-4e6a-87db-08b1a4a10a1e",
    }
    assert contract["components"]["parameters"]["Limit"]["schema"]["maximum"] == 100
    assert "next_since_id" in contract["components"]["schemas"]["SyncChangePage"][
        "properties"
    ]
    assert "include_inactive" in {
        parameter.get("name")
        for parameter in contract["paths"]["/api/v1/routes"]["get"]["parameters"]
    }
    assert contract["components"]["schemas"]["VehicleWrite"]["properties"] == {}


def test_tracking_endpoint_returns_empty_page_when_nobody_is_live() -> None:
    app = create_app(Settings(database_url="sqlite://"))
    from app.db.base import Base

    Base.metadata.create_all(app.state.engine)
    client = TestClient(app)
    response = client.get("/api/v1/tracking/live")

    assert response.status_code == 200
    assert response.json()["items"] == []
    assert response.json()["page"]["total"] == 0


def test_openapi_is_served_and_contract_schemas_validate_input() -> None:
    client = TestClient(create_app(Settings(database_url=None)))

    response = client.get("/openapi.json")

    assert response.status_code == 200
    assert response.json()["paths"] == client.app.openapi()["paths"]
    assert "password_hash" not in UserCreate.model_fields
    with pytest.raises(ValidationError):
        StopWrite(name="Out of range", latitude=95, longitude=40)
    with pytest.raises(ValidationError):
        UserUpdate()
