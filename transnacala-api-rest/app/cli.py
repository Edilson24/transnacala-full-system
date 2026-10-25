import argparse
from getpass import getpass
from uuid import uuid4

from sqlalchemy import select

from app.core.config import get_settings
from app.db.session import create_database_engine, create_session_factory
from app.models.entities import User, UserRole
from app.services.security import hash_password


def create_admin(username: str) -> None:
    settings = get_settings()
    database_url = settings.resolved_database_url
    if not database_url:
        raise SystemExit("Configure DATABASE_URL or MYSQL_USER/MYSQL_PASSWORD first.")

    password = getpass("New administrator password (minimum 12 characters): ")
    confirmation = getpass("Confirm password: ")
    if len(password) < 12:
        raise SystemExit("The administrator password must be at least 12 characters.")
    if password != confirmation:
        raise SystemExit("Passwords do not match.")

    engine = create_database_engine(database_url)
    try:
        with create_session_factory(engine)() as session:
            if session.scalar(select(User).where(User.username == username)):
                raise SystemExit("That username already exists.")
            session.add(
                User(
                    id=str(uuid4()),
                    username=username,
                    password_hash=hash_password(password),
                    full_name="Administrador",
                    role=UserRole.ADMIN,
                )
            )
            session.commit()
    finally:
        engine.dispose()
    print(f"Administrator account '{username}' created.")


def seed_test_users() -> None:
    settings = get_settings()
    if settings.app_env.lower() != "development":
        raise SystemExit("Test users may only be created when APP_ENV=development.")
    database_url = settings.resolved_database_url
    if not database_url:
        raise SystemExit("Configure DATABASE_URL or MYSQL_USER/MYSQL_PASSWORD first.")

    users = (
        ("edilson", "Edilson Vitorino Hilario", UserRole.ADMIN),
        ("anifa", "Anifa Armando", UserRole.PASSENGER),
        ("silva", "Silva Atumane", UserRole.DRIVER),
    )
    engine = create_database_engine(database_url)
    try:
        with create_session_factory(engine)() as session:
            missing = [
                account for account in users
                if session.scalar(select(User.id).where(User.username == account[0])) is None
            ]
            if not missing:
                print("All requested test accounts already exist; no changes made.")
                return

            created = []
            for username, full_name, role in missing:
                password = getpass(f"Password for {username} (minimum 12 characters): ")
                confirmation = getpass("Confirm password: ")
                if len(password) < 12:
                    raise SystemExit("Passwords must contain at least 12 characters.")
                if password != confirmation:
                    raise SystemExit(f"Passwords for '{username}' do not match.")
                created.append(
                    User(
                        id=str(uuid4()),
                        username=username,
                        password_hash=hash_password(password),
                        full_name=full_name,
                        role=role,
                        is_active=True,
                    )
                )
            session.add_all(created)
            session.commit()
            print("Created test accounts: " + ", ".join(user.username for user in created))
    finally:
        engine.dispose()


def main() -> None:
    parser = argparse.ArgumentParser(description="TransNacala API administration")
    subparsers = parser.add_subparsers(dest="command", required=True)
    admin_parser = subparsers.add_parser("create-admin")
    admin_parser.add_argument("username")
    subparsers.add_parser("seed-test-users")
    args = parser.parse_args()
    if args.command == "create-admin":
        create_admin(args.username)
    elif args.command == "seed-test-users":
        seed_test_users()


if __name__ == "__main__":
    main()
