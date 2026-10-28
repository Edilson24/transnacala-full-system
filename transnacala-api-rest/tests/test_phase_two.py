from datetime import datetime, timedelta, timezone
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.config import Settings
from app.db.base import Base
from app.main import create_app
from app.models.entities import LiveLocation, TrackingSession, User, UserRole
from app.services.security import hash_password


@pytest.fixture
def client() -> TestClient:
    app = create_app(
        Settings(
            database_url="sqlite://",
            auth_secret_key="test-signing-key-that-is-long-enough",
        )
    )
    Base.metadata.create_all(app.state.engine)
    with app.state.session_factory() as session:
        session.add(
            User(
                id=str(uuid4()),
                username="admin",
                password_hash=hash_password("correct horse battery staple"),
                full_name="Admin Test",
                role=UserRole.ADMIN,
            )
        )
        session.commit()
    with TestClient(app) as test_client:
        yield test_client


def login(
    client: TestClient,
    username: str = "admin",
    password: str = "correct horse battery staple",
) -> str:
    response = client.post(
        "/api/v1/auth/login",
        json={
            "username": username,
            "password": password,
        },
    )
    assert response.status_code == 200
    assert "password_hash" not in response.json()["user"]
    return response.json()["access_token"]


def test_authentication_and_admin_authorization(client: TestClient) -> None:
    response = client.get("/api/v1/users")
    assert response.status_code == 401

    token = login(client)
    headers = {"Authorization": f"Bearer {token}"}
    assert client.get("/api/v1/users", headers=headers).status_code == 200
    admin_id = client.get("/api/v1/users", headers=headers).json()["items"][0]["id"]
    last_admin = client.patch(
        f"/api/v1/users/{admin_id}",
        headers=headers,
        json={"is_active": False},
    )
    assert last_admin.status_code == 409

    driver = client.post(
        "/api/v1/users",
        headers=headers,
        json={
            "username": "driver",
            "password": "driver password",
            "full_name": "Test Driver",
            "role": "DRIVER",
        },
    )
    assert driver.status_code == 201

    driver_token = login(client, "driver", "driver password")
    forbidden = client.get(
        "/api/v1/users",
        headers={"Authorization": f"Bearer {driver_token}"},
    )
    assert forbidden.status_code == 403

    password_update = client.patch(
        f"/api/v1/users/{driver.json()['id']}",
        headers=headers,
        json={"password": "a much stronger replacement"},
    )
    assert password_update.status_code == 200
    assert "password_hash" not in password_update.json()
    assert client.post(
        "/api/v1/auth/login",
        json={"username": "driver", "password": "driver password"},
    ).status_code == 401
    assert client.post(
        "/api/v1/auth/login",
        json={"username": "driver", "password": "a much stronger replacement"},
    ).status_code == 200

    inactive = client.patch(
        f"/api/v1/users/{driver.json()['id']}",
        headers=headers,
        json={"is_active": False},
    )
    assert inactive.status_code == 200
    rejected_login = client.post(
        "/api/v1/auth/login",
        json={"username": "driver", "password": "driver password"},
    )
    assert rejected_login.status_code == 401


def test_catalogue_crud_assignment_and_incremental_sync(client: TestClient) -> None:
    headers = {"Authorization": f"Bearer {login(client)}"}
    driver = client.post(
        "/api/v1/users",
        headers=headers,
        json={
            "username": "driver",
            "password": "driver password",
            "full_name": "Test Driver",
            "role": "DRIVER",
        },
    ).json()
    stop = client.post(
        "/api/v1/stops",
        headers=headers,
        json={"name": "Nacala-Porto", "latitude": -14.55, "longitude": 40.67},
    )
    assert stop.status_code == 201

    route = client.post(
        "/api/v1/routes",
        headers=headers,
        json={
            "name": "Centro",
            "origin": "Porto",
            "destination": "Centro",
            "fare": "25.00",
        },
    )
    assert route.status_code == 201
    route_id = route.json()["id"]

    changed_route = client.put(
        f"/api/v1/routes/{route_id}/stops",
        headers=headers,
        json=[{"stop_id": stop.json()["id"], "stop_order": 1}],
    )
    assert changed_route.status_code == 200
    assert changed_route.json()["stops"][0]["name"] == "Nacala-Porto"

    added_route_stop = client.post(
        f"/api/v1/routes/{route_id}/stops",
        headers=headers,
        json={"name": "Mercado", "latitude": -14.54, "longitude": 40.68},
    )
    assert added_route_stop.status_code == 201
    assert [stop["stop_order"] for stop in added_route_stop.json()["stops"]] == [1, 2]
    assert added_route_stop.json()["stops"][1]["name"] == "Mercado"

    vehicle = client.post("/api/v1/vehicles", headers=headers, json={})
    assert vehicle.status_code == 201
    assert vehicle.json()["code"] == "V001"
    second_vehicle = client.post("/api/v1/vehicles", headers=headers, json={})
    assert second_vehicle.status_code == 201
    assert second_vehicle.json()["code"] == "V002"
    assigned = client.put(
        f"/api/v1/vehicles/{vehicle.json()['id']}/assignment",
        headers=headers,
        json={"driver_user_id": driver["id"], "route_id": route_id},
    )
    assert assigned.status_code == 200
    assert assigned.json()["driver_user_id"] == driver["id"]
    rejected_role_change = client.patch(
        f"/api/v1/users/{driver['id']}",
        headers=headers,
        json={"role": "PASSENGER"},
    )
    assert rejected_role_change.status_code == 409

    hidden_catalogue = client.get("/api/v1/routes?include_inactive=true")
    assert hidden_catalogue.status_code == 401
    deactivated = client.patch(
        f"/api/v1/routes/{route_id}",
        headers=headers,
        json={"is_active": False},
    )
    assert deactivated.status_code == 200
    assert client.get("/api/v1/routes").json()["items"] == []
    admin_catalogue = client.get(
        "/api/v1/routes?include_inactive=true", headers=headers
    )
    assert admin_catalogue.json()["items"][0]["is_active"] is False
    assert client.patch(
        f"/api/v1/routes/{route_id}",
        headers=headers,
        json={"is_active": True},
    ).status_code == 200

    changes = client.get("/api/v1/sync/changes?limit=2")
    assert changes.status_code == 200
    assert len(changes.json()["items"]) == 2
    assert changes.json()["has_more"] is True
    next_page = client.get(
        f"/api/v1/sync/changes?since_id={changes.json()['next_since_id']}"
    )
    assert next_page.json()["items"]


def test_token_signature_is_checked(client: TestClient) -> None:
    response = client.get(
        "/api/v1/users",
        headers={"Authorization": "Bearer not.a.valid.token"},
    )
    assert response.status_code == 401


def test_tracking_session_lifecycle_and_live_locations(client: TestClient) -> None:
    admin_headers = {"Authorization": f"Bearer {login(client)}"}
    driver_response = client.post(
        "/api/v1/users",
        headers=admin_headers,
        json={
            "username": "driver",
            "password": "driver password",
            "full_name": "Test Driver",
            "role": "DRIVER",
        },
    )
    assert driver_response.status_code == 201, driver_response.text
    driver = driver_response.json()
    vehicle_response = client.post("/api/v1/vehicles", headers=admin_headers, json={})
    assert vehicle_response.status_code == 201, vehicle_response.text
    vehicle = vehicle_response.json()
    assignment = client.put(
        f"/api/v1/vehicles/{vehicle['id']}/assignment",
        headers=admin_headers,
        json={"driver_user_id": driver["id"], "route_id": None},
    )
    assert assignment.status_code == 200
    driver_headers = {
        "Authorization": f"Bearer {login(client, 'driver', 'driver password')}"
    }
    passenger_response = client.post(
        "/api/v1/users",
        headers=admin_headers,
        json={
            "username": "passenger",
            "password": "passenger password",
            "full_name": "Test Passenger",
            "role": "PASSENGER",
        },
    )
    assert passenger_response.status_code == 201, passenger_response.text
    passenger_token = login(client, "passenger", "passenger password")
    forbidden = client.post(
        "/api/v1/tracking/sessions",
        headers={"Authorization": f"Bearer {passenger_token}"},
        json={"vehicle_id": vehicle["id"]},
    )
    assert forbidden.status_code == 403
    assert client.post(
        "/api/v1/tracking/sessions",
        headers=driver_headers,
        json={"vehicle_id": str(uuid4())},
    ).status_code == 403

    stale_session_id = str(uuid4())
    with client.app.state.session_factory() as session:
        session.add(
            TrackingSession(
                id=stale_session_id,
                vehicle_id=vehicle["id"],
                driver_user_id=driver["id"],
                started_at=datetime.now(timezone.utc).replace(tzinfo=None)
                - timedelta(minutes=10),
            )
        )
        session.commit()
    started = client.post(
        "/api/v1/tracking/sessions",
        headers=driver_headers,
        json={"vehicle_id": vehicle["id"]},
    )
    assert started.status_code == 201
    session_id = started.json()["id"]
    with client.app.state.session_factory() as session:
        assert session.get(TrackingSession, stale_session_id).ended_at is not None
    assert client.get(
        "/api/v1/tracking/sessions/active",
        headers=driver_headers,
        params={"vehicle_id": vehicle["id"]},
    ).json()["session"]["id"] == session_id
    reconnected = client.post(
        "/api/v1/tracking/sessions",
        headers=driver_headers,
        json={"vehicle_id": vehicle["id"]},
    )
    assert reconnected.status_code == 200
    assert reconnected.json()["id"] == session_id

    location = client.put(
        f"/api/v1/tracking/sessions/{session_id}/location",
        headers=driver_headers,
        json={
            "latitude": "-14.542123456789",
            "longitude": "40.672123456789",
            "accuracy_meters": "8.56789",
            "heading_degrees": "90.12345",
            "speed_meters_per_second": "4.23456",
        },
    )
    assert location.status_code == 200
    assert location.json()["vehicle_code"] == vehicle["code"]
    assert location.json()["latitude"] == "-14.5421235"
    assert location.json()["longitude"] == "40.6721235"
    assert location.json()["accuracy_meters"] == "8.57"
    assert location.json()["heading_degrees"] == "90.12"
    assert location.json()["speed_meters_per_second"] == "4.23"
    invalid_location = client.put(
        f"/api/v1/tracking/sessions/{session_id}/location",
        headers=driver_headers,
        json={"latitude": "91.123456789", "longitude": "40.672123456789"},
    )
    assert invalid_location.status_code == 422
    live = client.get("/api/v1/tracking/live")
    assert live.status_code == 200
    assert live.json()["page"]["total"] == 1
    assert live.json()["items"][0]["vehicle_id"] == vehicle["id"]

    ended = client.delete(
        f"/api/v1/tracking/sessions/{session_id}", headers=driver_headers
    )
    assert ended.status_code == 204
    assert client.get("/api/v1/tracking/live").json()["items"] == []
    assert client.put(
        f"/api/v1/tracking/sessions/{session_id}/location",
        headers=driver_headers,
        json={"latitude": -14.542, "longitude": 40.672},
    ).status_code == 409
    assert client.get(
        "/api/v1/tracking/sessions/active",
        headers=driver_headers,
        params={"vehicle_id": vehicle["id"]},
    ).json()["session"] is None
    with client.app.state.session_factory() as session:
        assert session.scalar(select(LiveLocation)) is None
        tracking_session = session.get(TrackingSession, session_id)
        assert tracking_session.ended_at is not None
