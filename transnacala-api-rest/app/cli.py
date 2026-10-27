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


def main() -> None:
    parser = argparse.ArgumentParser(description="TransNacala API administration")
    subparsers = parser.add_subparsers(dest="command", required=True)
    admin_parser = subparsers.add_parser("create-admin")
    admin_parser.add_argument("username")
    args = parser.parse_args()
    if args.command == "create-admin":
        create_admin(args.username)


if __name__ == "__main__":
    main()
