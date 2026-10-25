import enum
from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    CHAR,
    CheckConstraint,
    Enum,
    ForeignKey,
    ForeignKeyConstraint,
    Index,
    Integer,
    BigInteger,
    Numeric,
    String,
    Text,
    TIMESTAMP,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects import mysql
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class UserRole(str, enum.Enum):
    ADMIN = "ADMIN"
    DRIVER = "DRIVER"
    PASSENGER = "PASSENGER"


class SyncEntityType(str, enum.Enum):
    ROUTE = "ROUTE"
    STOP = "STOP"
    ROUTE_STOP = "ROUTE_STOP"
    VEHICLE = "VEHICLE"


class SyncOperation(str, enum.Enum):
    UPSERT = "UPSERT"
    DELETE = "DELETE"


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=func.current_timestamp()
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP,
        nullable=False,
        server_default=func.current_timestamp(),
        onupdate=func.current_timestamp(),
    )


class User(TimestampMixin, Base):
    __tablename__ = "users"
    __table_args__ = (
        UniqueConstraint("username", name="uq_users_username"),
        Index("ix_users_role_active", "role", "is_active"),
    )

    id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    username: Mapped[str] = mapped_column(String(80), nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str] = mapped_column(String(160), nullable=False)
    phone: Mapped[str | None] = mapped_column(String(32))
    role: Mapped[UserRole] = mapped_column(
        Enum(UserRole, name="user_role"), nullable=False
    )
    is_active: Mapped[bool] = mapped_column(
        Boolean, nullable=False, server_default="1"
    )


class Route(TimestampMixin, Base):
    __tablename__ = "routes"
    __table_args__ = (
        CheckConstraint("fare >= 0", name="chk_routes_fare_nonnegative"),
        Index("ix_routes_active_name", "is_active", "name"),
    )

    id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    origin: Mapped[str] = mapped_column(String(160), nullable=False)
    destination: Mapped[str] = mapped_column(String(160), nullable=False)
    fare: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    is_active: Mapped[bool] = mapped_column(
        Boolean, nullable=False, server_default="1"
    )


class Stop(TimestampMixin, Base):
    __tablename__ = "stops"
    __table_args__ = (
        CheckConstraint(
            "latitude BETWEEN -90 AND 90", name="chk_stops_latitude"
        ),
        CheckConstraint(
            "longitude BETWEEN -180 AND 180", name="chk_stops_longitude"
        ),
        Index("ix_stops_active_name", "is_active", "name"),
    )

    id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    name: Mapped[str] = mapped_column(String(160), nullable=False)
    latitude: Mapped[Decimal] = mapped_column(Numeric(10, 7), nullable=False)
    longitude: Mapped[Decimal] = mapped_column(Numeric(10, 7), nullable=False)
    is_active: Mapped[bool] = mapped_column(
        Boolean, nullable=False, server_default="1"
    )


class RouteStop(Base):
    __tablename__ = "route_stops"
    __table_args__ = (
        UniqueConstraint("route_id", "stop_order", name="uq_route_stops_order"),
        ForeignKeyConstraint(
            ["route_id"],
            ["routes.id"],
            name="fk_route_stops_route",
            onupdate="CASCADE",
            ondelete="CASCADE",
        ),
        ForeignKeyConstraint(
            ["stop_id"],
            ["stops.id"],
            name="fk_route_stops_stop",
            onupdate="CASCADE",
            ondelete="CASCADE",
        ),
    )

    route_id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    stop_id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    stop_order: Mapped[int] = mapped_column(
        Integer().with_variant(mysql.INTEGER(unsigned=True), "mysql"),
        nullable=False,
    )
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=func.current_timestamp()
    )


class Vehicle(TimestampMixin, Base):
    __tablename__ = "vehicles"
    __table_args__ = (
        UniqueConstraint("code", name="uq_vehicles_code"),
        Index("ix_vehicles_route_active", "route_id", "is_active"),
    )

    id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    code: Mapped[str] = mapped_column(String(16), nullable=False)
    driver_user_id: Mapped[str | None] = mapped_column(
        ForeignKey(
            "users.id",
            name="fk_vehicles_driver",
            onupdate="CASCADE",
            ondelete="SET NULL",
        )
    )
    route_id: Mapped[str | None] = mapped_column(
        ForeignKey(
            "routes.id",
            name="fk_vehicles_route",
            onupdate="CASCADE",
            ondelete="SET NULL",
        )
    )
    is_active: Mapped[bool] = mapped_column(
        Boolean, nullable=False, server_default="1"
    )


Index("ix_vehicles_driver", Vehicle.driver_user_id)


class TrackingSession(Base):
    __tablename__ = "tracking_sessions"
    __table_args__ = (
        UniqueConstraint(
            "id", "vehicle_id", name="uq_tracking_sessions_id_vehicle"
        ),
        ForeignKeyConstraint(
            ["vehicle_id"],
            ["vehicles.id"],
            name="fk_tracking_sessions_vehicle",
            onupdate="CASCADE",
            ondelete="RESTRICT",
        ),
        ForeignKeyConstraint(
            ["driver_user_id"],
            ["users.id"],
            name="fk_tracking_sessions_driver",
            onupdate="CASCADE",
            ondelete="RESTRICT",
        ),
        CheckConstraint(
            "ended_at IS NULL OR ended_at >= started_at",
            name="chk_tracking_session_end",
        ),
        Index("ix_tracking_sessions_vehicle_started", "vehicle_id", "started_at"),
        Index(
            "ix_tracking_sessions_driver_started", "driver_user_id", "started_at"
        ),
    )

    id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    vehicle_id: Mapped[str] = mapped_column(CHAR(36), nullable=False)
    driver_user_id: Mapped[str] = mapped_column(CHAR(36), nullable=False)
    started_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=func.current_timestamp()
    )
    ended_at: Mapped[datetime | None] = mapped_column(TIMESTAMP)


class LiveLocation(Base):
    __tablename__ = "live_locations"
    __table_args__ = (
        UniqueConstraint("session_id", name="uq_live_locations_session"),
        ForeignKeyConstraint(
            ["vehicle_id"],
            ["vehicles.id"],
            name="fk_live_locations_vehicle",
            onupdate="CASCADE",
            ondelete="CASCADE",
        ),
        ForeignKeyConstraint(
            ["session_id", "vehicle_id"],
            ["tracking_sessions.id", "tracking_sessions.vehicle_id"],
            name="fk_live_locations_session_vehicle",
            onupdate="CASCADE",
            ondelete="CASCADE",
        ),
        CheckConstraint(
            "latitude BETWEEN -90 AND 90", name="chk_live_locations_latitude"
        ),
        CheckConstraint(
            "longitude BETWEEN -180 AND 180", name="chk_live_locations_longitude"
        ),
        CheckConstraint(
            "accuracy_meters IS NULL OR accuracy_meters >= 0",
            name="chk_live_locations_accuracy",
        ),
        CheckConstraint(
            "heading_degrees IS NULL OR heading_degrees BETWEEN 0 AND 360",
            name="chk_live_locations_heading",
        ),
        CheckConstraint(
            "speed_meters_per_second IS NULL OR speed_meters_per_second >= 0",
            name="chk_live_locations_speed",
        ),
        Index("ix_live_locations_updated", "updated_at"),
    )

    vehicle_id: Mapped[str] = mapped_column(CHAR(36), primary_key=True)
    session_id: Mapped[str] = mapped_column(CHAR(36), nullable=False)
    latitude: Mapped[Decimal] = mapped_column(Numeric(10, 7), nullable=False)
    longitude: Mapped[Decimal] = mapped_column(Numeric(10, 7), nullable=False)
    accuracy_meters: Mapped[Decimal | None] = mapped_column(Numeric(8, 2))
    heading_degrees: Mapped[Decimal | None] = mapped_column(Numeric(6, 2))
    speed_meters_per_second: Mapped[Decimal | None] = mapped_column(Numeric(8, 2))
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP,
        nullable=False,
        server_default=func.current_timestamp(),
        onupdate=func.current_timestamp(),
    )


class SyncSequence(Base):
    __tablename__ = "sync_sequence"
    __table_args__ = (
        CheckConstraint("id = 1", name="chk_sync_sequence_singleton"),
    )

    id: Mapped[int] = mapped_column(
        Integer().with_variant(mysql.TINYINT(unsigned=True), "mysql"),
        primary_key=True,
        autoincrement=False,
    )
    last_change_id: Mapped[int] = mapped_column(
        BigInteger().with_variant(mysql.BIGINT(unsigned=True), "mysql"),
        nullable=False,
        server_default="0",
    )


class SyncChange(Base):
    __tablename__ = "sync_changes"
    __table_args__ = (
        Index(
            "ix_sync_changes_entity",
            "entity_type",
            "entity_id",
            "related_entity_id",
        ),
        Index("ix_sync_changes_changed_at", "changed_at"),
    )

    id: Mapped[int] = mapped_column(
        BigInteger().with_variant(mysql.BIGINT(unsigned=True), "mysql"),
        primary_key=True,
        autoincrement=False,
    )
    entity_type: Mapped[SyncEntityType] = mapped_column(
        Enum(SyncEntityType, name="sync_entity_type"), nullable=False
    )
    entity_id: Mapped[str] = mapped_column(CHAR(36), nullable=False)
    related_entity_id: Mapped[str | None] = mapped_column(CHAR(36))
    operation: Mapped[SyncOperation] = mapped_column(
        Enum(SyncOperation, name="sync_operation"), nullable=False
    )
    changed_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=func.current_timestamp()
    )
