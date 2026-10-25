from sqlalchemy import text
from sqlalchemy.orm import Session, sessionmaker


def check_database_connection(session_factory: sessionmaker[Session]) -> None:
    with session_factory() as session:
        session.execute(text("SELECT 1"))
