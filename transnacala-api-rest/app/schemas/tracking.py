from datetime import datetime
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, ValidationInfo, field_validator

from app.schemas.common import PageMetadata

class TrackingSessionStart(BaseModel):
    vehicle_id: UUID


class TrackingSessionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

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

    @field_validator(
        "latitude",
        "longitude",
        "accuracy_meters",
        "heading_degrees",
        "speed_meters_per_second",
        mode="before",
    )
    @classmethod
    def round_location_precision(
        cls, value: object, info: ValidationInfo
    ) -> object:
        if value is None:
            return value
        scale = 7 if info.field_name in {"latitude", "longitude"} else 2
        try:
            return Decimal(str(value)).quantize(
                Decimal(1).scaleb(-scale),
                rounding=ROUND_HALF_UP,
            )
        except (InvalidOperation, ValueError):
            return value


class LiveLocation(LocationUpdate):
    model_config = ConfigDict(from_attributes=True)

    vehicle_id: UUID
    session_id: UUID
    updated_at: datetime
    vehicle_code: str | None = None
    route_id: UUID | None = None
    route_name: str | None = None


class LiveLocationPage(BaseModel):
    items: list[LiveLocation]
    page: PageMetadata
