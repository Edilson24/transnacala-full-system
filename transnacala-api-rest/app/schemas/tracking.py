from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, Field


class TrackingSessionStart(BaseModel):
    vehicle_id: UUID


class TrackingSessionResponse(BaseModel):
    id: UUID
    vehicle_id: UUID
    started_at: datetime
    ended_at: datetime | None = None


class LocationUpdate(BaseModel):
    latitude: Decimal = Field(ge=-90, le=90, max_digits=10, decimal_places=7)
    longitude: Decimal = Field(ge=-180, le=180, max_digits=10, decimal_places=7)
    accuracy_meters: Decimal | None = Field(
        default=None, ge=0, max_digits=8, decimal_places=2
    )
    heading_degrees: Decimal | None = Field(
        default=None, ge=0, le=360, max_digits=6, decimal_places=2
    )
    speed_meters_per_second: Decimal | None = Field(
        default=None, ge=0, max_digits=8, decimal_places=2
    )


class LiveLocation(LocationUpdate):
    vehicle_id: UUID
    session_id: UUID
    updated_at: datetime
