from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.schemas.common import Page, UserRole


class ORMResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class NonEmptyPatch(BaseModel):
    @model_validator(mode="after")
    def require_at_least_one_field(self) -> "NonEmptyPatch":
        if not self.model_fields_set:
            raise ValueError("At least one field must be provided.")
        return self


class UserCreate(BaseModel):
    username: str = Field(min_length=1, max_length=80)
    password: str = Field(
        min_length=12, json_schema_extra={"format": "password"}
    )
    full_name: str = Field(min_length=1, max_length=160)
    phone: str | None = Field(default=None, max_length=32)
    role: UserRole

    @field_validator("username", "full_name")
    @classmethod
    def trim_required_text(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("This field cannot be blank.")
        return value


class UserUpdate(NonEmptyPatch):
    full_name: str | None = Field(default=None, min_length=1, max_length=160)
    phone: str | None = Field(default=None, max_length=32)
    password: str | None = Field(
        default=None, min_length=12, json_schema_extra={"format": "password"}
    )
    role: UserRole | None = None
    is_active: bool | None = None


class UserResponse(ORMResponse):
    id: UUID
    username: str
    full_name: str
    phone: str | None
    role: UserRole
    is_active: bool
    created_at: datetime
    updated_at: datetime


UserPage = Page[UserResponse]


class RouteWrite(BaseModel):
    name: str = Field(min_length=1, max_length=120)
    origin: str = Field(min_length=1, max_length=160)
    destination: str = Field(min_length=1, max_length=160)
    fare: Decimal = Field(ge=0, max_digits=10, decimal_places=2)
    description: str | None = None


class RouteUpdate(NonEmptyPatch):
    name: str | None = Field(default=None, min_length=1, max_length=120)
    origin: str | None = Field(default=None, min_length=1, max_length=160)
    destination: str | None = Field(default=None, min_length=1, max_length=160)
    fare: Decimal | None = Field(default=None, ge=0, max_digits=10, decimal_places=2)
    description: str | None = None
    is_active: bool | None = None


class RouteStopInput(BaseModel):
    stop_id: UUID
    stop_order: int = Field(ge=0)


class RouteStopResponse(ORMResponse):
    stop_id: UUID
    name: str
    stop_order: int
    latitude: Decimal
    longitude: Decimal


class RouteResponse(ORMResponse):
    id: UUID
    name: str
    origin: str
    destination: str
    fare: Decimal
    description: str | None
    is_active: bool
    stops: list[RouteStopResponse] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime


RoutePage = Page[RouteResponse]


class StopWrite(BaseModel):
    name: str = Field(min_length=1, max_length=160)
    latitude: Decimal = Field(ge=-90, le=90, max_digits=10, decimal_places=7)
    longitude: Decimal = Field(ge=-180, le=180, max_digits=10, decimal_places=7)


class StopUpdate(NonEmptyPatch):
    name: str | None = Field(default=None, min_length=1, max_length=160)
    latitude: Decimal | None = Field(
        default=None, ge=-90, le=90, max_digits=10, decimal_places=7
    )
    longitude: Decimal | None = Field(
        default=None, ge=-180, le=180, max_digits=10, decimal_places=7
    )
    is_active: bool | None = None


class StopResponse(ORMResponse):
    id: UUID
    name: str
    latitude: Decimal
    longitude: Decimal
    is_active: bool
    created_at: datetime
    updated_at: datetime


StopPage = Page[StopResponse]


class VehicleWrite(BaseModel):
    model_config = ConfigDict(extra="forbid")


class VehicleUpdate(NonEmptyPatch):
    is_active: bool | None = None


class VehicleAssignment(BaseModel):
    driver_user_id: UUID | None
    route_id: UUID | None


class VehicleResponse(ORMResponse):
    id: UUID
    code: str
    driver_user_id: UUID | None
    route_id: UUID | None
    is_active: bool
    created_at: datetime
    updated_at: datetime


VehiclePage = Page[VehicleResponse]
