from types import SimpleNamespace

import app.cli as cli
import pytest
from app.db.base import Base
from app.db.session import create_database_engine, create_session_factory
from app.models.entities import User, UserRole
from app.services.security import verify_password


def test_seed_test_users_creates_expected_roles_and_hashed_passwords(monkeypatch) -> None:
    engine = create_database_engine("sqlite://")
    Base.metadata.create_all(engine)
    dispose_engine = engine.dispose
    monkeypatch.setattr(engine, "dispose", lambda: None)
    monkeypatch.setattr(
        cli, "get_settings", lambda: SimpleNamespace(
            app_env="development", resolved_database_url="sqlite://"
        ),
    )
    monkeypatch.setattr(cli, "create_database_engine", lambda database_url: engine)
    passwords = iter((
        "edilson123456", "edilson123456",
        "anifa1234567", "anifa1234567",
        "silva1234567", "silva1234567",
    ))
    monkeypatch.setattr(cli, "getpass", lambda prompt: next(passwords))

    cli.seed_test_users()

    with create_session_factory(engine)() as session:
        accounts = {user.username: user for user in session.query(User).all()}
    assert accounts["edilson"].full_name == "Edilson Vitorino Hilario"
    assert accounts["edilson"].role == UserRole.ADMIN
    assert accounts["anifa"].full_name == "Anifa Armando"
    assert accounts["anifa"].role == UserRole.PASSENGER
    assert accounts["silva"].full_name == "Silva Atumane"
    assert accounts["silva"].role == UserRole.DRIVER
    for username, password in (
        ("edilson", "edilson123456"),
        ("anifa", "anifa1234567"),
        ("silva", "silva1234567"),
    ):
        assert accounts[username].password_hash != password
        assert verify_password(password, accounts[username].password_hash)
    original_hashes = {
        username: accounts[username].password_hash
        for username in ("edilson", "anifa", "silva")
    }
    cli.seed_test_users()
    with create_session_factory(engine)() as session:
        assert {
            user.username: user.password_hash
            for user in session.query(User).all()
        } == original_hashes
    dispose_engine()


def test_seed_test_users_refuses_non_development(monkeypatch) -> None:
    monkeypatch.setattr(
        cli, "get_settings",
        lambda: SimpleNamespace(
            app_env="production", resolved_database_url="sqlite://"
        ),
    )

    with pytest.raises(SystemExit, match="APP_ENV=development"):
        cli.seed_test_users()
