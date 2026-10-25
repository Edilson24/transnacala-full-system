from app.db.base import Base
from app import models  # noqa: F401


def test_models_cover_mysql_bootstrap_tables() -> None:
    assert set(Base.metadata.tables) == {
        "users",
        "routes",
        "stops",
        "route_stops",
        "vehicles",
        "tracking_sessions",
        "live_locations",
        "sync_sequence",
        "sync_changes",
    }


def test_relational_constraints_match_bootstrap_contract() -> None:
    route_stops = Base.metadata.tables["route_stops"]
    locations = Base.metadata.tables["live_locations"]

    assert "uq_route_stops_order" in {
        constraint.name for constraint in route_stops.constraints
    }
    assert "fk_live_locations_session_vehicle" in {
        constraint.name for constraint in locations.constraints
    }
    assert "chk_live_locations_latitude" in {
        constraint.name for constraint in locations.constraints
    }
